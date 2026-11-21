package com.example.catalog.service;

import com.example.catalog.document.ProductDocument;
import com.example.catalog.entity.Product;
import com.example.catalog.repository.ProductRepository;
import com.example.catalog.repository.ProductSearchRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProductReindexService {

    private static final int BATCH_SIZE = 50;

    private final ProductRepository productRepository;
    private final ProductSearchRepository searchRepository;
    private final ProductEmbeddingService embeddingService;
    private final ProductDocumentMapper mapper;

    /**
     * Rebuilds the Elasticsearch index from SQL.
     *
     * @param purge delete every existing document first (removes stale ones)
     * @return number of products indexed
     */
    public int reindexAll(boolean purge) {

        if (purge) {
            searchRepository.deleteAll();
            log.info("Purged Elasticsearch product index");
        }

        int total = 0;
        int pageNumber = 0;
        Page<Product> page;

        do {
            page = productRepository.findAll(
                    PageRequest.of(pageNumber++, BATCH_SIZE, Sort.by("id")));

            if (page.isEmpty()) {
                break;
            }

            List<Product> products = page.getContent();
            List<float[]> embeddings = embeddingService.generateEmbeddings(products);

            List<ProductDocument> documents = new ArrayList<>(products.size());
            for (int i = 0; i < products.size(); i++) {
                documents.add(mapper.toDocument(products.get(i), embeddings.get(i)));
            }

            searchRepository.saveAll(documents);
            total += documents.size();
            log.info("Reindexed {} products so far", total);

        } while (page.hasNext());

        return total;
    }
}
