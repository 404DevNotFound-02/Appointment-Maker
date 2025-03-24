package com.appointmnets.auth_service.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserRegistrationRequest {

    private String username;
    private String password;
    private String name;
    private String email;
    private String phone;
    private String preferences;
}
