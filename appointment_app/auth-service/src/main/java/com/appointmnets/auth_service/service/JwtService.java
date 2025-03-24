package com.appointmnets.auth_service.service;

import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.appointmnets.auth_service.entity.User;
import com.appointmnets.auth_service.repo.UserCredentialRepository;

import java.security.Key;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Service
public class JwtService {

    public static final String secret = "fb2d057ae42b8918f90fbea1ba2a6c19869a2bbe3cb1012f1280535e93ec7708";

    public static final long JWT_TOKEN_VALIDITY = 30 * 60 * 1000; // 30 minutes

    @Autowired
    private UserCredentialRepository userCredentialRepository;

    public String generateToken(String username) {
        User user = userCredentialRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", user.getId());
        return createToken(claims, username);
    }

    private String createToken(Map<String, Object> claims, String username) {
        return Jwts.builder()
                .setClaims(claims)
                .setSubject(username)
                .setIssuedAt(new Date(System.currentTimeMillis()))
                .setExpiration(new Date(System.currentTimeMillis() + JWT_TOKEN_VALIDITY))
                .signWith(getSignKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    public boolean validateToken(String token) {
        try {
            Jwts.parserBuilder()
                    .setSigningKey(getSignKey())
                    .build()
                    .parseClaimsJws(token);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public String getUserIdFromToken(String token) {
        Claims claims = extractAllClaims(token);
        Object userIdObj = claims.get("userId");
        if (userIdObj instanceof Integer) {
            return String.valueOf(userIdObj); // Convert Integer to String
        } else if (userIdObj instanceof Long) {
            return String.valueOf(userIdObj); // Convert Long to String
        } else if (userIdObj instanceof String) {
            return (String) userIdObj;
        } else {
            throw new IllegalArgumentException("Unexpected userId type: " + userIdObj.getClass().getName());
        }
    }

    private Claims extractAllClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(getSignKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    public boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }

    private Date extractExpiration(String token) {
        return extractAllClaims(token).getExpiration();
    }

    private Key getSignKey() {
        byte[] keyBytes = Decoders.BASE64.decode(secret);
        return Keys.hmacShaKeyFor(keyBytes);
    }
}