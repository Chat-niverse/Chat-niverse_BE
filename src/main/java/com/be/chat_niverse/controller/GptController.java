package com.be.chat_niverse.controller;

import com.be.chat_niverse.service.ChatGptService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/gpt")
public class GptController {
    private final ChatGptService chatGptService;

    public GptController(ChatGptService chatGptService){
        this.chatGptService = chatGptService;
    }

    @PostMapping
    public String chatWithGpt(@RequestParam String userInput){
        return chatGptService.getChatGptResponse(userInput);
    }
}