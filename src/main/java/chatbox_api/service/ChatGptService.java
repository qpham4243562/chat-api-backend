package chatbox_api.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

@Service
public class ChatGptService {

    @Value("${openai.api.key}")
    private String openaiApiKey;

    @Value("${openai.api.url}")
    private String openaiApiUrl;

    private final RestTemplate restTemplate;

    public ChatGptService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public String chatWithGpt(String message) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + openaiApiKey);
        headers.set("Content-Type", "application/json");

        Map<String, Object> body = new HashMap<>();
        body.put("model", "gpt-3.5-turbo");
        body.put("messages", new Map[] { Map.of("role", "user", "content", message) });

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);
        ResponseEntity<String> response = restTemplate.exchange(openaiApiUrl, HttpMethod.POST, entity, String.class);

        return response.getBody();
    }
}
