package chatbox_api.config;

import chatbox_api.response.ApiResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

@Component
public class CustomAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final ObjectMapper objectMapper;

    public CustomAuthenticationEntryPoint() {
        this.objectMapper = new ObjectMapper();
    }

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response, AuthenticationException authException) throws IOException, ServletException {
        // Tạo ApiResponse với thông tin lỗi
        if (request.getRequestURI().startsWith("/v3/api-docs") || request.getRequestURI().startsWith("/swagger-ui")) {
            response.sendError(HttpServletResponse.SC_OK, "Access granted for Swagger");
            return;
        }
        ApiResponse<Object> apiResponse = new ApiResponse<>(
                HttpStatus.UNAUTHORIZED.value(),
                "Unauthorized access - Please log in.",
                null
        );

        // Thiết lập mã phản hồi và loại nội dung
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        // Ghi phản hồi JSON vào response
        objectMapper.writeValue(response.getWriter(), apiResponse);
    }
}