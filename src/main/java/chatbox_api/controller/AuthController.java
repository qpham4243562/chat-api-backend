package chatbox_api.controller;

import chatbox_api.config.GlobalResponseHandler;
import chatbox_api.dto.*;
import chatbox_api.model.User;
import chatbox_api.repository.UserRepository;
import chatbox_api.response.ApiResponse;
import chatbox_api.response.ValidationError;
import chatbox_api.service.EmailService;
import chatbox_api.service.MyUserDetailsService;
import chatbox_api.util.JWTUtil;
import com.mongodb.DuplicateKeyException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
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

    @Autowired
    private GlobalResponseHandler globalResponseHandler;

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
    public ResponseEntity<?> register(@Valid @RequestBody User user, BindingResult result) {
        // Kiểm tra lỗi xác thực
        if (result.hasErrors()) {
            List<ValidationError> errors = result.getFieldErrors().stream()
                    .map(fieldError -> new ValidationError(
                            fieldError.getField(),
                            fieldError.getDefaultMessage(),
                            fieldError.getRejectedValue()))
                    .toList();

            // Trả về ApiResponse với danh sách lỗi đơn giản
            return globalResponseHandler.createSuccessResponse(errors, "Validation error", HttpStatus.BAD_REQUEST);
        }

        if (userRepository.findByUsername(user.getUsername()).isPresent()) {
            return globalResponseHandler.createSuccessResponse(null, "Username is already taken", HttpStatus.BAD_REQUEST);
        }

        if (userRepository.findByEmail(user.getEmail()).isPresent()) {
            return globalResponseHandler.createSuccessResponse(null, "Email is already taken", HttpStatus.BAD_REQUEST);
        }

        try {
            String activationCode = generateActivationCode();
            user.setPassword(passwordEncoder.encode(user.getPassword()));
            user.setActivationCode(activationCode);
            userRepository.save(user);

            String subject = "Account Activation Code";
            String text = "Your activation code is: " + activationCode;
            emailService.sendEmail(user.getEmail(), subject, text);

            // Trả về ApiResponse với kiểu dữ liệu User khi đăng ký thành công
            return globalResponseHandler.createSuccessResponse(user, "User registered successfully. Please check your email for the activation code.", HttpStatus.OK);
        } catch (DuplicateKeyException e) {
            return globalResponseHandler.createSuccessResponse(null, "An unexpected error occurred", HttpStatus.INTERNAL_SERVER_ERROR);
        }
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
    public ResponseEntity<ApiResponse<AuthenticationResponse>> createAuthenticationToken(@RequestBody User user, HttpServletResponse response) {
        Optional<User> dbUserOptional = userRepository.findByUsername(user.getUsername());

        if (!dbUserOptional.isPresent()) {
            return globalResponseHandler.createSuccessResponse(null, "Incorrect username or password", HttpStatus.BAD_REQUEST);
        }

        User dbUser = dbUserOptional.get();

        if (!passwordEncoder.matches(user.getPassword(), dbUser.getPassword())) {
            return globalResponseHandler.createSuccessResponse(null, "Incorrect username or password", HttpStatus.BAD_REQUEST);
        }

        if (!dbUser.isActive()) {
            return globalResponseHandler.createSuccessResponse(null, "Account is not activated. Please check your email for the activation code.", HttpStatus.BAD_REQUEST);
        }

        final UserDetails userDetails = myUserDetailsService.loadUserByUsername(user.getUsername());
        final String jwt = jwtUtil.generateToken(userDetails);

        Cookie cookie = new Cookie("JWT_TOKEN", jwt);
        cookie.setHttpOnly(true);
        cookie.setSecure(true);
        cookie.setMaxAge(24 * 60 * 60);
        cookie.setPath("/");
        response.addCookie(cookie);

        return globalResponseHandler.createSuccessResponse(new AuthenticationResponse(jwt), "Authenticated successfully", HttpStatus.OK);
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
    public ResponseEntity<ApiResponse<String>> logout(HttpServletResponse response) {
        Cookie jwtCookie = new Cookie("JWT_TOKEN", null);
        jwtCookie.setHttpOnly(true);
        jwtCookie.setMaxAge(0);
        jwtCookie.setPath("/");
        response.addCookie(jwtCookie);

        return globalResponseHandler.createSuccessResponse("Logged out successfully", "Success", HttpStatus.OK);
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
    public ResponseEntity<ApiResponse<String>> activateAccount(@RequestBody ActivateAccountRequest request) {
        String username = request.getUsername();
        String activationCode = request.getActivationCode();

        Optional<User> optionalUser = userRepository.findByUsername(username);

        if (optionalUser.isPresent()) {
            User user = optionalUser.get();
            if (user.getActivationCode().equals(activationCode)) {
                user.setActive(true);
                user.setActivationCode(null);
                userRepository.save(user);

                return globalResponseHandler.createSuccessResponse("Account activated successfully.", "Success", HttpStatus.OK);
            } else {
                return globalResponseHandler.createSuccessResponse(null, "Invalid activation code.", HttpStatus.BAD_REQUEST);
            }
        } else {
            return globalResponseHandler.createSuccessResponse(null, "User not found.", HttpStatus.BAD_REQUEST);
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
    public ResponseEntity<ApiResponse<String>> generateNewActivationCode(@RequestBody GenerateNewCodeRequest request) {
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

            return globalResponseHandler.createSuccessResponse("New activation code generated and sent to your email.", "Success", HttpStatus.OK);
        } else {
            return globalResponseHandler.createSuccessResponse(null, "User with this email not found.", HttpStatus.BAD_REQUEST);
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
    public ResponseEntity<ApiResponse<String>> forgotPassword(@RequestBody ForgotPasswordRequest request) {
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

            return globalResponseHandler.createSuccessResponse("Password reset link has been sent to your email.", "Success", HttpStatus.OK);
        } else {
            return globalResponseHandler.createSuccessResponse(null, "User with this email not found.", HttpStatus.BAD_REQUEST);
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
    public ResponseEntity<ApiResponse<String>> resetPassword(@RequestBody ResetPasswordRequest request) {
        String token = request.getToken();
        String newPassword = request.getNewPassword();

        Optional<User> optionalUser = userRepository.findByResetToken(token);

        if (optionalUser.isPresent()) {
            User user = optionalUser.get();

            user.setPassword(passwordEncoder.encode(newPassword));
            user.setResetToken(null);
            userRepository.save(user);

            return globalResponseHandler.createSuccessResponse("Password has been reset successfully.", "Success", HttpStatus.OK);
        } else {
            return globalResponseHandler.createSuccessResponse(null, "Invalid password reset token.", HttpStatus.BAD_REQUEST);
        }
    }


    private String generateResetToken() {
        return UUID.randomUUID().toString();
    }
    @GetMapping("/login/google")
    public void redirectToGoogle(HttpServletResponse response) throws IOException {
        // Điều hướng người dùng đến trang đăng nhập Google
        response.sendRedirect("/oauth2/authorization/google");
    }
    @GetMapping("/loginSuccess")
    public ResponseEntity<Map<String, String>> loginSuccess() {
        Map<String, String> response = new HashMap<>();
        response.put("message", "Login with Google was successful!");
        return ResponseEntity.ok(response);
    }

    @GetMapping("/loginFailure")
    public ResponseEntity<Map<String, String>> loginFailure() {
        Map<String, String> response = new HashMap<>();
        response.put("message", "Login with Google failed!");
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
    }

}
