package com.example.catalog.service;

import com.example.catalog.document.ProductDocument;
import com.example.catalog.entity.Product;
import com.example.catalog.event.ProductIndexEvent;
import com.example.catalog.repository.ProductRepository;
import com.example.catalog.repository.ProductSearchRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Keeps Elasticsearch in step with SQL. Runs after commit, on another thread,
 * with a few retries. If every retry fails, POST /api/v1/admin/products/reindex
 * repairs the index.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ProductIndexer {

    private static final int MAX_ATTEMPTS = 3;

    private final ProductRepository productRepository;
    private final ProductSearchRepository searchRepository;
    private final ProductEmbeddingService embeddingService;
    private final ProductDocumentMapper mapper;

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onProductChanged(ProductIndexEvent event) {

        for (int attempt = 1; attempt <= MAX_ATTEMPTS; attempt++) {
            try {
                apply(event);
                return;
            } catch (Exception ex) {
                if (attempt == MAX_ATTEMPTS) {
                    log.error("Indexing FAILED for product {} ({}). "
                                    + "Run POST /api/v1/admin/products/reindex to repair.",
                            event.productId(), event.action(), ex);
                    return;
                }
                log.warn("Indexing attempt {}/{} failed for product {}: {}",
                        attempt, MAX_ATTEMPTS, event.productId(), ex.getMessage());
                try {
                    Thread.sleep(1000L * attempt);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    return;
                }
            }
        }
    }

    private void apply(ProductIndexEvent event) {

        Long id = event.productId();

        if (event.action() == ProductIndexEvent.Action.DELETE) {
            searchRepository.deleteById(id);
            log.info("Removed product {} from Elasticsearch", id);
            return;
        }

        Product product = productRepository.findById(id).orElse(null);
        if (product == null) {
            log.warn("Product {} no longer exists; skipping index", id);
            return;
        }

        float[] embedding = null;

        if (!event.reembed()) {
            embedding = searchRepository.findById(id)
                    .map(ProductDocument::getEmbedding)
                    .orElse(null);
        }
        if (embedding == null) {
            embedding = embeddingService.generateEmbedding(product);
        }

        searchRepository.save(mapper.toDocument(product, embedding));
        log.info("Indexed product {} in Elasticsearch", id);
    }
}
