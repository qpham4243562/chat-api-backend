package chatbox_api.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class GoogleAiService {

    @Value("${google.ai.api.key}")
    private String googleApiKey;

    @Value("${google.ai.api.url}")
    private String googleApiUrl;

    private final RestTemplate restTemplate;

    public GoogleAiService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public String callGeminiApi(String inputText) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Content-Type", "application/json");

        Map<String, Object> body = new HashMap<>();
        body.put("contents", List.of(Map.of("parts", List.of(Map.of("text", inputText)))));

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);

        String url = googleApiUrl + "?key=" + googleApiKey;
        ResponseEntity<String> response = restTemplate.exchange(
                url,
                HttpMethod.POST,
                entity,
                String.class
        );

        // Trích xuất phần "text" từ phản hồi
        String responseBody = response.getBody();
        // Bạn cần phân tích JSON của phản hồi để lấy giá trị "text" từ các "candidates"
        // Đơn giản hóa: Giả sử rằng JSON phản hồi có thể phân tích và lấy phần "text"
        String extractedText = extractTextFromResponse(responseBody);

        return extractedText;
    }

    private String extractTextFromResponse(String responseBody) {
        // Giả sử bạn dùng ObjectMapper hoặc thư viện JSON khác để phân tích và lấy text
        // Đây là ví dụ về cách phân tích JSON:
        ObjectMapper objectMapper = new ObjectMapper();
        String text = "";
        try {
            JsonNode root = objectMapper.readTree(responseBody);
            text = root.path("candidates").get(0).path("content").path("parts").get(0).path("text").asText();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return text;
    }
}

