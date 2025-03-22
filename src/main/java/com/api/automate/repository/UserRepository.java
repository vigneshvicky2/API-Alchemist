package com.api.automate.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.api.automate.model.User;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);
}
