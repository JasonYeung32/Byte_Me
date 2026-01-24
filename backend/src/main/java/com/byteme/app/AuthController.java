package com.byteme.app;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final UserAccountRepository userRepo;
    private final SellerRepository sellerRepo;
    private final EmployeeRepository employeeRepo;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    public AuthController(UserAccountRepository userRepo, SellerRepository sellerRepo, 
                          EmployeeRepository employeeRepo, PasswordEncoder passwordEncoder, JwtUtil jwtUtil) {
        this.userRepo = userRepo;
        this.sellerRepo = sellerRepo;
        this.employeeRepo = employeeRepo;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody RegisterRequest req) {
        if (userRepo.existsByEmail(req.getEmail())) {
            return ResponseEntity.badRequest().body("Email already exists");
        }

        UserAccount user = new UserAccount();
        user.setEmail(req.getEmail());
        user.setPasswordHash(passwordEncoder.encode(req.getPassword()));
        user.setRole(req.getRole());
        userRepo.save(user);

        if (req.getRole() == UserAccount.Role.SELLER && req.getBusinessName() != null) {
            Seller seller = new Seller();
            seller.setUser(user);
            seller.setName(req.getBusinessName());
            seller.setLocationText(req.getLocation());
            sellerRepo.save(seller);
        }

        String token = jwtUtil.generateToken(user.getUserId(), user.getEmail(), user.getRole());
        return ResponseEntity.ok(new AuthResponse(token, user.getUserId(), user.getEmail(), user.getRole()));
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest req) {
        var user = userRepo.findByEmail(req.getEmail()).orElse(null);
        
        if (user == null || !passwordEncoder.matches(req.getPassword(), user.getPasswordHash())) {
            return ResponseEntity.status(401).body("Invalid credentials");
        }

        String token = jwtUtil.generateToken(user.getUserId(), user.getEmail(), user.getRole());
        return ResponseEntity.ok(new AuthResponse(token, user.getUserId(), user.getEmail(), user.getRole()));
    }

    @GetMapping("/me")
    public ResponseEntity<?> me() {
        UUID userId = (UUID) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        var user = userRepo.findById(userId).orElseThrow();
        return ResponseEntity.ok(new AuthResponse(null, user.getUserId(), user.getEmail(), user.getRole()));
    }

    // DTOs
    public static class RegisterRequest {
        private String email;
        private String password;
        private UserAccount.Role role;
        private String businessName;
        private String location;

        public RegisterRequest() {}

        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }
        public String getPassword() { return password; }
        public void setPassword(String password) { this.password = password; }
        public UserAccount.Role getRole() { return role; }
        public void setRole(UserAccount.Role role) { this.role = role; }
        public String getBusinessName() { return businessName; }
        public void setBusinessName(String businessName) { this.businessName = businessName; }
        public String getLocation() { return location; }
        public void setLocation(String location) { this.location = location; }
    }

    public static class LoginRequest {
        private String email;
        private String password;

        public LoginRequest() {}

        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }
        public String getPassword() { return password; }
        public void setPassword(String password) { this.password = password; }
    }

    public static class AuthResponse {
        private String token;
        private UUID userId;
        private String email;
        private UserAccount.Role role;

        public AuthResponse() {}

        public AuthResponse(String token, UUID userId, String email, UserAccount.Role role) {
            this.token = token;
            this.userId = userId;
            this.email = email;
            this.role = role;
        }

        public String getToken() { return token; }
        public void setToken(String token) { this.token = token; }
        public UUID getUserId() { return userId; }
        public void setUserId(UUID userId) { this.userId = userId; }
        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }
        public UserAccount.Role getRole() { return role; }
        public void setRole(UserAccount.Role role) { this.role = role; }
    }
}
