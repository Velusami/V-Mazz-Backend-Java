package com.example.catalog.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.ai.ollama.OllamaChatModel;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/ai")
@RequiredArgsConstructor
public class AiController {
    private final OllamaChatModel chatModel;

    @GetMapping("/ask")
    public Map<String, String> ask(
            @RequestParam String question) {

        String answer = chatModel.call(question);

        return Map.of(
                "question", question,
                "answer", answer
        );
    }
}
