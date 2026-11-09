package ek.osnb.demo.coffeeshop.ai;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ChatController {
    private final AiClientService aiClientService;

    public ChatController(AiClientService aiClientService) {
        this.aiClientService = aiClientService;
    }

    @GetMapping("/ask")
    public String chat(@RequestParam String prompt) {
        return aiClientService.chat(prompt);
    }
}
