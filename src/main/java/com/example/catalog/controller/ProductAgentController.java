package com.example.catalog.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/ai/agent")
@Tag(name = "Product Agent", description = "Conversational catalog assistant")
public class ProductAgentController {

    private final ChatClient agent;
    private final ChatMemory chatMemory;

    public ProductAgentController(ChatClient productAgentChatClient, ChatMemory chatMemory) {
        this.agent = productAgentChatClient;
        this.chatMemory = chatMemory;
    }

    public record ChatRequest(@NotBlank String question, String conversationId) { }

    public record ChatResponse(String conversationId, String answer) { }

    @PostMapping("/chat")
    @Operation(summary = "Ask the agent (omit conversationId to start a new conversation)")
    public ChatResponse chat(@Valid @RequestBody ChatRequest request) {

        String conversationId = hasText(request.conversationId())
                ? request.conversationId()
                : UUID.randomUUID().toString();

        String answer = prompt(request.question(), conversationId).call().content();

        return new ChatResponse(conversationId, answer == null ? "" : answer);
    }

    @PostMapping(value = "/chat/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @Operation(summary = "Ask the agent, streaming the answer (conversationId required)")
    public Flux<String> stream(@Valid @RequestBody ChatRequest request) {

        if (!hasText(request.conversationId())) {
            throw new IllegalArgumentException(
                    "conversationId is required for streaming");
        }
        return prompt(request.question(), request.conversationId()).stream().content();
    }

    @DeleteMapping("/memory/{conversationId}")
    @Operation(summary = "Forget a conversation")
    public Map<String, String> clearMemory(@PathVariable String conversationId) {
        chatMemory.clear(conversationId);
        return Map.of("conversationId", conversationId, "message", "Conversation memory cleared");
    }

    // The question goes in as a parameter so braces in user text are never
    // interpreted as template variables.
    private ChatClient.ChatClientRequestSpec prompt(String question, String conversationId) {
        return agent.prompt()
                .user(u -> u.text("{question}").param("question", question))
                .advisors(a -> a.param(ChatMemory.CONVERSATION_ID, conversationId));
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
