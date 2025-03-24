package com.appointmnets.auth_service.service;

import java.util.HashMap;
import java.util.Map;

import java.util.Optional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.appointmnets.auth_service.dto.UserLoginRequest;
import com.appointmnets.auth_service.dto.UserRegistrationRequest;
import com.appointmnets.auth_service.entity.User;
import com.appointmnets.auth_service.repo.UserCredentialRepository;

import jakarta.transaction.Transactional;

@Service
public class AuthService {

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private UserCredentialRepository userCredentialRepository;

    @Autowired
    private BCryptPasswordEncoder passwordEncoder;

    @Autowired
    private JwtService jwtService;

    @Transactional(rollbackOn = Exception.class)
    public ResponseEntity<String> saveUser(UserRegistrationRequest request) {
        if (userCredentialRepository.findByUsername(request.getUsername()).isPresent()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("{\"message\": \"Username already taken\"}");
        }

        User authUser = new User();
        authUser.setUsername(request.getUsername());
        authUser.setPassword(passwordEncoder.encode(request.getPassword()));
        authUser = userCredentialRepository.save(authUser);

        String userProfileUrl = "http://localhost:8080/users";
        Map<String, Object> userProfileRequest = new HashMap<>();
        userProfileRequest.put("userId", authUser.getId());
        userProfileRequest.put("name", request.getName());
        userProfileRequest.put("email", request.getEmail());
        userProfileRequest.put("phone", request.getPhone());
        userProfileRequest.put("preferences", request.getPreferences());

        try {
            ResponseEntity<String> response = restTemplate.postForEntity(userProfileUrl, userProfileRequest,
                    String.class);
            if (!response.getStatusCode().is2xxSuccessful()) {
                throw new RuntimeException("Profile creation failed");
            }
            return ResponseEntity.ok("{\"message\": \"User registered successfully!\"}");
        } catch (Exception e) {
            throw new RuntimeException("Profile service error: " + e.getMessage());
        }
    }

    public ResponseEntity<String> Login(UserLoginRequest request) {
        Optional<User> u = userCredentialRepository.findByUsername(request.getUsername());
        if (u.isPresent() && passwordEncoder.matches(request.getPassword(), u.get().getPassword())) {
            String token = jwtService.generateToken(u.get().getUsername());
            return ResponseEntity.ok("{\"token\": \"" + token + "\"}");
        } else {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body("{\"message\": \"Invalid username or password\"}");
        }
    }

    public String generateToken(String username) {
        return jwtService.generateToken(username);
    }

    public ResponseEntity<String> validateToken(String token) {
        try {
            jwtService.validateToken(token);
            return ResponseEntity.ok("{\"message\": \"Token is valid\"}");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body("{\"message\": \"Invalid token\"}");
        }
    }
}
