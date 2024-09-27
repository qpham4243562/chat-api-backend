package chatbox_api.config;

import chatbox_api.model.User;
import chatbox_api.repository.UserRepository;
import chatbox_api.util.JWTUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import jakarta.servlet.ServletException;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;


import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;


@Component
public class OAuth2LoginSuccessHandler implements AuthenticationSuccessHandler {

    private final UserRepository userRepository;
    private final JWTUtil jwtUtil;

    @Autowired
    public OAuth2LoginSuccessHandler(UserRepository userRepository, JWTUtil jwtUtil) {
        this.userRepository = userRepository;
        this.jwtUtil = jwtUtil;
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException, ServletException {
        OAuth2AuthenticationToken oauth2Token = (OAuth2AuthenticationToken) authentication;
        String email = oauth2Token.getPrincipal().getAttribute("email");
        String name = oauth2Token.getPrincipal().getAttribute("name");

        // Kiểm tra xem người dùng đã tồn tại trong DB hay chưa
        Optional<User> userOptional = userRepository.findByEmail(email);
        User user;

        if (userOptional.isPresent()) {
            user = userOptional.get();
        } else {
            // Tạo người dùng mới nếu chưa tồn tại
            user = new User();
            user.setEmail(email);
            user.setUsername(name);
            user.setActive(true); // Đặt trạng thái kích hoạt nếu cần thiết
            userRepository.save(user);
        }

        // Tạo JWT token
        String jwt = jwtUtil.generateToken(new org.springframework.security.core.userdetails.User(
                user.getUsername(), "", Collections.emptyList()));

        // Tạo phản hồi JSON trả về JWT và thông tin người dùng
        Map<String, String> jsonResponse = new HashMap<>();
        jsonResponse.put("jwtToken", jwt);
        jsonResponse.put("userId", user.getId());
        jsonResponse.put("username", user.getUsername());
        jsonResponse.put("email", user.getEmail());

        // Thiết lập header phản hồi JSON
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        // Ghi dữ liệu JSON vào phản hồi
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.writeValue(response.getWriter(), jsonResponse);
    }
}