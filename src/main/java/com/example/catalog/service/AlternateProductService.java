package com.example.catalog.service;

import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import com.example.catalog.document.ProductDocument;
import com.example.catalog.dto.AlternateProductResponse;
import com.example.catalog.dto.AlternatesResult;
import com.example.catalog.exception.ProductNotFoundException;
import com.example.catalog.repository.ProductRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.elasticsearch.client.elc.NativeQuery;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Suggests alternate parts for an MPN: the nearest ACTIVE products to the source
 * part in embedding space (document-to-document kNN), optionally in stock only.
 *
 * The embedding is built from item name + manufacturer + MPN, so a suggestion
 * means "similar catalog entry", not "verified functional equivalent".
 */
@Slf4j
@Service
public class AlternateProductService {

    static final int DEFAULT_LIMIT = 5;
    static final int MAX_LIMIT = 10;
    private static final int NUM_CANDIDATES = 100;

    static final String NOTE = "Suggestions are ranked by similarity of the catalog description "
            + "(name, category, specifications, package, manufacturer). Verify electrical and "
            + "mechanical specifications against the datasheet before treating any part as an equivalent.";

    private final ProductRepository productRepository;
    private final ElasticsearchOperations operations;
    private final EmbeddingService embeddingService;
    private final float minSimilarity;

    public AlternateProductService(
            ProductRepository productRepository,
            ElasticsearchOperations operations,
            EmbeddingService embeddingService,
            @Value("${app.alternates.min-similarity:0.70}") float minSimilarity) {
        this.productRepository = productRepository;
        this.operations = operations;
        this.embeddingService = embeddingService;
        this.minSimilarity = minSimilarity;
    }

    public AlternatesResult findAlternates(
            String mpn,
            Integer limit,
            boolean inStockOnly,
            boolean differentManufacturer) {
        return findAlternates(mpn, limit, inStockOnly, differentManufacturer, true, false);
    }

    /**
     * @param sameCategory only suggest parts in the same category as the source (if it has one)
     * @param samePackage  only suggest parts with the same package (drop-in replacements)
     */
    public AlternatesResult findAlternates(
            String mpn,
            Integer limit,
            boolean inStockOnly,
            boolean differentManufacturer,
            boolean sameCategory,
            boolean samePackage) {

        if (mpn == null || mpn.isBlank()) {
            throw new IllegalArgumentException("MPN must not be empty");
        }

        int size = limit == null ? DEFAULT_LIMIT : limit;
        if (size < 1 || size > MAX_LIMIT) {
            throw new IllegalArgumentException("limit must be between 1 and " + MAX_LIMIT);
        }

        ProductDocument source = findSource(mpn.trim());

        // 1. Query vector: the source part's own stored embedding
        //    (document vs document, same prefix). Fallback: embed its text.
        List<Float> vector = toVector(source);

        // 2. Filters
        List<Query> filters = new ArrayList<>();
        filters.add(term("status", "ACTIVE"));
        if (sameCategory && hasText(source.getCategory())) {
            filters.add(term("category", source.getCategory()));
        }
        if (samePackage && hasText(source.getPackageType())) {
            filters.add(term("packageType", source.getPackageType()));
        }
        if (inStockOnly) {
            filters.add(Query.of(q -> q.range(r -> r.number(n -> n.field("quantity").gte(1.0)))));
        }

        List<Query> exclusions = new ArrayList<>();
        exclusions.add(term("mpn", source.getMpn()));                       // never suggest itself
        if (differentManufacturer && hasText(source.getManufacturer())) {
            exclusions.add(term("manufacturer", source.getManufacturer())); // second-source only
        }

        Query filter = Query.of(q -> q.bool(b -> b.filter(filters).mustNot(exclusions)));

        // 3. kNN search
        Query knn = Query.of(q -> q.knn(k -> k
                .field("embedding")
                .queryVector(vector)
                .k(size)
                .numCandidates(NUM_CANDIDATES)
                .similarity(minSimilarity)
                .filter(List.of(filter))));

        List<AlternateProductResponse> alternates = operations.search(
                        NativeQuery.builder().withQuery(knn).withMaxResults(size).build(),
                        ProductDocument.class)
                .getSearchHits().stream()
                .filter(hit -> hit.getContent().getMpn() != null
                        && !hit.getContent().getMpn().equalsIgnoreCase(source.getMpn()))
                .map(hit -> toResponse(source, hit))
                .toList();

        log.debug("{} alternates found for MPN {}", alternates.size(), source.getMpn());

        return new AlternatesResult(
                source.getMpn(),
                source.getItemName(),
                source.getManufacturer(),
                source.getPrice(),
                source.getCurrency(),
                source.getCategory(),
                source.getPackageType(),
                source.getSpecifications(),
                alternates,
                NOTE);
    }

    // ------------------------------------------------------------------

    private ProductDocument findSource(String mpn) {

        // 1. Search the index by MPN (case-insensitive).
        Query byMpn = Query.of(q -> q.term(t -> t.field("mpn").value(mpn).caseInsensitive(true)));

        Optional<ProductDocument> found = operations.search(
                        NativeQuery.builder().withQuery(byMpn).withMaxResults(1).build(),
                        ProductDocument.class)
                .getSearchHits().stream()
                .findFirst()
                .map(SearchHit::getContent);

        if (found.isPresent()) {
            return found.get();
        }

        // 2. Fall back to the database: find the product by MPN, then fetch its
        //    Elasticsearch document by id. A get-by-id does not depend on how the
        //    'mpn' field is mapped or on index refresh timing.
        var dbProduct = productRepository.findByMpn(mpn);

        if (dbProduct.isEmpty()) {
            throw new ProductNotFoundException("No product found for MPN: " + mpn);
        }

        ProductDocument byId = operations.get(
                String.valueOf(dbProduct.get().getId()), ProductDocument.class);

        if (byId != null) {
            log.warn("MPN {} was not matched by the 'mpn' query but exists in the index by id. "
                    + "Check the index mapping of the 'mpn' field.", mpn);
            return byId;
        }

        throw new ProductNotFoundException("Product " + mpn + " exists in the database but is not "
                + "indexed in Elasticsearch. Run POST /api/v1/admin/products/reindex?purge=true");
    }

    private List<Float> toVector(ProductDocument source) {

        float[] embedding = source.getEmbedding();

        if (embedding == null || embedding.length == 0) {
            String text = ((source.getItemName() == null ? "" : source.getItemName()) + " "
                    + (source.getManufacturer() == null ? "" : source.getManufacturer())).trim();
            if (text.isEmpty()) {
                throw new IllegalStateException(
                        "Product " + source.getMpn() + " has no embedding and no text to embed");
            }
            embedding = embeddingService.generateQueryEmbedding(text);
        }

        List<Float> vector = new ArrayList<>(embedding.length);
        for (float value : embedding) {
            vector.add(value);
        }
        return vector;
    }

    private AlternateProductResponse toResponse(ProductDocument source, SearchHit<ProductDocument> hit) {

        ProductDocument p = hit.getContent();

        boolean sameManufacturer = hasText(source.getManufacturer())
                && source.getManufacturer().equalsIgnoreCase(p.getManufacturer());

        return new AlternateProductResponse(
                p.getMpn(),
                p.getItemName(),
                p.getManufacturer(),
                p.getPrice(),
                p.getCurrency(),
                p.getQuantity(),
                p.getStatus(),
                p.getCategory(),
                p.getPackageType(),
                p.getSpecifications(),
                Math.round(hit.getScore() * 1000.0) / 1000.0,
                sameManufacturer,
                priceDifferencePercent(source.getPrice(), source.getCurrency(), p.getPrice(), p.getCurrency()));
    }

    /** (alternate - source) / source * 100, or null if it cannot be compared. */
    static Double priceDifferencePercent(
            BigDecimal sourcePrice, String sourceCurrency,
            BigDecimal altPrice, String altCurrency) {

        if (sourcePrice == null || altPrice == null
                || sourceCurrency == null || altCurrency == null
                || !sourceCurrency.equalsIgnoreCase(altCurrency)
                || sourcePrice.signum() <= 0) {
            return null;
        }

        return altPrice.subtract(sourcePrice)
                .multiply(BigDecimal.valueOf(100))
                .divide(sourcePrice, 1, RoundingMode.HALF_UP)
                .doubleValue();
    }

    // Case-insensitive so a stored "Active"/"ACTIVE" or manufacturer casing never hides a part.
    private Query term(String field, String value) {
        return Query.of(q -> q.term(t -> t.field(field).value(value).caseInsensitive(true)));
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
