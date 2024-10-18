package chatbox_api.controller;

import chatbox_api.service.GoogleAiService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;


@RestController
@RequestMapping("/api/gemini")
public class GoogleAiController {

    private final GoogleAiService googleAiService;

    public GoogleAiController(GoogleAiService googleAiService) {
        this.googleAiService = googleAiService;
    }

    @GetMapping("/generate")
    public String generateContent(@RequestParam String text) {
        // Convert the input text into the required format for Gemini API
        Map<String, String> userMessage = Map.of("role", "user", "content", text);

        // Create a list containing this single message
        List<Map<String, String>> messages = List.of(userMessage);

        // Pass the list to the GoogleAiService
        return googleAiService.callGeminiApi(messages);
    }


}
