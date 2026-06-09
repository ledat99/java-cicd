# Giải Thích Chi Tiết Mã Nguồn: Từ Con Số 0

Tài liệu này được viết theo hướng "cầm tay chỉ việc", giúp bạn hiểu rõ từng dòng code, tại sao lại viết như vậy, và tư duy để xây dựng một ứng dụng Spring Boot từ đầu.

## 1. Tư Duy Kiến Trúc (Mô Hình 3 Lớp)
Khi viết ứng dụng Spring Boot, chúng ta thường chia code thành 3 lớp (layer) chính để dễ quản lý. Hãy tưởng tượng ứng dụng của bạn như một **Nhà Hàng**:

- **Controller (Lễ tân/Bồi bàn)**: Nơi tiếp nhận yêu cầu (Request) từ người dùng (hoặc từ web/app). Lễ tân không tự nấu ăn, mà chỉ ghi nhận order và gọi đầu bếp.
- **Service (Đầu bếp)**: Nơi chứa "logic nghiệp vụ". Đầu bếp nhận order từ lễ tân, xử lý nguyên liệu, xào nấu (kiểm tra mật khẩu, tính toán...).
- **Repository (Thủ kho)**: Nơi chuyên làm việc với cơ sở dữ liệu (Kho chứa). Đầu bếp cần nguyên liệu gì thì bảo thủ kho đi lấy. Thủ kho chỉ biết cất/lấy dữ liệu chứ không biết nấu ăn.
- **Entity (Nguyên liệu/Món ăn)**: Định nghĩa hình thù của dữ liệu (Ví dụ: Thực thể User gồm có tên, mật khẩu...).

---

## 2. Lớp Entity: Định hình dữ liệu
*(Nằm trong package `entity`)*

Lớp này định nghĩa các bảng trong cơ sở dữ liệu. Ở đây ta có `User` và `Profile` (Mỗi người dùng có 1 hồ sơ cá nhân).

### File: `User.java`
```java
@Entity // Báo cho Spring biết đây là một thực thể, sẽ được tạo thành 1 bảng trong Database
@Table(name = "users") // Đặt tên bảng là "users"
public class User {
    
    @Id // Đây là khóa chính (Primary Key)
    @GeneratedValue(strategy = GenerationType.IDENTITY) // Khóa chính tự động tăng (1, 2, 3...)
    private Long id;
    
    @Column(unique = true, nullable = false) // Không được để trống (nullable = false) và không được trùng tên (unique = true)
    private String name;
    
    @Column(nullable = false)
    private String password;

    // Quan hệ 1-1 (Một User chỉ có 1 Profile).
    // cascade = CascadeType.ALL: Nghĩa là khi Lưu, Xóa, Sửa User thì Profile cũng bị thao tác theo. (Ví dụ Xóa User thì tự động xóa Profile).
    // fetch = FetchType.LAZY: Khi lấy User ra, chưa lấy Profile vội để tiết kiệm RAM. Khi nào gọi getProfile() mới chọc xuống DB lấy.
    @OneToOne(cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JoinColumn(name = "profile_id", referencedColumnName = "id") // Tạo một cột tên là "profile_id" trong bảng users để làm khóa ngoại
    private Profile profile;
}
```

### File: `Profile.java`
```java
@Entity
@Table(name = "profiles")
public class Profile {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    private String avatarUrl;
    private String bio;

    // mappedBy = "profile": Báo rằng mối quan hệ này đã được quản lý bởi biến "profile" bên class User. Không cần tạo thêm cột khóa ngoại bên bảng này nữa.
    @OneToOne(mappedBy = "profile")
    @com.fasterxml.jackson.annotation.JsonIgnore // Rất Quan Trọng: Ngăn chặn lỗi lặp vô hạn (Infinite Recursion) khi biến dữ liệu thành JSON để trả về cho người dùng.
    private User user;
}
```

---

## 3. Lớp Repository: Thủ Kho
*(Nằm trong package `repository`)*

### File: `UserRepository.java`
```java
@Repository // Đánh dấu đây là "Thủ kho"
// JpaRepository<User, Long>: Cung cấp sẵn các hàm như save(), findAll(), deleteById()... cho thực thể User (có khóa chính là Long) mà ta KHÔNG CẦN VIẾT SQL.
public interface UserRepository extends JpaRepository<User, Long> {
    
    // Spring Boot rất thông minh, bạn chỉ cần gõ "findByName", nó sẽ tự động dịch ra câu lệnh SQL: "SELECT * FROM users WHERE name = ?"
    Optional<User> findByName(String name);
}
```

---

## 4. Lớp Service: Đầu bếp xử lý logic
*(Nằm trong package `service`)*

### File: `UserService.java`
```java
@Service // Đánh dấu đây là "Đầu bếp"
public class UserService {

    @Autowired // Lấy "Thủ kho" (UserRepository) đưa vào đây để "Đầu bếp" có thể sai vặt
    private UserRepository userRepository;

    // Hàm tạo User
    @Transactional // RẤT QUAN TRỌNG: Giao dịch. Nếu trong lúc đang lưu User hoặc Profile mà bị sập điện hoặc có lỗi, toàn bộ dữ liệu sẽ được quay ngược (Rollback) lại như chưa hề có cuộc chia ly. Tránh tình trạng rác dữ liệu (Có User mà mất Profile).
    public User createUser(String name, String password, String bio, String avatarUrl) {
        Profile profile = new Profile();
        profile.setBio(bio);
        profile.setAvatarUrl(avatarUrl);

        User user = new User();
        user.setName(name);
        user.setPassword(password); // Thực tế mật khẩu phải mã hóa, không lưu thẳng chữ thường nhé
        user.setProfile(profile);

        profile.setUser(user); // Gắn ngược lại để trói chặt quan hệ 2 chiều

        // Chỉ cần lưu User, nhờ có CascadeType.ALL, Spring sẽ tự động hiểu và lưu luôn cả Profile vào DB
        return userRepository.save(user);
    }

    // Hàm phân trang
    // Pageable là một gói chứa thông tin "Đang ở trang số mấy?" và "Mỗi trang mấy người?".
    public Page<User> getAllUsersWithPagination(Pageable pageable) {
        return userRepository.findAll(pageable); // Lệnh findAll có sẵn của JpaRepository
    }

    // Hàm đăng nhập
    public boolean login(String name, String password) {
        Optional<User> userOpt = userRepository.findByName(name); // Tìm xem có user đó không
        if (userOpt.isPresent()) { // Nếu tìm thấy
            User user = userOpt.get();
            return user.getPassword().equals(password); // So sánh xem mật khẩu có giống không
        }
        return false; // Nếu không tìm thấy trả về false
    }
}
```

---

## 5. Lớp Controller: Lễ Tân Nhận Yêu Cầu
*(Nằm trong package `controller`)*

### File: `UserController.java`
```java
@RestController // Đánh dấu đây là "Lễ tân" chuyên trả về dữ liệu kiểu JSON (chuyên dùng cho API)
@RequestMapping("/api/users") // Mọi đường dẫn (URL) vào lễ tân này đều bắt đầu bằng "/api/users"
public class UserController {

    @Autowired // Gọi "Đầu bếp" (UserService) ra để làm việc
    private UserService userService;

    // @PostMapping: Hàm này sẽ bắt các yêu cầu gửi bằng phương thức POST (thường dùng để thêm mới/bảo mật)
    // Đường dẫn đầy đủ sẽ là: http://localhost:8081/api/users/create
    @PostMapping("/create")
    public ResponseEntity<User> createUser(
            @RequestParam String name, // @RequestParam: Bắt giá trị mà người dùng gửi lên qua URL (?name=...)
            @RequestParam String password, 
            @RequestParam String bio, 
            @RequestParam String avatarUrl) {
        // Sai "đầu bếp" làm món ăn
        User user = userService.createUser(name, password, bio, avatarUrl);
        
        // Trả kết quả (Món ăn) cho khách. 200 OK
        return ResponseEntity.ok(user);
    }

    // @GetMapping: Dùng để lấy dữ liệu (đọc)
    @GetMapping("/page")
    public ResponseEntity<Page<User>> getUsers(
            @RequestParam(defaultValue = "0") int page, // Nếu người dùng không gửi trang số mấy, mặc định là trang 0
            @RequestParam(defaultValue = "5") int size) {
        
        // Gói thông tin phân trang
        Pageable pageable = PageRequest.of(page, size);
        
        // Sai đầu bếp đi lấy dữ liệu có phân trang
        Page<User> usersPage = userService.getAllUsersWithPagination(pageable);
        
        return ResponseEntity.ok(usersPage);
    }

    // Hàm Login
    @PostMapping("/login")
    public ResponseEntity<String> login(@RequestParam String name, 
                                        @RequestParam String password) {
        boolean isSuccess = userService.login(name, password); // Nhờ đầu bếp check pass
        if (isSuccess) {
            return ResponseEntity.ok("Login thành công!");
        } else {
            // Trả về lỗi 401 Unauthorized nếu sai pass
            return ResponseEntity.status(401).body("Tài khoản hoặc mật khẩu không đúng.");
        }
    }
}
```

## Tổng Kết
Để một luồng chạy hoàn chỉnh:
1. Bạn gửi 1 yêu cầu (Request) từ trình duyệt hoặc Postman.
2. **Controller** đón lấy yêu cầu đó, kiểm tra xem bạn truyền thông tin gì.
3. Controller ném thông tin đó xuống **Service**.
4. **Service** suy nghĩ logic (tính toán, ghép nối dữ liệu...) rồi gọi **Repository**.
5. **Repository** sinh ra mã SQL, chọc vào Database lấy/lưu trữ dữ liệu **Entity** rồi trả ngược lại cho **Service**.
6. **Service** lấy được cục dữ liệu liền quăng ngược lên lại cho **Controller**.
7. **Controller** gói cục dữ liệu đó lại dưới dạng chuẩn (thường là JSON), kèm theo mã trạng thái (như 200 OK) rồi trả thẳng ra màn hình cho bạn.
