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

import java.util.Base64;
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

        // Thêm log để theo dõi
        System.out.println("Sending request to Google AI with text: " + inputText);
        ResponseEntity<String> response = restTemplate.exchange(
                url,
                HttpMethod.POST,
                entity,
                String.class
        );

        String result = extractTextFromResponse(response.getBody());
        System.out.println("Extracted response from Google AI: " + result);
        return result;
    }


    public String callGeminiApiWithImage(byte[] imageBytes) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Content-Type", "application/json");

        String base64Image = Base64.getEncoder().encodeToString(imageBytes);

        Map<String, Object> imagePart = new HashMap<>();
        imagePart.put("inline_data", Map.of(
                "mime_type", "image/jpeg",
                "data", base64Image
        ));

        Map<String, Object> textPart = new HashMap<>();
        textPart.put("text", "What's in this image?");

        List<Map<String, Object>> parts = List.of(imagePart, textPart);

        Map<String, Object> content = new HashMap<>();
        content.put("parts", parts);

        Map<String, Object> body = new HashMap<>();
        body.put("contents", List.of(content));

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);

        String url = googleApiUrl + "?key=" + googleApiKey;
        ResponseEntity<String> response = restTemplate.exchange(
                url,
                HttpMethod.POST,
                entity,
                String.class
        );

        return extractTextFromResponse(response.getBody());
    }


    private String extractTextFromResponse(String responseBody) {
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
