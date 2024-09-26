package chatbox_api.config;

import chatbox_api.dto.ApiResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;

@ControllerAdvice
public class GlobalResponseHandler {

    // Tạo một phương thức chung để gói gọn phản hồi thành công
    public <T> ResponseEntity<ApiResponse<T>> createSuccessResponse(T body, String message, HttpStatus status) {
        ApiResponse<T> apiResponse = new ApiResponse<>(status.value(), message, body);
        return new ResponseEntity<>(apiResponse, status);
    }

    // Xử lý tất cả các ngoại lệ chung và định dạng lại phản hồi lỗi
    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ResponseEntity<ApiResponse<Object>> handleException(Exception ex) {
        ApiResponse<Object> apiResponse = new ApiResponse<>(HttpStatus.BAD_REQUEST.value(), ex.getMessage(), null);
        return new ResponseEntity<>(apiResponse, HttpStatus.BAD_REQUEST);
    }
}
