package com.api.automate.security;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

import com.api.automate.service.CustomUserDetailsService;

@Configuration
@EnableWebSecurity
public class WebSecurityConfig {
	
	@Autowired
	private CustomUserDetailsService userDetailsService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtAuthenticationFilter jwtAuthenticationFilter; // Inject the JWT filter

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .authorizeHttpRequests(authorize -> authorize
                .anyRequest().permitAll() 
            )
            .csrf(csrf -> csrf.disable()) 
            .headers(headers -> headers
                .frameOptions(frameOptions -> frameOptions.disable())  
            );

        return http.build();
    }
}
