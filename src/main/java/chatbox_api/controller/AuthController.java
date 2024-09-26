package chatbox_api.controller;

import chatbox_api.model.User;
import chatbox_api.repository.UserRepository;
import chatbox_api.service.EmailService;
import chatbox_api.service.MyUserDetailsService;
import chatbox_api.util.JWTUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/auth")
public class AuthController {

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private MyUserDetailsService myUserDetailsService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JWTUtil jwtUtil;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private EmailService emailService;

    @PostMapping("/register")
    public Map<String, String> register(@RequestBody User user) {
        String activationCode = generateActivationCode();
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        user.setActivationCode(activationCode);  // Store activation code
        userRepository.save(user);

        // Send the activation code to the user's email
        String subject = "Account Activation Code";
        String text = "Your activation code is: " + activationCode;
        emailService.sendEmail(user.getEmail(), subject, text);

        Map<String, String> response = new HashMap<>();
        response.put("message", "User registered successfully. Please check your email for the activation code.");
        return response;
    }

    private String generateActivationCode() {
        int codeLength = 6;
        Random random = new Random();
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < codeLength; i++) {
            sb.append(random.nextInt(10));  // Generate a random digit
        }
        return sb.toString();
    }

    // API Đăng nhập (Login)
    @Operation(security = { @SecurityRequirement(name = "bearerAuth"), @SecurityRequirement(name = "cookieAuth") })
    @PostMapping("/authenticate")
    public ResponseEntity<?> createAuthenticationToken(@RequestBody User user, HttpServletResponse response) throws Exception {
        Optional<User> dbUserOptional = userRepository.findByUsername(user.getUsername());  // Sử dụng Optional

        // Kiểm tra xem người dùng có tồn tại không
        if (!dbUserOptional.isPresent()) {
            throw new Exception("Incorrect username or password");
        }

        User dbUser = dbUserOptional.get();  // Lấy đối tượng User từ Optional

        // Kiểm tra mật khẩu
        if (!passwordEncoder.matches(user.getPassword(), dbUser.getPassword())) {
            throw new Exception("Incorrect username or password");
        }

        // Kiểm tra tài khoản có active hay chưa
        if (!dbUser.isActive()) {
            return ResponseEntity.badRequest().body("Account is not activated. Please check your email for the activation code.");
        }

        final UserDetails userDetails = myUserDetailsService.loadUserByUsername(user.getUsername());
        final String jwt = jwtUtil.generateToken(userDetails);

        Cookie cookie = new Cookie("JWT_TOKEN", jwt);
        cookie.setHttpOnly(true);
        cookie.setSecure(true);
        cookie.setMaxAge(24 * 60 * 60); // 1 day
        cookie.setPath("/");
        response.addCookie(cookie);

        return ResponseEntity.ok(new AuthenticationResponse(jwt));
    }



    // Tạo một lớp phản hồi mới
    public class AuthenticationResponse {
        private final String jwt;

        public AuthenticationResponse(String jwt) {
            this.jwt = jwt;
        }

        public String getJwt() {
            return jwt;
        }
    }

    // API Đăng xuất (Logout)
    @PostMapping("/logout")
    public Map<String, String> logout(HttpServletResponse response) {
        // Xóa cookie bằng cách đặt giá trị thời gian sống của cookie về 0
        Cookie jwtCookie = new Cookie("JWT_TOKEN", null);
        jwtCookie.setHttpOnly(true);
        jwtCookie.setMaxAge(0);  // Cookie hết hạn ngay lập tức
        jwtCookie.setPath("/");
        response.addCookie(jwtCookie);

        Map<String, String> result = new HashMap<>();
        result.put("message", "Logged out successfully");
        return result;
    }
    @PostMapping("/activate")
    public Map<String, String> activateAccount(@RequestBody Map<String, String> request) {
        String username = request.get("username");
        String activationCode = request.get("activationCode");

        // Sử dụng Optional để tìm User
        Optional<User> optionalUser = userRepository.findByUsername(username);

        if (optionalUser.isPresent()) {
            User user = optionalUser.get();
            // Kiểm tra mã kích hoạt
            if (user.getActivationCode().equals(activationCode)) {
                user.setActive(true);  // Kích hoạt tài khoản
                user.setActivationCode(null);  // Xóa mã kích hoạt sau khi đã kích hoạt
                userRepository.save(user);

                Map<String, String> response = new HashMap<>();
                response.put("message", "Account activated successfully.");
                return response;
            } else {
                Map<String, String> response = new HashMap<>();
                response.put("error", "Invalid activation code.");
                return response;
            }
        } else {
            Map<String, String> response = new HashMap<>();
            response.put("error", "User not found.");
            return response;
        }
    }
    @PostMapping("/generate-new-code")
    public Map<String, String> generateNewActivationCode(@RequestBody Map<String, String> request) {
        String email = request.get("email");

        // Tìm người dùng theo email
        Optional<User> optionalUser = userRepository.findByEmail(email);

        if (optionalUser.isPresent()) {
            User user = optionalUser.get();

            // Tạo mã kích hoạt mới
            String newActivationCode = generateActivationCode();
            user.setActivationCode(newActivationCode);

            // Lưu mã kích hoạt mới vào cơ sở dữ liệu
            userRepository.save(user);

            // Gửi mã kích hoạt mới qua email
            String subject = "Your New Activation Code";
            String text = "Your new activation code is: " + newActivationCode;
            emailService.sendEmail(user.getEmail(), subject, text);

            Map<String, String> response = new HashMap<>();
            response.put("message", "New activation code generated and sent to your email.");
            return response;
        } else {
            Map<String, String> response = new HashMap<>();
            response.put("error", "User with this email not found.");
            return response;
        }
    }
    @PostMapping("/forgot-password")
    public ResponseEntity<?> forgotPassword(@RequestBody Map<String, String> request) {
        String email = request.get("email");

        // Tìm người dùng theo email
        Optional<User> optionalUser = userRepository.findByEmail(email);

        if (optionalUser.isPresent()) {
            User user = optionalUser.get();

            // Tạo mã đặt lại mật khẩu hoặc liên kết đặt lại mật khẩu
            String resetToken = generateResetToken();
            user.setResetToken(resetToken);  // Lưu mã này vào cơ sở dữ liệu cho người dùng
            userRepository.save(user);

            // Gửi email chứa mã xác thực hoặc liên kết đặt lại mật khẩu
            String resetLink = "http://your-app-url/reset-password?token=" + resetToken;  // Liên kết đặt lại mật khẩu
            String subject = "Password Reset Request";
            String text = "Click the link below to reset your password: \n" + resetLink;
            emailService.sendEmail(user.getEmail(), subject, text);

            return ResponseEntity.ok("Password reset link has been sent to your email.");
        } else {
            return ResponseEntity.status(404).body("User with this email not found.");
        }
    }
    @PostMapping("/reset-password")
    public ResponseEntity<?> resetPassword(@RequestBody Map<String, String> request) {
        String token = request.get("token");
        String newPassword = request.get("newPassword");

        // Tìm người dùng dựa trên mã đặt lại mật khẩu (reset token)
        Optional<User> optionalUser = userRepository.findByResetToken(token);

        if (optionalUser.isPresent()) {
            User user = optionalUser.get();

            // Cập nhật mật khẩu mới
            user.setPassword(passwordEncoder.encode(newPassword));
            user.setResetToken(null);  // Xóa mã đặt lại sau khi đã sử dụng
            userRepository.save(user);

            return ResponseEntity.ok("Password has been reset successfully.");
        } else {
            return ResponseEntity.status(404).body("Invalid password reset token.");
        }
    }
    private String generateResetToken() {
        return UUID.randomUUID().toString();  // Tạo mã ngẫu nhiên
    }
}