package com.example.catalog.service;

import com.example.catalog.document.ProductDocument;
import com.example.catalog.entity.Product;
import com.example.catalog.exception.ProductNotFoundException;
import com.example.catalog.repository.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.core.query.Query;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AlternateProductServiceTest {

    @Mock ProductRepository productRepository;
    @Mock ElasticsearchOperations operations;
    @Mock EmbeddingService embeddingService;

    AlternateProductService service;

    @BeforeEach
    void setUp() {
        service = new AlternateProductService(productRepository, operations, embeddingService, 0.70f);
    }

    @SuppressWarnings("unchecked")
    private void indexReturnsNothing() {
        SearchHits<ProductDocument> empty = mock(SearchHits.class);
        when(empty.getSearchHits()).thenReturn(List.of());
        when(operations.search(any(Query.class), eq(ProductDocument.class))).thenReturn(empty);
    }

    @Test
    void blankMpn_isRejected() {
        assertThrows(IllegalArgumentException.class,
                () -> service.findAlternates("  ", 5, true, false));
    }

    @Test
    void limitOutOfRange_isRejected() {
        assertThrows(IllegalArgumentException.class,
                () -> service.findAlternates("LM358", 0, true, false));
        assertThrows(IllegalArgumentException.class,
                () -> service.findAlternates("LM358", 11, true, false));
    }

    @Test
    void unknownMpn_throwsNotFound() {
        indexReturnsNothing();
        when(productRepository.findByMpn("NOPE")).thenReturn(Optional.empty());

        ProductNotFoundException ex = assertThrows(ProductNotFoundException.class,
                () -> service.findAlternates("NOPE", 5, true, false));

        assertTrue(ex.getMessage().contains("No product found"));
    }

    @Test
    void mpnInDatabaseButNotIndexed_tellsUserToReindex() {
        indexReturnsNothing();
        when(productRepository.findByMpn("LM358"))
                .thenReturn(Optional.of(Product.builder().id(1L).mpn("LM358").build()));

        ProductNotFoundException ex = assertThrows(ProductNotFoundException.class,
                () -> service.findAlternates("LM358", 5, true, false));

        assertTrue(ex.getMessage().contains("reindex"));
    }

    @Test
    void priceDifference_sameCurrency_isPercentOfSource() {
        assertEquals(25.0, AlternateProductService.priceDifferencePercent(
                new BigDecimal("100"), "USD", new BigDecimal("125"), "usd"));
        assertEquals(-10.0, AlternateProductService.priceDifferencePercent(
                new BigDecimal("50"), "INR", new BigDecimal("45"), "INR"));
    }

    @Test
    void priceDifference_differentCurrencyOrMissingPrice_isNull() {
        assertNull(AlternateProductService.priceDifferencePercent(
                new BigDecimal("100"), "USD", new BigDecimal("125"), "INR"));
        assertNull(AlternateProductService.priceDifferencePercent(
                null, "USD", new BigDecimal("125"), "USD"));
        assertNull(AlternateProductService.priceDifferencePercent(
                BigDecimal.ZERO, "USD", new BigDecimal("125"), "USD"));
    }
}
