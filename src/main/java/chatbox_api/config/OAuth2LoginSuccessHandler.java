package chatbox_api.config;

import chatbox_api.model.User;
import chatbox_api.repository.UserRepository;
import chatbox_api.response.ApiResponse;
import chatbox_api.util.JWTUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.Cookie;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.Collections;
import java.util.Optional;

@Component
public class OAuth2LoginSuccessHandler implements AuthenticationSuccessHandler {

    private final UserRepository userRepository;
    private final JWTUtil jwtUtil;
    private final ObjectMapper objectMapper;

    @Autowired
    public OAuth2LoginSuccessHandler(UserRepository userRepository, JWTUtil jwtUtil) {
        this.userRepository = userRepository;
        this.jwtUtil = jwtUtil;
        this.objectMapper = new ObjectMapper();
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException, ServletException {
        OAuth2AuthenticationToken oauth2Token = (OAuth2AuthenticationToken) authentication;

        // Lấy thông tin người dùng từ OAuth2
        String email = oauth2Token.getPrincipal().getAttribute("email");
        String name = oauth2Token.getPrincipal().getAttribute("name");

        // Tìm hoặc tạo mới người dùng trong database
        User user = userRepository.findByEmail(email).orElseGet(() -> registerNewUser(email, name));

        // Tạo JWT token cho người dùng
        String jwt = jwtUtil.generateToken(new org.springframework.security.core.userdetails.User(
                user.getUsername(), "", Collections.emptyList()));

        // Tạo và thêm cookie chứa JWT token vào phản hồi
        Cookie jwtCookie = new Cookie("JWT_TOKEN", jwt);
        jwtCookie.setHttpOnly(true);
        jwtCookie.setSecure(true); // Đặt là true nếu bạn sử dụng HTTPS
        jwtCookie.setMaxAge(24 * 60 * 60); // Thiết lập thời gian hết hạn, ví dụ: 1 ngày
        jwtCookie.setPath("/");
        response.addCookie(jwtCookie);

        // Tạo phản hồi ApiResponse trả về JWT và thông tin người dùng
        ApiResponse<Object> apiResponse = createApiResponse(user, jwt);

        // Thiết lập header phản hồi JSON
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        // Ghi dữ liệu JSON vào phản hồi
        objectMapper.writeValue(response.getWriter(), apiResponse);
    }


    // Phương thức hỗ trợ để đăng ký người dùng mới nếu không tồn tại
    private User registerNewUser(String email, String name) {
        User newUser = new User();
        newUser.setEmail(email);
        newUser.setUsername(name);
        newUser.setActive(true); // Đặt trạng thái kích hoạt nếu cần thiết
        return userRepository.save(newUser);
    }

    // Tạo ApiResponse bao gồm token và thông tin người dùng
    private ApiResponse<Object> createApiResponse(User user, String jwt) {
        // Đóng gói thông tin người dùng và JWT token vào ApiResponse
        return new ApiResponse<>(HttpStatus.OK.value(), "Authentication successful", new OAuth2Response(user, jwt));
    }

    // Lớp phụ để chứa thông tin phản hồi
    private static class OAuth2Response {
        private String jwtToken;
        private String userId;
        private String username;
        private String email;

        public OAuth2Response(User user, String jwtToken) {
            this.jwtToken = jwtToken;
            this.userId = user.getId();
            this.username = user.getUsername();
            this.email = user.getEmail();
        }

        public String getJwtToken() {
            return jwtToken;
        }

        public String getUserId() {
            return userId;
        }

        public String getUsername() {
            return username;
        }

        public String getEmail() {
            return email;
        }
    }
}
