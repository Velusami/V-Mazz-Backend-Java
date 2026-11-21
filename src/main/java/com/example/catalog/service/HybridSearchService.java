package com.example.catalog.service;

import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import com.example.catalog.document.ProductDocument;
import com.example.catalog.dto.ProductSearchCriteria;
import org.springframework.data.elasticsearch.client.elc.NativeQuery;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Keyword + vector search merged with Reciprocal Rank Fusion.
 * Structured filters (manufacturer, price, stock...) are applied INSIDE
 * Elasticsearch - on both the keyword query and the kNN query - so they never
 * hide a match that ranked outside the top N.
 */
@Service
public class HybridSearchService {

    private static final int RESULT_SIZE = 10;
    private static final int NUM_CANDIDATES = 100;
    private static final int RRF_CONSTANT = 60;
    private static final float MIN_SIMILARITY = 0.60F;

    private final ElasticsearchOperations operations;
    private final EmbeddingService embeddingService;

    public HybridSearchService(
            ElasticsearchOperations operations,
            EmbeddingService embeddingService) {
        this.operations = operations;
        this.embeddingService = embeddingService;
    }

    public List<ProductDocument> search(String queryText) {
        return search(queryText, null);
    }

    /**
     * @param queryText product keywords; may be blank when filters are given
     * @param criteria  optional structured filters
     */
    public List<ProductDocument> search(String queryText, ProductSearchCriteria criteria) {

        final String text = queryText == null ? "" : queryText.trim();
        final List<Query> filters = buildFilters(criteria);

        if (text.isEmpty() && filters.isEmpty()) {
            throw new IllegalArgumentException(
                    "Provide search text or at least one filter");
        }

        // Filter-only request, e.g. "active TI products under 100".
        if (text.isEmpty()) {
            Query filterOnly = Query.of(q -> q.bool(b -> b.filter(filters)));
            return operations.search(
                            NativeQuery.builder()
                                    .withQuery(filterOnly)
                                    .withMaxResults(RESULT_SIZE)
                                    .build(),
                            ProductDocument.class)
                    .getSearchHits().stream()
                    .map(SearchHit::getContent)
                    .toList();
        }

        // 1. Keyword search (+ filters)
        Query keywordQuery = Query.of(q -> q.bool(b -> b
                .must(m -> m.multiMatch(mm -> mm
                        .query(text)
                        .fields("mpn^3", "itemName^2", "manufacturer")
                        .minimumShouldMatch("60%")))
                .filter(filters)));

        SearchHits<ProductDocument> keywordHits = operations.search(
                NativeQuery.builder()
                        .withQuery(keywordQuery)
                        .withMaxResults(RESULT_SIZE)
                        .build(),
                ProductDocument.class);

        // 2. Vector search (+ the same filters)
        float[] embedding = embeddingService.generateQueryEmbedding(text);
        List<Float> vector = new ArrayList<>(embedding.length);
        for (float value : embedding) {
            vector.add(value);
        }

        Query knnQuery = Query.of(q -> q.knn(k -> k
                .field("embedding")
                .queryVector(vector)
                .k(RESULT_SIZE)
                .numCandidates(NUM_CANDIDATES)
                .similarity(MIN_SIMILARITY)
                .filter(filters)));

        SearchHits<ProductDocument> vectorHits = operations.search(
                NativeQuery.builder()
                        .withQuery(knnQuery)
                        .withMaxResults(RESULT_SIZE)
                        .build(),
                ProductDocument.class);

        // 3. Reciprocal Rank Fusion
        Map<String, ProductDocument> documents = new LinkedHashMap<>();
        Map<String, Double> scores = new HashMap<>();

        accumulate(documents, scores, keywordHits);
        accumulate(documents, scores, vectorHits);

        return scores.entrySet().stream()
                .sorted(Map.Entry.<String, Double>comparingByValue().reversed())
                .limit(RESULT_SIZE)
                .map(entry -> documents.get(entry.getKey()))
                .toList();
    }

    private void accumulate(
            Map<String, ProductDocument> documents,
            Map<String, Double> scores,
            SearchHits<ProductDocument> hits) {

        int rank = 1;

        for (SearchHit<ProductDocument> hit : hits.getSearchHits()) {
            ProductDocument product = hit.getContent();
            String key = product.getId() != null
                    ? product.getId().toString()
                    : product.getMpn();

            if (key != null) {
                documents.putIfAbsent(key, product);
                scores.merge(key, 1.0 / (RRF_CONSTANT + rank), Double::sum);
            }
            rank++;
        }
    }

    // ------------------------------------------------------------------
    // Filters
    // ------------------------------------------------------------------

    private List<Query> buildFilters(ProductSearchCriteria c) {

        List<Query> filters = new ArrayList<>();

        if (c == null) {
            return filters;
        }

        if (hasText(c.getManufacturer())) {
            filters.add(term("manufacturer", c.getManufacturer().trim()));
        }
        if (hasText(c.getCurrency())) {
            filters.add(term("currency", c.getCurrency().trim()));
        }
        if (hasText(c.getStatus())) {
            filters.add(term("status", c.getStatus().trim()));
        }
        if (c.getMinPrice() != null || c.getMaxPrice() != null) {
            filters.add(range("price", toDouble(c.getMinPrice()), toDouble(c.getMaxPrice())));
        }
        if (c.getMinimumQuantity() != null) {
            filters.add(range("quantity", c.getMinimumQuantity().doubleValue(), null));
        }

        return filters;
    }

    // Case-insensitive, so "texas instruments" matches the keyword
    // "Texas Instruments" without changing the index mapping.
    private Query term(String field, String value) {
        return Query.of(q -> q.term(t -> t
                .field(field)
                .value(value)
                .caseInsensitive(true)));
    }

    private Query range(String field, Double gte, Double lte) {
        return Query.of(q -> q.range(r -> r.number(n -> {
            n.field(field);
            if (gte != null) {
                n.gte(gte);
            }
            if (lte != null) {
                n.lte(lte);
            }
            return n;
        })));
    }

    private Double toDouble(BigDecimal value) {
        return value == null ? null : value.doubleValue();
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
