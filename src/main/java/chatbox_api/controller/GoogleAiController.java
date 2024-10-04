package chatbox_api.controller;

import chatbox_api.service.GoogleAiService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;



@RestController
@RequestMapping("/api/gemini")
public class GoogleAiController {

    private final GoogleAiService googleAiService;

    public GoogleAiController(GoogleAiService googleAiService) {
        this.googleAiService = googleAiService;
    }

    @GetMapping("/generate")
    public String generateContent(@RequestParam String text) {
        return googleAiService.callGeminiApi(text);
    }
}
