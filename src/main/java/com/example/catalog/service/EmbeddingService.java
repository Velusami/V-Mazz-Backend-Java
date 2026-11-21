package com.example.catalog.service;

import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.stereotype.Service;

@Service
public class EmbeddingService {

    // nomic-embed-text is trained with task prefixes. Documents and queries
    // must use matching prefixes (see ProductEmbeddingService for documents).
    static final String QUERY_PREFIX = "search_query: ";

    private final EmbeddingModel embeddingModel;

    public EmbeddingService(EmbeddingModel embeddingModel) {
        this.embeddingModel = embeddingModel;
    }

    /** Raw embedding, no prefix. Used by the /embedding debug endpoint. */
    public float[] generateEmbedding(String text) {
        return embeddingModel.embed(text);
    }

    /** Embedding for a user search query. */
    public float[] generateQueryEmbedding(String text) {
        return embeddingModel.embed(QUERY_PREFIX + text.trim());
    }
}
