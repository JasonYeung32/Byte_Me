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
    private final OrganisationRepository orgRepo;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    public AuthController(UserAccountRepository userRepo, SellerRepository sellerRepo,
                          OrganisationRepository orgRepo, PasswordEncoder passwordEncoder, JwtUtil jwtUtil) {
        this.userRepo = userRepo;
        this.sellerRepo = sellerRepo;
        this.orgRepo = orgRepo;
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

        UUID profileId = null;

        if (req.getRole() == UserAccount.Role.SELLER) {
            Seller seller = new Seller();
            seller.setUser(user);
            seller.setName(req.getBusinessName());
            seller.setLocationText(req.getLocation());
            sellerRepo.save(seller);
            profileId = seller.getSellerId();
        } else if (req.getRole() == UserAccount.Role.ORG_ADMIN) {
            Organisation org = new Organisation();
            org.setUser(user);
            org.setName(req.getBusinessName());
            org.setLocationText(req.getLocation());
            org.setBillingEmail(req.getEmail());
            orgRepo.save(org);
            profileId = org.getOrgId();
        }

        String token = jwtUtil.generateToken(user.getUserId(), user.getEmail(), user.getRole());
        return ResponseEntity.ok(new AuthResponse(token, user.getUserId(), profileId, user.getEmail(), user.getRole()));
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest req) {
        var user = userRepo.findByEmail(req.getEmail()).orElse(null);

        if (user == null || !passwordEncoder.matches(req.getPassword(), user.getPasswordHash())) {
            return ResponseEntity.status(401).body("Invalid credentials");
        }

        if (req.getRole() != null && user.getRole() != req.getRole()) {
            String expectedType = req.getRole() == UserAccount.Role.SELLER ? "seller" : "organization";
            return ResponseEntity.status(401).body("This account is not a " + expectedType + " account");
        }

        UUID profileId = null;
        if (user.getRole() == UserAccount.Role.SELLER) {
            var seller = sellerRepo.findByUserUserId(user.getUserId()).orElse(null);
            if (seller != null) profileId = seller.getSellerId();
        } else if (user.getRole() == UserAccount.Role.ORG_ADMIN) {
            var org = orgRepo.findByUserUserId(user.getUserId()).orElse(null);
            if (org != null) profileId = org.getOrgId();
        }

        String token = jwtUtil.generateToken(user.getUserId(), user.getEmail(), user.getRole());
        return ResponseEntity.ok(new AuthResponse(token, user.getUserId(), profileId, user.getEmail(), user.getRole()));
    }

    @GetMapping("/me")
    public ResponseEntity<?> me() {
        UUID userId = (UUID) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        var user = userRepo.findById(userId).orElseThrow();

        UUID profileId = null;
        if (user.getRole() == UserAccount.Role.SELLER) {
            var seller = sellerRepo.findByUserUserId(userId).orElse(null);
            if (seller != null) profileId = seller.getSellerId();
        } else if (user.getRole() == UserAccount.Role.ORG_ADMIN) {
            var org = orgRepo.findByUserUserId(userId).orElse(null);
            if (org != null) profileId = org.getOrgId();
        }

        return ResponseEntity.ok(new AuthResponse(null, user.getUserId(), profileId, user.getEmail(), user.getRole()));
    }

    // DTOs
    public static class RegisterRequest {
        private String email;
        private String password;
        private UserAccount.Role role;
        private String businessName;
        private String location;

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
        private UserAccount.Role role;

        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }
        public String getPassword() { return password; }
        public void setPassword(String password) { this.password = password; }
        public UserAccount.Role getRole() { return role; }
        public void setRole(UserAccount.Role role) { this.role = role; }
    }

    public static class AuthResponse {
        private String token;
        private UUID userId;
        private UUID profileId;
        private String email;
        private UserAccount.Role role;

        public AuthResponse(String token, UUID userId, UUID profileId, String email, UserAccount.Role role) {
            this.token = token;
            this.userId = userId;
            this.profileId = profileId;
            this.email = email;
            this.role = role;
        }

        public String getToken() { return token; }
        public UUID getUserId() { return userId; }
        public UUID getProfileId() { return profileId; }
        public String getEmail() { return email; }
        public UserAccount.Role getRole() { return role; }
    }
}
