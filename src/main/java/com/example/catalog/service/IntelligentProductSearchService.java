package com.example.catalog.service;

import com.example.catalog.document.ProductDocument;
import com.example.catalog.dto.ProductSearchCriteria;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Locale;

/**
 * Natural language -> structured filters (LLM) -> hybrid search with those
 * filters pushed into Elasticsearch.
 */
@Slf4j
@Service
public class IntelligentProductSearchService {

    // No curly braces in this prompt: Spring AI treats them as template variables.
    private static final String PARSER_PROMPT = """
            You convert a buyer's product request into search filters.

            Rules:
            - Use null for any field the buyer did not mention. Never invent filters.
            - itemName is only the product type or keywords (for example: operational amplifier).
              Do not put the manufacturer, price or stock words in itemName.
            - "above 100" means minPrice 100.
            - "below 100" or "under 100" means maxPrice 100.
            - "between 100 and 600" means minPrice 100 and maxPrice 600.
            - "in stock" means minimumQuantity 1 unless another quantity is given.
            - Prices and quantities are plain numbers.
            - currency is an ISO code such as USD or INR.
            - status only if the buyer states one, for example ACTIVE.
            """;

    private final ChatClient chatClient;
    private final HybridSearchService hybridSearchService;

    public IntelligentProductSearchService(
            ChatClient.Builder chatClientBuilder,
            HybridSearchService hybridSearchService) {
        this.chatClient = chatClientBuilder.build();
        this.hybridSearchService = hybridSearchService;
    }

    public List<ProductDocument> search(String userQuery) {

        if (userQuery == null || userQuery.isBlank()) {
            throw new IllegalArgumentException("Search query must not be empty");
        }

        String query = userQuery.trim();

        ProductSearchCriteria criteria = parse(query);
        validate(criteria);

        log.debug("Search criteria for '{}': {}", query, criteria);

        return hybridSearchService.search(criteria.getItemName(), criteria);
    }

    private ProductSearchCriteria parse(String query) {

        ProductSearchCriteria criteria = null;

        try {
            // Spring AI adds the JSON format instructions and parses the reply.
            criteria = chatClient.prompt()
                    .system(PARSER_PROMPT)
                    .user(u -> u.text("{request}").param("request", query))
                    .call()
                    .entity(ProductSearchCriteria.class);
        } catch (Exception ex) {
            log.warn("Could not parse search criteria for '{}': {}", query, ex.getMessage());
        }

        criteria = normalize(criteria);

        // Parser failed or found nothing useful: plain hybrid search on the raw text.
        if (isEmpty(criteria)) {
            criteria = new ProductSearchCriteria();
            criteria.setItemName(query);
        }

        return criteria;
    }

    private ProductSearchCriteria normalize(ProductSearchCriteria c) {

        if (c == null) {
            return null;
        }

        c.setItemName(clean(c.getItemName()));
        c.setManufacturer(clean(c.getManufacturer()));
        c.setStatus(clean(c.getStatus()));

        String currency = clean(c.getCurrency());
        c.setCurrency(currency == null ? null : currency.toUpperCase(Locale.ROOT));

        return c;
    }

    // Small models sometimes return the string "null" instead of null.
    private String clean(String value) {
        if (value == null || value.isBlank() || value.equalsIgnoreCase("null")) {
            return null;
        }
        return value.trim();
    }

    private boolean isEmpty(ProductSearchCriteria c) {
        return c == null
                || (c.getItemName() == null
                && c.getManufacturer() == null
                && c.getMinPrice() == null
                && c.getMaxPrice() == null
                && c.getCurrency() == null
                && c.getMinimumQuantity() == null
                && c.getStatus() == null);
    }

    private void validate(ProductSearchCriteria c) {

        if (c.getMinPrice() != null && c.getMinPrice().signum() < 0) {
            throw new IllegalArgumentException("Minimum price cannot be negative");
        }
        if (c.getMaxPrice() != null && c.getMaxPrice().signum() < 0) {
            throw new IllegalArgumentException("Maximum price cannot be negative");
        }
        if (c.getMinPrice() != null && c.getMaxPrice() != null
                && c.getMinPrice().compareTo(c.getMaxPrice()) > 0) {
            throw new IllegalArgumentException("Minimum price cannot exceed maximum price");
        }
        if (c.getMinimumQuantity() != null && c.getMinimumQuantity() < 0) {
            throw new IllegalArgumentException("Minimum quantity cannot be negative");
        }
    }
}
