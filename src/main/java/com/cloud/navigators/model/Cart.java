package com.cloud.navigators.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

@Data
public class Cart {

    private List<Item> items;
    private String shippingaddress;
    private String  user ;
    private String email ;

}
