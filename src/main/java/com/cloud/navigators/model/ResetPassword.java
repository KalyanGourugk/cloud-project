package com.cloud.navigators.model;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class ResetPassword {

    private String currentpassword ;

    private String newpassword ;

    private String confirmpassword ;

}
