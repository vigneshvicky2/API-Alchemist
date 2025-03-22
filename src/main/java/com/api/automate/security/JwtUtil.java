package com.api.automate.security;

import java.util.Date;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;

@Service
public class JwtUtil {

    @Value("${jwt.secret}")
    private String secret;

    private final long EXPIRATION_TIME = 1000 * 60 * 60 * 10; // 10 hours

    // Generate JWT token using email as the unique identifier
    public String generateToken(String email) {
        Algorithm algorithm = Algorithm.HMAC256(secret);
        return JWT.create()
                .withSubject(email) // Set the subject to the user's email
                .withIssuedAt(new Date())
                .withExpiresAt(new Date(System.currentTimeMillis() + EXPIRATION_TIME))
                .sign(algorithm);
    }

    // Validate JWT token
    public boolean validateToken(String token) {
        try {
            Algorithm algorithm = Algorithm.HMAC256(secret);
            JWT.require(algorithm).build().verify(token);
            return true; // Token is valid
        } catch (Exception e) {
            return false; // Token is invalid
        }
    }

    // Extract email from the token
    public String extractEmail(String token) {
        return JWT.decode(token).getSubject(); // Extract the email from the token
    }
}
