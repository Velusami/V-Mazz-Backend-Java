package com.example.catalog.service;

import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import com.example.catalog.document.ProductDocument;
import com.example.catalog.dto.AlternateProductResponse;
import com.example.catalog.dto.BomCandidate;
import com.example.catalog.dto.BomLine;
import com.example.catalog.dto.BomLineResult;
import com.example.catalog.dto.BomMatchResponse;
import com.example.catalog.dto.BomMatchStatus;
import com.example.catalog.dto.BomMatchedProduct;
import com.example.catalog.entity.Product;
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
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Matches bill-of-materials lines to catalog products.
 *
 * 1. If the line has an MPN that exists in the catalog -> EXACT (confidence 100).
 * 2. Otherwise the description (+ manufacturer + MPN text) is matched semantically:
 *    kNN over the product embeddings, blended with how many of the description's words
 *    (10k, 0402, dual, ...) appear in the product's name / specs / package.
 *    confidence = 60 % vector similarity + 40 % word coverage (+5 if the manufacturer agrees).
 * 3. Short stock or an inactive part triggers alternate suggestions.
 *
 * Thresholds (app.bom.matched-threshold / app.bom.review-threshold) need tuning against
 * your own data and embedding model.
 */
@Slf4j
@Service
public class BomMatchingService {

    static final int MAX_ROWS = 200;
    private static final int CANDIDATES = 3;
    private static final int NUM_CANDIDATES = 50;
    private static final int ALTERNATES_PER_LINE = 3;
    private static final double VECTOR_WEIGHT = 0.6;
    private static final double TEXT_WEIGHT = 0.4;
    private static final int MANUFACTURER_BONUS = 5;
    private static final int AMBIGUITY_GAP = 2;

    private static final Set<String> STOPWORDS =
            Set.of("the", "and", "for", "with", "of", "or", "in", "to", "pcs", "pc", "qty");

    static final String NOTE = "Matches are suggestions based on catalog descriptions. Confirm every "
            + "REVIEW line and verify specifications against datasheets before ordering.";

    private final ProductRepository productRepository;
    private final ElasticsearchOperations operations;
    private final EmbeddingService embeddingService;
    private final AlternateProductService alternateProductService;
    private final int matchedThreshold;
    private final int reviewThreshold;

    public BomMatchingService(
            ProductRepository productRepository,
            ElasticsearchOperations operations,
            EmbeddingService embeddingService,
            AlternateProductService alternateProductService,
            @Value("${app.bom.matched-threshold:85}") int matchedThreshold,
            @Value("${app.bom.review-threshold:70}") int reviewThreshold) {
        this.productRepository = productRepository;
        this.operations = operations;
        this.embeddingService = embeddingService;
        this.alternateProductService = alternateProductService;
        this.matchedThreshold = matchedThreshold;
        this.reviewThreshold = reviewThreshold;
    }

    // ------------------------------------------------------------------
    // Whole BOM
    // ------------------------------------------------------------------

    public BomMatchResponse match(List<BomLine> rows, boolean includeAlternates) {

        if (rows == null || rows.isEmpty()) {
            throw new IllegalArgumentException("The BOM has no rows");
        }
        if (rows.size() > MAX_ROWS) {
            throw new IllegalArgumentException("A BOM can have at most " + MAX_ROWS + " rows per request");
        }

        List<BomLineResult> lines = new ArrayList<>();
        int lineNumber = 1;
        for (BomLine row : rows) {
            lines.add(matchLine(lineNumber++, row, includeAlternates));
        }

        int exact = 0;
        int matched = 0;
        int review = 0;
        int notFound = 0;
        Map<String, BigDecimal> totals = new TreeMap<>();

        for (BomLineResult line : lines) {
            switch (line.getMatchStatus()) {
                case EXACT -> exact++;
                case MATCHED -> matched++;
                case REVIEW -> review++;
                case NOT_FOUND -> notFound++;
            }
            boolean confident = line.getMatchStatus() == BomMatchStatus.EXACT
                    || line.getMatchStatus() == BomMatchStatus.MATCHED;
            if (confident && line.getLineCost() != null && line.getMatch() != null
                    && line.getMatch().getCurrency() != null) {
                totals.merge(line.getMatch().getCurrency(), line.getLineCost(), BigDecimal::add);
            }
        }

        return BomMatchResponse.builder()
                .totalLines(lines.size())
                .exactMatches(exact)
                .matched(matched)
                .needsReview(review)
                .notFound(notFound)
                .estimatedTotalByCurrency(totals)
                .lines(lines)
                .note(NOTE)
                .build();
    }

    // ------------------------------------------------------------------
    // One line
    // ------------------------------------------------------------------

    public BomLineResult matchLine(int lineNumber, BomLine row, boolean includeAlternates) {

        if (row == null) {
            throw new IllegalArgumentException("Line " + lineNumber + " is empty");
        }

        String mpn = blankToNull(row.mpn());
        String description = blankToNull(row.description());
        String manufacturer = blankToNull(row.manufacturer());
        Integer quantity = row.quantity();

        if (mpn == null && description == null) {
            throw new IllegalArgumentException("Line " + lineNumber + ": provide an MPN or a description");
        }
        if (quantity != null && quantity < 0) {
            throw new IllegalArgumentException("Line " + lineNumber + ": quantity cannot be negative");
        }

        BomLineResult.BomLineResultBuilder base = BomLineResult.builder()
                .lineNumber(lineNumber)
                .reference(blankToNull(row.reference()))
                .requestedDescription(description)
                .requestedMpn(mpn)
                .requestedManufacturer(manufacturer)
                .requestedQuantity(quantity)
                .otherCandidates(List.of())
                .alternates(List.of());

        if (mpn != null) {
            Optional<Product> exact = productRepository.findByMpnIgnoreCase(mpn);
            if (exact.isPresent()) {
                return exactLine(base, exact.get(), quantity, includeAlternates);
            }
        }

        return semanticLine(base, mpn, description, manufacturer, quantity, includeAlternates);
    }

    // ------------------------------------------------------------------
    // Exact MPN
    // ------------------------------------------------------------------

    private BomLineResult exactLine(
            BomLineResult.BomLineResultBuilder base,
            Product product,
            Integer quantity,
            boolean includeAlternates) {

        boolean active = "ACTIVE".equalsIgnoreCase(product.getStatus());
        Boolean sufficient = stockSufficient(quantity, product.getQuantity());

        boolean needAlternates = includeAlternates && (!active || Boolean.FALSE.equals(sufficient));

        return base
                .matchStatus(active ? BomMatchStatus.EXACT : BomMatchStatus.REVIEW)
                .confidence(100)
                .reason(active
                        ? "Exact MPN match"
                        : "MPN found, but the product is " + product.getStatus() + " - consider an alternate")
                .match(fromProduct(product))
                .stockSufficient(sufficient)
                .shortfall(shortfall(quantity, product.getQuantity()))
                .lineCost(lineCost(quantity, product.getPrice()))
                .alternates(needAlternates ? alternatesFor(product.getMpn()) : List.of())
                .build();
    }

    // ------------------------------------------------------------------
    // Semantic match
    // ------------------------------------------------------------------

    private record Scored(ProductDocument doc, int confidence) { }

    private BomLineResult semanticLine(
            BomLineResult.BomLineResultBuilder base,
            String mpn,
            String description,
            String manufacturer,
            Integer quantity,
            boolean includeAlternates) {

        String prefix = mpn != null ? "MPN " + mpn + " is not in the catalog. " : "";

        String queryText = Stream.of(description, manufacturer, mpn)
                .filter(Objects::nonNull)
                .collect(Collectors.joining(" "));

        float[] embedding = embeddingService.generateQueryEmbedding(queryText);
        List<Float> vector = new ArrayList<>(embedding.length);
        for (float value : embedding) {
            vector.add(value);
        }

        Query active = Query.of(q -> q.term(t -> t.field("status").value("ACTIVE").caseInsensitive(true)));
        Query knn = Query.of(q -> q.knn(k -> k
                .field("embedding")
                .queryVector(vector)
                .k(CANDIDATES)
                .numCandidates(NUM_CANDIDATES)
                .filter(List.of(active))));

        List<SearchHit<ProductDocument>> hits = operations.search(
                        NativeQuery.builder().withQuery(knn).withMaxResults(CANDIDATES).build(),
                        ProductDocument.class)
                .getSearchHits();

        Set<String> wanted = tokens(Stream.of(description, mpn)
                .filter(Objects::nonNull)
                .collect(Collectors.joining(" ")));

        List<Scored> scored = hits.stream()
                .map(hit -> new Scored(
                        hit.getContent(),
                        confidence(hit.getScore(), wanted, manufacturer, hit.getContent())))
                .sorted(Comparator.comparingInt(Scored::confidence).reversed())
                .toList();

        if (scored.isEmpty()) {
            return base
                    .matchStatus(BomMatchStatus.NOT_FOUND)
                    .confidence(0)
                    .reason(prefix + "No similar product in the catalog")
                    .build();
        }

        Scored best = scored.get(0);

        BomMatchStatus status;
        String reason;

        if (best.confidence() >= matchedThreshold) {
            status = BomMatchStatus.MATCHED;
            reason = prefix + "Matched on description and specifications";

            // Two parts scoring almost the same: a person should choose.
            if (scored.size() > 1 && best.confidence() - scored.get(1).confidence() < AMBIGUITY_GAP) {
                status = BomMatchStatus.REVIEW;
                reason = prefix + "Several catalog parts match almost equally - please choose one";
            }
        } else if (best.confidence() >= reviewThreshold) {
            status = BomMatchStatus.REVIEW;
            reason = prefix + "Possible match - please confirm";
        } else {
            status = BomMatchStatus.NOT_FOUND;
            reason = prefix + "No confident match. The closest catalog parts are listed";
        }

        // Nothing good enough: show the closest parts but do not claim a match.
        if (status == BomMatchStatus.NOT_FOUND) {
            return base
                    .matchStatus(status)
                    .confidence(best.confidence())
                    .reason(reason)
                    .otherCandidates(scored.stream().map(this::toCandidate).toList())
                    .build();
        }

        ProductDocument doc = best.doc();
        Boolean sufficient = stockSufficient(quantity, doc.getQuantity());

        return base
                .matchStatus(status)
                .confidence(best.confidence())
                .reason(reason)
                .match(fromDocument(doc))
                .stockSufficient(sufficient)
                .shortfall(shortfall(quantity, doc.getQuantity()))
                .lineCost(lineCost(quantity, doc.getPrice()))
                .otherCandidates(scored.stream().skip(1).map(this::toCandidate).toList())
                .alternates(includeAlternates && Boolean.FALSE.equals(sufficient)
                        ? alternatesFor(doc.getMpn())
                        : List.of())
                .build();
    }

    // ------------------------------------------------------------------
    // Scoring helpers (package-private for tests)
    // ------------------------------------------------------------------

    static int confidence(double vectorScore, Set<String> wanted, String manufacturer, ProductDocument doc) {

        double vector = Math.max(0.0, Math.min(1.0, vectorScore));
        double text = coverage(wanted, tokens(productText(doc)));

        int percent = (int) Math.round((VECTOR_WEIGHT * vector + TEXT_WEIGHT * text) * 100);

        if (manufacturer != null && manufacturer.equalsIgnoreCase(doc.getManufacturer())) {
            percent += MANUFACTURER_BONUS;
        }
        return Math.min(100, percent);
    }

    private static String productText(ProductDocument doc) {
        return Stream.of(doc.getItemName(), doc.getCategory(), doc.getSpecifications(),
                        doc.getPackageType(), doc.getManufacturer(), doc.getMpn())
                .filter(Objects::nonNull)
                .collect(Collectors.joining(" "));
    }

    /** Lower-case words; "10 kOhm" becomes "10kohm" so it matches "10k ohm". */
    static Set<String> tokens(String text) {

        Set<String> result = new LinkedHashSet<>();
        if (text == null) {
            return result;
        }

        String normalised = text.toLowerCase(Locale.ROOT)
                .replaceAll("(\\d)\\s+([a-z%]+)", "$1$2");

        for (String raw : normalised.split("[^a-z0-9%.]+")) {
            String token = raw.replaceAll("^\\.+|\\.+$", "");
            if (token.length() >= 2 && !STOPWORDS.contains(token)) {
                result.add(token);
            }
        }
        return result;
    }

    /** Share of the wanted words that appear (exactly or as a prefix) among the available words. */
    static double coverage(Set<String> wanted, Set<String> available) {

        if (wanted.isEmpty()) {
            return 0.0;
        }
        long hits = wanted.stream()
                .filter(w -> available.stream().anyMatch(a -> tokenMatches(w, a)))
                .count();
        return (double) hits / wanted.size();
    }

    static boolean tokenMatches(String a, String b) {
        if (a.equals(b)) {
            return true;
        }
        return (a.length() >= 3 && b.startsWith(a)) || (b.length() >= 3 && a.startsWith(b));
    }

    // ------------------------------------------------------------------
    // Stock / cost / mapping
    // ------------------------------------------------------------------

    private Boolean stockSufficient(Integer requested, Integer available) {
        return requested == null || available == null ? null : available >= requested;
    }

    private Integer shortfall(Integer requested, Integer available) {
        return requested == null || available == null ? null : Math.max(0, requested - available);
    }

    private BigDecimal lineCost(Integer quantity, BigDecimal price) {
        return quantity == null || price == null
                ? null
                : price.multiply(BigDecimal.valueOf(quantity)).setScale(2, RoundingMode.HALF_UP);
    }

    private List<AlternateProductResponse> alternatesFor(String mpn) {
        try {
            return alternateProductService
                    .findAlternates(mpn, ALTERNATES_PER_LINE, true, false, true, false)
                    .alternates();
        } catch (Exception ex) {
            log.warn("Could not load alternates for {}: {}", mpn, ex.getMessage());
            return List.of();
        }
    }

    private BomCandidate toCandidate(Scored scored) {
        return new BomCandidate(fromDocument(scored.doc()), scored.confidence());
    }

    private BomMatchedProduct fromProduct(Product p) {
        return BomMatchedProduct.builder()
                .mpn(p.getMpn())
                .itemName(p.getItemName())
                .manufacturer(p.getManufacturer())
                .category(p.getCategory())
                .packageType(p.getPackageType())
                .specifications(p.getSpecifications())
                .price(p.getPrice())
                .currency(p.getCurrency())
                .quantityAvailable(p.getQuantity())
                .status(p.getStatus())
                .build();
    }

    private BomMatchedProduct fromDocument(ProductDocument d) {
        return BomMatchedProduct.builder()
                .mpn(d.getMpn())
                .itemName(d.getItemName())
                .manufacturer(d.getManufacturer())
                .category(d.getCategory())
                .packageType(d.getPackageType())
                .specifications(d.getSpecifications())
                .price(d.getPrice())
                .currency(d.getCurrency())
                .quantityAvailable(d.getQuantity())
                .status(d.getStatus())
                .build();
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
