package com.cloud.navigators.client;

import com.cloud.navigators.model.Cart;
import org.springframework.http.*;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.client.RestTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Service
public class ProductCartClient {

    @Value("${producendpoint}")
    private String productUrl ;

    public Cart[] getCartDetails(String email){

        RestTemplate restTemplate = new RestTemplate() ;

        List<HttpMessageConverter<?>> messageConverters = new ArrayList<>();
        MappingJackson2HttpMessageConverter converter = new MappingJackson2HttpMessageConverter();
        converter.setSupportedMediaTypes(Collections.singletonList(MediaType.ALL));
        messageConverters.add(converter);
        restTemplate.setMessageConverters(messageConverters);

        HttpHeaders headers = new HttpHeaders();
        headers.add("email", email);
        headers.set("Content-Type", "application/json");
        headers.set("Accept", "*/*");
        HttpEntity<Void> requestEntity = new HttpEntity<>(headers);

        ResponseEntity<Cart[]> responseEntity = restTemplate.exchange(productUrl,
                HttpMethod.GET,
                requestEntity,
                Cart[].class);

        // Retrieve the response body
        return responseEntity.getBody();

    }


}
