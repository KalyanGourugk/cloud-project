package com.cloud.navigators.controller;

import com.cloud.navigators.model.AuthResponse;
import com.cloud.navigators.model.UserSignup;
import com.cloud.navigators.services.CustomerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import javax.servlet.http.HttpSession;

@Controller
public class ProductsController {

    @Autowired
    CustomerService customerService ;

    @GetMapping("/navigators")
    public String frontPage(Model model, HttpSession session) {

        if(customerService.validateToken(session)){
            return "login";
        }

        AuthResponse response =  (AuthResponse) session.getAttribute("accessToken") ;
        model.addAttribute("pictureUrl", response.getPhotolink());
        model.addAttribute("name", response.getUsername());

        return "navigators";
    }

}
