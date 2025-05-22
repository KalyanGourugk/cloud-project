package com.cloud.navigators.model;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AuthResponse {

    private String accessToken ;

    private String refreshToken ;

    private String idToken ;

    private String username ;

    private String photolink ;
}
