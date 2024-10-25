package chatbox_api.controller;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.http.ResponseEntity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import chatbox_api.service.GoogleAiService;
import chatbox_api.service.ConversationService;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

@RestController
@RequestMapping("/api/images")
@CrossOrigin(origins = "*")
public class ImageController {

    private static final Set<String> ALLOWED_CONTENT_TYPES = new HashSet<>(Arrays.asList(
            "image/jpeg",
            "image/png",
            "image/gif",
            "image/webp"
    ));

    private static final long MAX_FILE_SIZE = 5 * 1024 * 1024; // 5MB

    private final GoogleAiService googleAiService;
    private final ConversationService conversationService;

    @Autowired
    public ImageController(GoogleAiService googleAiService, ConversationService conversationService) {
        this.googleAiService = googleAiService;
        this.conversationService = conversationService;
    }

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> uploadImageForGemini(
            @RequestParam("file") MultipartFile file,
            @RequestParam("username") String username,
            @RequestParam("conversationId") String conversationId) {

        Map<String, String> response = new HashMap<>();

        try {
            // Validate inputs
            if (username == null || username.trim().isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "Username is required"));
            }

            if (conversationId == null || conversationId.trim().isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "Conversation ID is required"));
            }

            // Validate file
            if (file == null || file.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "No file uploaded"));
            }

            // Check file size
            if (file.getSize() > MAX_FILE_SIZE) {
                return ResponseEntity.badRequest().body(Map.of(
                        "error", "File size exceeds maximum limit of 5MB"
                ));
            }

            // Validate content type
            String contentType = file.getContentType();
            if (contentType == null || !ALLOWED_CONTENT_TYPES.contains(contentType.toLowerCase())) {
                return ResponseEntity.badRequest().body(Map.of(
                        "error", "Invalid file type. Allowed types: JPEG, PNG, GIF, WEBP"
                ));
            }

            // Process image
            byte[] imageBytes = file.getBytes();

            // Call Gemini API with retry mechanism
            String geminiResponse = null;
            int maxRetries = 3;
            int attempt = 0;
            Exception lastException = null;

            while (attempt < maxRetries && geminiResponse == null) {
                try {
                    geminiResponse = googleAiService.callGeminiApiWithImage(imageBytes);
                } catch (Exception e) {
                    lastException = e;
                    attempt++;
                    if (attempt < maxRetries) {
                        Thread.sleep(1000 * attempt); // Exponential backoff
                    }
                }
            }

            if (geminiResponse == null) {
                throw new RuntimeException("Failed to process image after " + maxRetries + " attempts", lastException);
            }

            // Save image and response to conversation
            conversationService.addMessageToConversation(
                    conversationId,
                    username,
                    "[Image uploaded]",
                    "image"
            );

            conversationService.addMessageToConversation(
                    conversationId,
                    "Cherry",
                    geminiResponse,
                    "text"
            );

            // Prepare success response
            Map<String, Object> successResponse = new HashMap<>();
            successResponse.put("message", geminiResponse);
            successResponse.put("status", "success");
            successResponse.put("imageProcessed", true);

            return ResponseEntity.ok(successResponse);

        } catch (IOException e) {
            return handleError("Error reading file: " + e.getMessage());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return handleError("Operation interrupted: " + e.getMessage());
        } catch (Exception e) {
            return handleError("Unexpected error: " + e.getMessage());
        }
    }

    private ResponseEntity<Map<String, String>> handleError(String message) {
        Map<String, String> error = new HashMap<>();
        error.put("error", message);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
    }
}