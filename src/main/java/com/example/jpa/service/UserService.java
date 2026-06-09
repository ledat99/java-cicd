package com.example.jpa.service;

import com.example.jpa.entity.Profile;
import com.example.jpa.entity.User;
import com.example.jpa.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
public class UserService {

    @Autowired
    private UserRepository userRepository;

    @Transactional
    public User createUser(String name, String password, String bio, String avatarUrl) {
        if (userRepository.findByName(name).isPresent()) {
            throw new IllegalArgumentException("Tên người dùng đã tồn tại: " + name);
        }

        Profile profile = new Profile();
        profile.setBio(bio);
        profile.setAvatarUrl(avatarUrl);

        User user = new User();
        user.setName(name);
        user.setPassword(password);
        user.setProfile(profile);

        profile.setUser(user);

        return userRepository.save(user);
    }

    public Page<User> getAllUsersWithPagination(Pageable pageable) {
        return userRepository.findAll(pageable);
    }

    public boolean login(String name, String password) {
        Optional<User> userOpt = userRepository.findByName(name);
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            return user.getPassword().equals(password);
        }
        return false;
    }
}
