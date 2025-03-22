package com.generated.api.controller;

import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/user")
public class UserController {

@PostMapping("/create")
public String handleCreate() {
    return "Success";
}
@GetMapping("/get/{id}")
public String handleGet{id}() {
    return "{ "id": 1, "name": "John Doe", "email": "john@example.com" }";
}

}
