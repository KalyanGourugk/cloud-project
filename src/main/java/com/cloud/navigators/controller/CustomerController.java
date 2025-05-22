package com.cloud.navigators.controller;

import com.cloud.navigators.model.*;
import com.cloud.navigators.services.CustomerService;
import com.cloud.navigators.services.S3Service;
import com.cloud.navigators.services.ShoppingCartService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpSession;
import java.io.IOException;

@Controller
@ControllerAdvice
public class CustomerController {

    @Autowired
    private CustomerService customerService ;

    @Autowired
    private S3Service s3Service ;

    @Autowired
    private ShoppingCartService shoppingCartService ;

    // ######### 1. Signup
    @GetMapping("/signup")
    public String signupForm(Model model, HttpSession session) {

        model.addAttribute("user", new UserSignup());
        return "signup";
    }

    @PostMapping("/signup")
    public String signup(@ModelAttribute UserSignup user, Model model,
                         @RequestParam("file") MultipartFile file, HttpSession session) throws IOException {


        System.out.println("Received a file along with details");

        String publicurl = "" ;
        if(file != null){
             publicurl = s3Service.uploadfiletoS3(file) ;
        }

        user.setPhotoUrl(publicurl);
        customerService.signupUser(user);
        session.setAttribute("UserSignup", user);
        model.addAttribute("confirm", new ConfirmationCode());

        return "verifycode";
    }


    // ######### 2. Verification code

    @PostMapping("/verifycode")
    public String confirmationCode(@ModelAttribute ConfirmationCode code, Model model, HttpSession session) {

        // Verifying the code
        System.out.println("Code passed is " + code);

        UserSignup user = (UserSignup)session.getAttribute("UserSignup");
        customerService.confirmUser(code.getCode(), user);

        model.addAttribute("user", new UserLogin());

        return "login";
    }

    // 3. ###### Login

    @GetMapping("/login")
    public String loginForm(Model model, HttpSession session) {

        if(session.getAttribute("accessToken") != null){
            AuthResponse response =  (AuthResponse) session.getAttribute("accessToken") ;
            model.addAttribute("pictureUrl", response.getPhotolink());
            model.addAttribute("name", response.getUsername());
            return "redirect:/navigators" ;
        }

        model.addAttribute("user", new UserLogin());
        return "login";
    }

    @PostMapping("/login")
    public String login(@ModelAttribute UserLogin user, Model model, HttpSession session) {

        System.out.println("Calling Cognito..");

        if(session.getAttribute("accessToken") != null){
            AuthResponse response =  (AuthResponse) session.getAttribute("accessToken") ;
            model.addAttribute("pictureUrl", response.getPhotolink());
            model.addAttribute("name", response.getUsername());
            return "redirect:/navigators" ;
        }

        Object result = customerService.authenticateUser(user) ;

        if(result instanceof String){  // reset password
            session.setAttribute("userLogin", user);
            model.addAttribute("reset", new ResetPassword());
            return "reset";
        }

        AuthResponse accesstoken = (AuthResponse) customerService.authenticateUser(user) ;
        model.addAttribute("user", user);
        session.setAttribute("accessToken", accesstoken);

        System.out.println("Login Successfully");

        return "redirect:/navigators" ;
    }

    // ### 4. Reset Password - First Time

    @PostMapping("/reset")
    public String reset(@ModelAttribute ResetPassword reset, Model model, HttpSession session) {

        System.out.println("Calling Cognito..");

        UserLogin login = (UserLogin) session.getAttribute("userLogin") ;

        assert login != null;
        customerService.handleResetPassword(login, reset) ;

        return "login" ;
    }


    @GetMapping("/logout")
    public String logoutPage(HttpSession session, Model model) {
        // Return the view for the logout page
        if (session != null) {
            session.invalidate();
        }
        model.addAttribute("user", new UserLogin());
        return "login";
    }

    @GetMapping("/cart")
    public String cart(HttpSession session, Model model) {


        shoppingCartService.populateModel(model, "mayankdembla913@gmail.com");


        return "shoppingCart";
    }



    // --- Handle Exception

    @ExceptionHandler(Exception.class)
    public ModelAndView handleException(Exception ex, Model model, HttpSession session) {

        // Return the view for the logout page
        if (session != null) {
            session.invalidate();
        }
        model.addAttribute("user", new UserLogin());

        // Create a ModelAndView object to specify the login page
        ModelAndView modelAndView = new ModelAndView("redirect:/login");

        // You can also add a message to be displayed on the login page
        modelAndView.addObject("errorMessage", "An error occurred. Please log in again.");

        return modelAndView;
    }


}
