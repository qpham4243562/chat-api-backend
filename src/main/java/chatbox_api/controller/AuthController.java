package chatbox_api.controller;

import chatbox_api.dto.ActivateAccountRequest;
import chatbox_api.dto.ForgotPasswordRequest;
import chatbox_api.dto.GenerateNewCodeRequest;
import chatbox_api.dto.ResetPasswordRequest;
import chatbox_api.model.User;
import chatbox_api.repository.UserRepository;
import chatbox_api.service.EmailService;
import chatbox_api.service.MyUserDetailsService;
import chatbox_api.util.JWTUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
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

    @Operation(summary = "Register a new user", description = "API to register a new user",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(content = @Content(
                    mediaType = "application/json",
                    examples = @ExampleObject(value = """
                {
                    "username": "john_doe",
                    "password": "password123",
                    "email": "john.doe@example.com"
                }
            """)
            ))
    )
    @PostMapping("/register")
    public Map<String, String> register(@RequestBody User user) {
        String activationCode = generateActivationCode();
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        user.setActivationCode(activationCode);
        userRepository.save(user);

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
            sb.append(random.nextInt(10));
        }
        return sb.toString();
    }

    @Operation(summary = "Authenticate a user", description = "API to authenticate and generate JWT token",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(content = @Content(
                    mediaType = "application/json",
                    examples = @ExampleObject(value = """
                {
                    "username": "john_doe",
                    "password": "password123"
                }
            """)
            ))
    )
    @PostMapping("/authenticate")
    public ResponseEntity<?> createAuthenticationToken(@RequestBody User user, HttpServletResponse response) throws Exception {
        Optional<User> dbUserOptional = userRepository.findByUsername(user.getUsername());

        if (!dbUserOptional.isPresent()) {
            throw new Exception("Incorrect username or password");
        }

        User dbUser = dbUserOptional.get();

        if (!passwordEncoder.matches(user.getPassword(), dbUser.getPassword())) {
            throw new Exception("Incorrect username or password");
        }

        if (!dbUser.isActive()) {
            return ResponseEntity.badRequest().body("Account is not activated. Please check your email for the activation code.");
        }

        final UserDetails userDetails = myUserDetailsService.loadUserByUsername(user.getUsername());
        final String jwt = jwtUtil.generateToken(userDetails);

        Cookie cookie = new Cookie("JWT_TOKEN", jwt);
        cookie.setHttpOnly(true);
        cookie.setSecure(true);
        cookie.setMaxAge(24 * 60 * 60);
        cookie.setPath("/");
        response.addCookie(cookie);

        return ResponseEntity.ok(new AuthenticationResponse(jwt));
    }

    public class AuthenticationResponse {
        private final String jwt;

        public AuthenticationResponse(String jwt) {
            this.jwt = jwt;
        }

        public String getJwt() {
            return jwt;
        }
    }

    @PostMapping("/logout")
    public Map<String, String> logout(HttpServletResponse response) {
        Cookie jwtCookie = new Cookie("JWT_TOKEN", null);
        jwtCookie.setHttpOnly(true);
        jwtCookie.setMaxAge(0);
        jwtCookie.setPath("/");
        response.addCookie(jwtCookie);

        Map<String, String> result = new HashMap<>();
        result.put("message", "Logged out successfully");
        return result;
    }

    @Operation(summary = "Activate account", description = "API to activate a user account",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(content = @Content(
                    mediaType = "application/json",
                    examples = @ExampleObject(value = """
        {
            "username": "john_doe",
            "activationCode": "123456"
        }
        """)
            ))
    )
    @PostMapping("/activate")
    public Map<String, String> activateAccount(@RequestBody ActivateAccountRequest request) {
        String username = request.getUsername();
        String activationCode = request.getActivationCode();

        Optional<User> optionalUser = userRepository.findByUsername(username);

        if (optionalUser.isPresent()) {
            User user = optionalUser.get();
            if (user.getActivationCode().equals(activationCode)) {
                user.setActive(true);
                user.setActivationCode(null);
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


    @Operation(summary = "Generate new activation code", description = "API to generate a new activation code and send via email",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(content = @Content(
                    mediaType = "application/json",
                    examples = @ExampleObject(value = """
        {
            "email": "john.doe@example.com"
        }
        """)
            ))
    )
    @PostMapping("/generate-new-code")
    public Map<String, String> generateNewActivationCode(@RequestBody GenerateNewCodeRequest request) {
        String email = request.getEmail();

        Optional<User> optionalUser = userRepository.findByEmail(email);

        if (optionalUser.isPresent()) {
            User user = optionalUser.get();

            String newActivationCode = generateActivationCode();
            user.setActivationCode(newActivationCode);
            userRepository.save(user);

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


    @Operation(summary = "Forgot password", description = "API to handle forgot password and send reset link",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(content = @Content(
                    mediaType = "application/json",
                    examples = @ExampleObject(value = """
        {
            "email": "john.doe@example.com"
        }
        """)
            ))
    )
    @PostMapping("/forgot-password")
    public ResponseEntity<?> forgotPassword(@RequestBody ForgotPasswordRequest request) {
        String email = request.getEmail();

        Optional<User> optionalUser = userRepository.findByEmail(email);

        if (optionalUser.isPresent()) {
            User user = optionalUser.get();

            String resetToken = generateResetToken();
            user.setResetToken(resetToken);
            userRepository.save(user);

            String resetLink = "http://your-app-url/reset-password?token=" + resetToken;
            String subject = "Password Reset Request";
            String text = "Click the link below to reset your password: \n" + resetLink;
            emailService.sendEmail(user.getEmail(), subject, text);

            return ResponseEntity.ok("Password reset link has been sent to your email.");
        } else {
            return ResponseEntity.status(404).body("User with this email not found.");
        }
    }


    @Operation(summary = "Reset password", description = "API to reset a user's password",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(content = @Content(
                    mediaType = "application/json",
                    examples = @ExampleObject(value = """
        {
            "token": "abc123xyz",
            "newPassword": "newStrongPassword123"
        }
        """)
            ))
    )
    @PostMapping("/reset-password")
    public ResponseEntity<?> resetPassword(@RequestBody ResetPasswordRequest request) {
        String token = request.getToken();
        String newPassword = request.getNewPassword();

        Optional<User> optionalUser = userRepository.findByResetToken(token);

        if (optionalUser.isPresent()) {
            User user = optionalUser.get();

            user.setPassword(passwordEncoder.encode(newPassword));
            user.setResetToken(null);
            userRepository.save(user);

            return ResponseEntity.ok("Password has been reset successfully.");
        } else {
            return ResponseEntity.status(404).body("Invalid password reset token.");
        }
    }


    private String generateResetToken() {
        return UUID.randomUUID().toString();
    }
}
