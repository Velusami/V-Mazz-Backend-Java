package com.example.catalog.service;

import com.example.catalog.document.ProductDocument;
import com.example.catalog.dto.AlternateProductResponse;
import com.example.catalog.dto.AlternatesResult;
import com.example.catalog.dto.BomLine;
import com.example.catalog.dto.BomLineResult;
import com.example.catalog.dto.BomMatchStatus;
import com.example.catalog.entity.Product;
import com.example.catalog.repository.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BomMatchingServiceTest {

    @Mock ProductRepository productRepository;
    @Mock ElasticsearchOperations operations;
    @Mock EmbeddingService embeddingService;
    @Mock AlternateProductService alternateProductService;

    BomMatchingService service;

    @BeforeEach
    void setUp() {
        service = new BomMatchingService(
                productRepository, operations, embeddingService, alternateProductService, 85, 70);
    }

    private Product lm358(int stock, String status) {
        return Product.builder()
                .id(1L).mpn("LM358").itemName("Operational Amplifier").manufacturer("Texas Instruments")
                .price(new BigDecimal("25.50")).currency("INR").quantity(stock).status(status)
                .build();
    }

    // ---------------- exact MPN ----------------

    @Test
    void exactMpn_inStock_isExact_withLineCost_andNoAlternates() {
        when(productRepository.findByMpnIgnoreCase("lm358")).thenReturn(Optional.of(lm358(1000, "ACTIVE")));

        BomLineResult r = service.matchLine(1, new BomLine("U1", null, "lm358", null, 100), true);

        assertEquals(BomMatchStatus.EXACT, r.getMatchStatus());
        assertEquals(100, r.getConfidence());
        assertEquals("LM358", r.getMatch().getMpn());
        assertEquals(Boolean.TRUE, r.getStockSufficient());
        assertEquals(0, r.getShortfall());
        assertEquals(new BigDecimal("2550.00"), r.getLineCost());
        assertTrue(r.getAlternates().isEmpty());
        verifyNoInteractions(alternateProductService);
    }

    @Test
    void exactMpn_shortStock_reportsShortfall_andSuggestsAlternates() {
        when(productRepository.findByMpnIgnoreCase("LM358")).thenReturn(Optional.of(lm358(10, "ACTIVE")));

        AlternateProductResponse alt = new AlternateProductResponse(
                "LM358-ST", "Operational Amplifier", "STMicroelectronics", new BigDecimal("22.75"), "INR",
                1500, "ACTIVE", "Operational Amplifier", "SOIC-8", "Dual channel", 0.96, false, -10.8);
        AlternatesResult alternates = new AlternatesResult(
                "LM358", "Operational Amplifier", "Texas Instruments", new BigDecimal("25.50"), "INR",
                null, null, null, List.of(alt), "note");
        when(alternateProductService.findAlternates(
                eq("LM358"), eq(3), eq(true), eq(false), eq(true), eq(false))).thenReturn(alternates);

        BomLineResult r = service.matchLine(1, new BomLine(null, null, "LM358", null, 100), true);

        assertEquals(BomMatchStatus.EXACT, r.getMatchStatus());
        assertEquals(Boolean.FALSE, r.getStockSufficient());
        assertEquals(90, r.getShortfall());
        assertEquals(1, r.getAlternates().size());
        assertEquals("LM358-ST", r.getAlternates().get(0).mpn());
    }

    @Test
    void exactMpn_inactiveProduct_needsReview() {
        when(productRepository.findByMpnIgnoreCase("LM741")).thenReturn(
                Optional.of(Product.builder().id(7L).mpn("LM741").itemName("Operational Amplifier")
                        .manufacturer("Texas Instruments").price(new BigDecimal("20.00")).currency("INR")
                        .quantity(300).status("INACTIVE").build()));

        BomLineResult r = service.matchLine(1, new BomLine(null, null, "LM741", null, 10), false);

        assertEquals(BomMatchStatus.REVIEW, r.getMatchStatus());
        assertTrue(r.getReason().contains("INACTIVE"));
    }

    @Test
    void noQuantity_meansNoStockCheckAndNoCost() {
        when(productRepository.findByMpnIgnoreCase("LM358")).thenReturn(Optional.of(lm358(1000, "ACTIVE")));

        BomLineResult r = service.matchLine(1, new BomLine(null, null, "LM358", null, null), true);

        assertNull(r.getStockSufficient());
        assertNull(r.getLineCost());
    }

    // ---------------- validation ----------------

    @Test
    void lineWithoutMpnOrDescription_isRejected() {
        assertThrows(IllegalArgumentException.class,
                () -> service.matchLine(3, new BomLine("R1", " ", null, "Yageo", 10), true));
    }

    @Test
    void negativeQuantity_isRejected() {
        assertThrows(IllegalArgumentException.class,
                () -> service.matchLine(1, new BomLine(null, "Resistor", null, null, -5), true));
    }

    @Test
    void emptyOrOversizedBom_isRejected() {
        assertThrows(IllegalArgumentException.class, () -> service.match(List.of(), true));
        List<BomLine> tooMany = java.util.Collections.nCopies(
                BomMatchingService.MAX_ROWS + 1, new BomLine(null, "Resistor", null, null, 1));
        assertThrows(IllegalArgumentException.class, () -> service.match(tooMany, true));
    }

    // ---------------- scoring helpers ----------------

    @Test
    void tokens_joinNumberAndUnit_andDropStopwords() {
        Set<String> tokens = BomMatchingService.tokens("Resistor 10 kOhm, 0402 1% for the board");

        assertTrue(tokens.contains("resistor"));
        assertTrue(tokens.contains("10kohm"));
        assertTrue(tokens.contains("0402"));
        assertTrue(tokens.contains("1%"));
        assertFalse(tokens.contains("the"));
        assertFalse(tokens.contains("for"));
    }

    @Test
    void coverage_matchesPrefixes_so10kMatches10kohm() {
        Set<String> wanted = BomMatchingService.tokens("Resistor 10k 0402");
        Set<String> product = BomMatchingService.tokens("Resistor 10 kOhm 0402 1% tolerance");

        assertEquals(1.0, BomMatchingService.coverage(wanted, product), 0.0001);
        assertEquals(0.0, BomMatchingService.coverage(Set.of(), product), 0.0001);
    }

    @Test
    void confidence_blendsVectorScoreAndWordCoverage() {
        ProductDocument doc = ProductDocument.builder()
                .mpn("RC0402FR-0710KL").itemName("Resistor 10k ohm 0402 1%").manufacturer("Yageo")
                .category("Resistor").packageType("0402").specifications("10 kOhm, 1% tolerance")
                .build();
        Set<String> wanted = BomMatchingService.tokens("Resistor 10k ohm 0402 1%");

        assertEquals(100, BomMatchingService.confidence(1.0, wanted, null, doc));
        // 0.6 * 0.9 + 0.4 * 1.0 = 0.94
        assertEquals(94, BomMatchingService.confidence(0.9, wanted, null, doc));
        // manufacturer agreement adds 5 (capped at 100)
        assertEquals(99, BomMatchingService.confidence(0.9, wanted, "yageo", doc));
        // unrelated wording scores low even with a high vector score: 0.6 * 0.9
        assertTrue(BomMatchingService.confidence(
                0.9, BomMatchingService.tokens("Laser diode 650nm"), null, doc) < 70);
    }
}
