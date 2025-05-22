package com.cloud.navigators.services;

import com.cloud.navigators.client.ProductCartClient;
import com.cloud.navigators.model.Cart;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Service;
import org.springframework.ui.Model;

import java.math.BigDecimal;

@Service
public class ShoppingCartService {

    @Autowired
    private ResourceLoader resourceLoader;

    private Cart shoppingCart; // Populate this from Step 2

    @Autowired
    private ProductCartClient productCartClient ;

    public void loadShoppingCart() {
        try {
            Resource resource = resourceLoader.getResource("classpath:data/democart.json");
            ObjectMapper objectMapper = new ObjectMapper();
            shoppingCart = objectMapper.readValue(resource.getInputStream(), Cart[].class)[0];
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    public void populateModel(Model model, String email) {

        try {
            shoppingCart  = productCartClient.getCartDetails(email)[0];
        }catch (Exception e){
            //
            loadShoppingCart() ;
        }

        model.addAttribute("items", shoppingCart.getItems());
        model.addAttribute("shippingAddress", shoppingCart.getShippingaddress());
        model.addAttribute("user", shoppingCart.getUser() + "'s shopping cart");
        model.addAttribute("email", shoppingCart.getEmail()) ;
        model.addAttribute("count", shoppingCart.getItems().size() + " ITEMS") ;
        model.addAttribute("total", shoppingCart.getItems().stream()
                .map(item -> new BigDecimal(item.getPrice().replace("$", "").trim()))
                .reduce(BigDecimal::add).get().toString()) ;
    }


}
