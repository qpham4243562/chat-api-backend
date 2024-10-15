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

import java.util.*;

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

    public String callGeminiApi(List<Map<String, String>> messages) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Content-Type", "application/json");

        List<Map<String, Object>> contents = new ArrayList<>();
        for (Map<String, String> message : messages) {
            Map<String, Object> content = new HashMap<>();
            content.put("role", message.get("role"));
            content.put("parts", List.of(Map.of("text", message.get("content"))));
            contents.add(content);
        }

        Map<String, Object> body = new HashMap<>();
        body.put("contents", contents);

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);

        String url = googleApiUrl + "?key=" + googleApiKey;

        System.out.println("Sending request to Google AI with messages: " + messages);
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
