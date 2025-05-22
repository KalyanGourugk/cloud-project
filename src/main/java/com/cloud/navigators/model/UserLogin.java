package com.cloud.navigators.model;

import lombok.Data;

@Data
public class UserLogin {

    private String email;
    private String password;

    private String family_name ;

    private String profile_pic ;

}
