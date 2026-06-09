package com.example.jpa.controller;

import com.example.jpa.entity.User;
import com.example.jpa.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
public class UserController {

    @Autowired
    private UserService userService;

    @PostMapping("/create")
    public ResponseEntity<?> createUser(@RequestParam String name, 
                                           @RequestParam String password, 
                                           @RequestParam String bio, 
                                           @RequestParam String avatarUrl) {
        try {
            User user = userService.createUser(name, password, bio, avatarUrl);
            return ResponseEntity.ok(user);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @GetMapping("/page")
    public ResponseEntity<Page<User>> getUsers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "5") int size) {
        
        Pageable pageable = PageRequest.of(page, size);
        Page<User> usersPage = userService.getAllUsersWithPagination(pageable);
        
        return ResponseEntity.ok(usersPage);
    }

    @PostMapping("/login")
    public ResponseEntity<String> login(@RequestParam String name, 
                                        @RequestParam String password) {
        boolean isSuccess = userService.login(name, password);
        if (isSuccess) {
            return ResponseEntity.ok("Login thành công!");
        } else {
            return ResponseEntity.status(401).body("Tài khoản hoặc mật khẩu không đúng.");
        }
    }
}
