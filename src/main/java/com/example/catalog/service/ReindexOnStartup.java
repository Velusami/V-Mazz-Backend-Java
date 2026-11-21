package com.example.catalog.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Dev convenience: H2 is in-memory and re-seeded from data.sql on every start,
 * so push those rows into Elasticsearch. Turn off in production
 * (app.reindex-on-startup=false) - it calls the embedding model per product.
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.reindex-on-startup", havingValue = "true")
public class ReindexOnStartup implements ApplicationRunner {

    private final ProductReindexService reindexService;

    @Override
    public void run(ApplicationArguments args) {
        try {
            int count = reindexService.reindexAll(false);
            log.info("Startup reindex complete: {} products", count);
        } catch (Exception e) {
            // Never block application start because Ollama/Elasticsearch is down.
            log.error("Startup reindex failed - are Elasticsearch and Ollama running?", e);
        }
    }
}
