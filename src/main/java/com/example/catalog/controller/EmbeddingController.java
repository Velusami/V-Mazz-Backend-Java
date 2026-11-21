package com.example.catalog.controller;

import com.example.catalog.service.EmbeddingService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/ai")
public class EmbeddingController {

    private final EmbeddingService embeddingService;

    public EmbeddingController(EmbeddingService embeddingService) {
        this.embeddingService = embeddingService;
    }

    @PostMapping("/embedding")
    public Map<String, Object> generateEmbedding(
            @RequestBody Map<String, String> request) {

        String text = request.get("text");

        float[] embedding =
                embeddingService.generateEmbedding(text);

        return Map.of(
                "text", text,
                "dimensions", embedding.length,
                "embedding", embedding
        );
    }
}
