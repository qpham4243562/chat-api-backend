package chatbox_api.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
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
    private static final int MAX_TOKENS = 30720;
    private static final int ESTIMATED_TOKENS_PER_MESSAGE = 150;
    private static final int MAX_MESSAGES = MAX_TOKENS / ESTIMATED_TOKENS_PER_MESSAGE;

    public GoogleAiService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public String callGeminiApi(List<Map<String, String>> messages) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Content-Type", "application/json");

        // Trim history if needed
        List<Map<String, String>> trimmedMessages = trimMessageHistory(messages);

        // Add system message if it doesn't exist
        if (trimmedMessages.isEmpty() || !trimmedMessages.get(0).get("role").equals("system")) {
            Map<String, String> systemMessage = new HashMap<>();
            systemMessage.put("role", "system");
            systemMessage.put("content", "You are Cherry, a helpful AI assistant. You should remember the context of our conversation and previous messages to provide relevant answers. Be concise but friendly in your responses.");
            trimmedMessages.add(0, systemMessage);
        }

        // Convert messages to Gemini API format
        List<Map<String, Object>> contents = new ArrayList<>();
        for (Map<String, String> message : trimmedMessages) {
            Map<String, Object> content = new HashMap<>();
            // Map roles correctly
            String role = mapRole(message.get("role"));
            content.put("role", role);
            content.put("parts", List.of(Map.of("text", message.get("content"))));
            contents.add(content);
        }

        Map<String, Object> body = new HashMap<>();
        body.put("contents", contents);

        // Add generation config
        Map<String, Object> generationConfig = new HashMap<>();
        generationConfig.put("temperature", 0.7);
        generationConfig.put("topP", 0.8);
        generationConfig.put("topK", 40);
        generationConfig.put("maxOutputTokens", 2048);
        body.put("generationConfig", generationConfig);

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);

        String url = googleApiUrl + "?key=" + googleApiKey;

        try {
            System.out.println("Sending request to Google AI with messages count: " + trimmedMessages.size());
            ResponseEntity<String> response = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    entity,
                    String.class
            );

            String result = extractTextFromResponse(response.getBody());
            System.out.println("Received response from Google AI");
            return result;
        } catch (Exception e) {
            System.err.println("Error calling Gemini API: " + e.getMessage());
            return handleError(e);
        }
    }

    private String mapRole(String originalRole) {
        return switch (originalRole.toLowerCase()) {
            case "system", "assistant", "model" -> "model";
            default -> "user";
        };
    }

    private List<Map<String, String>> trimMessageHistory(List<Map<String, String>> messages) {
        if (messages.size() <= MAX_MESSAGES) {
            return messages;
        }

        List<Map<String, String>> trimmed = new ArrayList<>();

        // Keep system message if it exists
        if (!messages.isEmpty() && "system".equals(messages.get(0).get("role"))) {
            trimmed.add(messages.get(0));
        }

        // Calculate how many recent messages we can keep
        int availableSlots = MAX_MESSAGES - trimmed.size();
        int startIndex = Math.max(messages.size() - availableSlots,
                trimmed.isEmpty() ? 0 : 1);

        // Add most recent messages
        trimmed.addAll(messages.subList(startIndex, messages.size()));

        return trimmed;
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

        // Add generation config for image analysis
        Map<String, Object> generationConfig = new HashMap<>();
        generationConfig.put("temperature", 0.4);  // Lower temperature for more focused image description
        generationConfig.put("maxOutputTokens", 1024);
        body.put("generationConfig", generationConfig);

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);

        String url = googleApiUrl + "?key=" + googleApiKey;

        try {
            ResponseEntity<String> response = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    entity,
                    String.class
            );

            return extractTextFromResponse(response.getBody());
        } catch (Exception e) {
            System.err.println("Error processing image: " + e.getMessage());
            return handleError(e);
        }
    }

    private String extractTextFromResponse(String responseBody) {
        ObjectMapper objectMapper = new ObjectMapper();
        try {
            JsonNode root = objectMapper.readTree(responseBody);
            JsonNode candidates = root.path("candidates");
            if (candidates.isEmpty()) {
                throw new RuntimeException("No response candidates found");
            }
            return candidates.get(0)
                    .path("content")
                    .path("parts")
                    .get(0)
                    .path("text")
                    .asText();
        } catch (Exception e) {
            System.err.println("Error extracting text from response: " + e.getMessage());
            throw new RuntimeException("Failed to process AI response", e);
        }
    }

    private String handleError(Exception e) {
        String errorMessage = e.getMessage();
        if (errorMessage.contains("429")) {
            return "I'm receiving too many requests right now. Please try again in a moment.";
        } else if (errorMessage.contains("400")) {
            return "I couldn't process that request. Please try rephrasing or simplifying your message.";
        } else {
            return "I encountered an error while processing your request. Please try again.";
        }
    }
}