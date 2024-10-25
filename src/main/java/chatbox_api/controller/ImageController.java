package chatbox_api.controller;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.http.ResponseEntity;
import org.springframework.beans.factory.annotation.Autowired;
import chatbox_api.service.GoogleAiService;
import chatbox_api.service.ConversationService;

import java.io.IOException;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/images")
@CrossOrigin(origins = "*")
public class ImageController {

    private final GoogleAiService googleAiService;
    private final ConversationService conversationService;

    @Autowired
    public ImageController(GoogleAiService googleAiService, ConversationService conversationService) {
        this.googleAiService = googleAiService;
        this.conversationService = conversationService;
    }

    @PostMapping("/upload")
    public ResponseEntity<Map<String, String>> uploadImageForGemini(
            @RequestParam("file") MultipartFile file,
            @RequestParam("username") String username,
            @RequestParam("conversationId") String conversationId) {
        Map<String, String> response = new HashMap<>();

        if (file.isEmpty()) {
            response.put("message", "File is empty.");
            return ResponseEntity.badRequest().body(response);
        }

        try {
            byte[] imageBytes = file.getBytes();
            String mimeType = file.getContentType();

            if (mimeType == null || !mimeType.startsWith("image/")) {
                response.put("message", "Invalid file type. Please upload an image.");
                return ResponseEntity.badRequest().body(response);
            }

            String base64Image = Base64.getEncoder().encodeToString(imageBytes);

            // Save the image to the conversation
            conversationService.addImageToConversation(conversationId, username, base64Image);

            String geminiResponse = googleAiService.callGeminiApiWithImage(imageBytes);

            // Save the Gemini response to the conversation
            conversationService.addMessageToConversation(conversationId, "Cherry", geminiResponse, "text");

            response.put("message", geminiResponse);
            return ResponseEntity.ok(response);
        } catch (IOException e) {
            e.printStackTrace();
            response.put("message", "Error processing file: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }

    }

}