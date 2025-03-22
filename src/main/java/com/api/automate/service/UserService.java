package com.api.automate.service;

import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.api.automate.model.User;
import com.api.automate.repository.UserRepository;

@Service
public class UserService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;  // Changed from BCryptPasswordEncoder

    public User register(User user) {
        // Encode the password before saving
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        user.setRole("customer");
        return userRepository.save(user);
    }
    public User updateUser(User user) {
        // Ensure the user exists before updating
        Optional<User> existingUser = userRepository.findById(user.getId());
        if (existingUser.isPresent()) {
            return userRepository.save(user);
        } else {
            throw new IllegalArgumentException("User not found");
        }
    }

    public void save(User user) {
        userRepository.save(user);
    }

    public void delete(User user) {
        userRepository.delete(user);
    }


    public Optional<User> findByEmail(String email) {
        return userRepository.findByEmail(email);
    }
}
