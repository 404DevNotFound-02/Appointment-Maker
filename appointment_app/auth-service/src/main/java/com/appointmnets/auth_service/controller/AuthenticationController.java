package com.appointmnets.auth_service.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.appointmnets.auth_service.dto.UserLoginRequest;
import com.appointmnets.auth_service.dto.UserRegistrationRequest;
import com.appointmnets.auth_service.service.AuthService;
import com.appointmnets.auth_service.service.JwtService;

@RestController
@RequestMapping("/auth")
public class AuthenticationController {
    @Autowired
    private AuthService service;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private AuthenticationManager authenticationManager;

    @PostMapping("/register")
    public ResponseEntity<String> addNewUser(@RequestBody UserRegistrationRequest user) {
        return service.saveUser(user);
    }

    @PostMapping("/login")
    public ResponseEntity<String> Login(@RequestBody UserLoginRequest user) {
        return service.Login(user);
    }

    @GetMapping("/user-id")
    public ResponseEntity<String> getUserIdFromToken(@RequestHeader("Authorization") String token) {
        if (token != null && token.startsWith("Bearer ")) {
            token = token.substring(7);
        }
        try {
            String userId = jwtService.getUserIdFromToken(token);
            return ResponseEntity.ok(userId);
        } catch (Exception e) {
            return ResponseEntity.status(401).body(null); // Unauthorized
        }
    }

    @PostMapping("/validate")
    public ResponseEntity<Boolean> validateToken(@RequestHeader("Authorization") String token) {
        // Remove "Bearer " prefix if present
        if (token != null && token.startsWith("Bearer ")) {
            token = token.substring(7);
        }
        boolean isValid = jwtService.validateToken(token) && !jwtService.isTokenExpired(token);
        return ResponseEntity.ok(isValid);
    }

    // @GetMapping("/getuser")
    // public Long getUserId(String token) {
    // return jwtService.getUserId(token);
    // }

    // @PostMapping("/token")
    // public String getToken(@RequestBody UserRegistrationRequest authRequest) {
    // Authentication authenticate = authenticationManager.authenticate(
    // new UsernamePasswordAuthenticationToken(authRequest.getUsername(),
    // authRequest.getPassword()));
    // if (authenticate.isAuthenticated()) {
    // return service.generateToken(authRequest.getUsername());
    // } else {
    // throw new RuntimeException("invalid access");
    // }
    // }

    // @GetMapping("/validate")
    // public String validateToken(@RequestParam("token") String token) {
    // service.validateToken(token);
    // return "Token is valid";
    // }

}
