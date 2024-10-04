package chatbox_api.service;

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

        return response.getBody();
    }
}
