# Hướng dẫn chi tiết: Spring Boot + JPA (One-to-One) + Pagination + Transaction + Login

Dự án này sẽ giúp bạn hiểu rõ cách xây dựng một ứng dụng Spring Boot cơ bản bao gồm các khái niệm:
1. **JPA Entities & Relationships**: Quan hệ `@OneToOne` giữa `User` và `Profile`.
2. **Pagination (Phân trang)**: Lấy danh sách User theo từng trang.
3. **Transaction**: Đảm bảo tính toàn vẹn dữ liệu khi thêm hoặc cập nhật.
4. **Login**: Một chức năng đăng nhập đơn giản kiểm tra username/password.

## 1. Cấu trúc dự án
Bạn có thể tạo dự án thông qua [Spring Initializr](https://start.spring.io/) với các dependencies:
- Spring Web
- Spring Data JPA
- H2 Database (hoặc MySQL Driver nếu bạn dùng MySQL)
- Lombok (tùy chọn, để giảm boilerplate code)

---

## 2. Entities (Mô hình dữ liệu)

### `User.java`
Thêm trường `password` để làm chức năng đăng nhập đơn giản.
```java
package com.example.jpa.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "users")
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(unique = true, nullable = false)
    private String name;
    
    @Column(nullable = false)
    private String password;

    // Quan hệ 1-1 với Profile
    @OneToOne(cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JoinColumn(name = "profile_id", referencedColumnName = "id")
    private Profile profile;

    // Getters and Setters...
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
    public Profile getProfile() { return profile; }
    public void setProfile(Profile profile) { this.profile = profile; }
}
```

### `Profile.java`
```java
package com.example.jpa.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "profiles")
public class Profile {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    private String avatarUrl;
    private String bio;

    // mappedBy trỏ tới tên biến "profile" trong class User
    @OneToOne(mappedBy = "profile")
    private User user;

    // Getters and Setters...
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getAvatarUrl() { return avatarUrl; }
    public void setAvatarUrl(String avatarUrl) { this.avatarUrl = avatarUrl; }
    public String getBio() { return bio; }
    public void setBio(String bio) { this.bio = bio; }
    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }
}
```

---

## 3. Repositories (Truy xuất dữ liệu)

Sử dụng `JpaRepository` hỗ trợ sẵn phân trang (`Pageable`).

### `UserRepository.java`
```java
package com.example.jpa.repository;

import com.example.jpa.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    // Tìm user theo name (dùng cho chức năng login)
    Optional<User> findByName(String name);
}
```

---

## 4. Service layer (Logic nghiệp vụ & Transaction)

Ở đây ta sẽ sử dụng `@Transactional` để đảm bảo lưu `User` và `Profile` một cách an toàn.

### `UserService.java`
```java
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

    /**
     * Tạo User và Profile cùng lúc.
     * @Transactional đảm bảo nếu có lỗi xảy ra, toàn bộ thao tác sẽ bị rollback (hủy).
     */
    @Transactional
    public User createUser(String name, String password, String bio, String avatarUrl) {
        // Tạo profile
        Profile profile = new Profile();
        profile.setBio(bio);
        profile.setAvatarUrl(avatarUrl);

        // Tạo user
        User user = new User();
        user.setName(name);
        user.setPassword(password); // Thực tế cần mã hóa (BCrypt)
        user.setProfile(profile);

        // Liên kết 2 chiều
        profile.setUser(user);

        // Nhờ CascadeType.ALL, chỉ cần lưu User, Profile sẽ tự động được lưu.
        return userRepository.save(user);
    }

    /**
     * Phân trang danh sách User
     */
    public Page<User> getAllUsersWithPagination(Pageable pageable) {
        return userRepository.findAll(pageable);
    }

    /**
     * Chức năng Login đơn giản
     */
    public boolean login(String name, String password) {
        Optional<User> userOpt = userRepository.findByName(name);
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            // So sánh password (trong thực tế dùng passwordEncoder.matches)
            return user.getPassword().equals(password);
        }
        return false;
    }
}
```

---

## 5. Controller (REST APIs)

### `UserController.java`
```java
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

    // API 1: Tạo User mới (có Transactional)
    @PostMapping("/create")
    public ResponseEntity<User> createUser(@RequestParam String name, 
                                           @RequestParam String password, 
                                           @RequestParam String bio, 
                                           @RequestParam String avatarUrl) {
        User user = userService.createUser(name, password, bio, avatarUrl);
        return ResponseEntity.ok(user);
    }

    // API 2: Lấy danh sách User có phân trang (Pagination)
    @GetMapping("/page")
    public ResponseEntity<Page<User>> getUsers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "5") int size) {
        
        Pageable pageable = PageRequest.of(page, size);
        Page<User> usersPage = userService.getAllUsersWithPagination(pageable);
        
        return ResponseEntity.ok(usersPage);
    }

    // API 3: Login đơn giản
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
```

---

## 6. Cấu hình (`application.properties`)
Ví dụ cấu hình kết nối tới H2 Database để chạy test nhanh trên RAM (In-Memory).

```properties
# H2 Database
spring.datasource.url=jdbc:h2:mem:testdb
spring.datasource.driverClassName=org.h2.Driver
spring.datasource.username=sa
spring.datasource.password=

# Hibernate (Tự động tạo bảng dựa trên Entity)
spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=true

# H2 Console (Để vào xem database trên web: http://localhost:8080/h2-console)
spring.h2.console.enabled=true
```
