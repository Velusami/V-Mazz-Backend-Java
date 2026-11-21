package com.example.catalog.service;

import com.example.catalog.document.ProductDocument;
import com.example.catalog.dto.AlternateProductResponse;
import com.example.catalog.dto.AlternatesResult;
import com.example.catalog.dto.BomCandidate;
import com.example.catalog.dto.BomLine;
import com.example.catalog.dto.BomLineResult;
import com.example.catalog.dto.BomMatchedProduct;
import com.example.catalog.dto.ProductSearchCriteria;
import com.example.catalog.exception.ProductNotFoundException;
import com.example.catalog.repository.ProductSearchRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.stream.Collectors;

/** The ONE set of read-only tools the product agent can call. */
@Component
@RequiredArgsConstructor
public class ProductAgentTools {

    private final IntelligentProductSearchService searchService;
    private final HybridSearchService hybridSearchService;
    private final ProductSearchRepository searchRepository;
    private final AlternateProductService alternateProductService;
    private final BomMatchingService bomMatchingService;

    @Tool(description = """
            Search the catalog using the buyer's natural-language request.
            Use for finding products by name, type, manufacturer, price range,
            currency or availability.
            """)
    public String searchProducts(
            @ToolParam(description = "The buyer's product search request") String request) {

        if (request == null || request.isBlank()) {
            return "Please provide a search request.";
        }
        return formatAll(searchService.search(request), "No matching products found.");
    }

    @Tool(description = """
            Get price, stock and status for ONE product by its exact MPN.
            Use when the user asks about a specific part, its price, or whether
            it is in stock. Never guess an MPN.
            """)
    public String getProductByMpn(
            @ToolParam(description = "Exact manufacturer part number") String mpn) {

        if (mpn == null || mpn.isBlank()) {
            return "A product MPN is required.";
        }
        return findByMpn(mpn)
                .map(this::format)
                .orElse("No product found for MPN: " + mpn.trim());
    }

    @Tool(description = """
            Find ACTIVE, in-stock products from one manufacturer whose unit price
            does not exceed a budget. Use when a buyer gives a manufacturer and a budget.
            """)
    public String findProductsByManufacturerAndBudget(
            @ToolParam(description = "Manufacturer name, for example Texas Instruments")
            String manufacturer,
            @ToolParam(description = "Maximum unit price in the catalog currency")
            double maxPrice) {

        if (manufacturer == null || manufacturer.isBlank()) {
            return "Please provide a manufacturer.";
        }
        if (!Double.isFinite(maxPrice) || maxPrice < 0) {
            return "Please provide a valid non-negative maximum price.";
        }

        ProductSearchCriteria criteria = new ProductSearchCriteria();
        criteria.setManufacturer(manufacturer.trim());
        criteria.setMaxPrice(BigDecimal.valueOf(maxPrice));
        criteria.setMinimumQuantity(1);
        criteria.setStatus("ACTIVE");

        return formatAll(
                hybridSearchService.search("", criteria),
                "No active, in-stock products found for that manufacturer and budget.");
    }

    @Tool(description = """
            Compare two products by their exact MPNs using real catalog data.
            Only compare prices when both use the same currency.
            """)
    public String compareProducts(
            @ToolParam(description = "Exact MPN of the first product") String firstMpn,
            @ToolParam(description = "Exact MPN of the second product") String secondMpn) {

        if (firstMpn == null || firstMpn.isBlank()
                || secondMpn == null || secondMpn.isBlank()) {
            return "Two MPNs are required to compare products.";
        }

        Optional<ProductDocument> first = findByMpn(firstMpn);
        Optional<ProductDocument> second = findByMpn(secondMpn);

        if (first.isEmpty() || second.isEmpty()) {
            return "Could not compare. "
                    + (first.isEmpty() ? "Not found: " + firstMpn.trim() + ". " : "")
                    + (second.isEmpty() ? "Not found: " + secondMpn.trim() + "." : "");
        }

        return "PRODUCT 1\n" + format(first.get())
                + "\n\nPRODUCT 2\n" + format(second.get());
    }

    @Tool(description = """
            Suggest alternate or substitute parts for ONE product by its exact MPN.
            Returns active, in-stock catalog parts that are most similar, with price
            difference. Use when the user asks for an alternate, substitute, replacement
            or second source. Optionally restrict to other manufacturers.
            """)
    public String suggestAlternates(
            @ToolParam(description = "Exact MPN of the part that needs an alternate") String mpn,
            @ToolParam(description = "true to only suggest parts from OTHER manufacturers") boolean differentManufacturer,
            @ToolParam(description = "true if the alternate must have the same package (drop-in replacement)") boolean samePackage) {

        if (mpn == null || mpn.isBlank()) {
            return "A product MPN is required.";
        }

        AlternatesResult result;
        try {
            result = alternateProductService.findAlternates(mpn, 5, true, differentManufacturer, true, samePackage);
        } catch (ProductNotFoundException ex) {
            return "No product found for MPN: " + mpn.trim();
        }

        if (result.alternates().isEmpty()) {
            return "No in-stock alternates found for " + result.sourceMpn() + ".";
        }

        String list = result.alternates().stream()
                .map(this::formatAlternate)
                .collect(Collectors.joining("\n\n"));

        return "ALTERNATES FOR " + result.sourceMpn() + " (" + result.sourceItemName() + ", "
                + result.sourceManufacturer() + ", " + result.sourcePrice() + " " + result.sourceCurrency() + ")\n\n"
                + list + "\n\n" + result.note();
    }

    @Tool(description = """
            Match ONE bill-of-materials line to a catalog product. Give the part description
            and/or the MPN, and the required quantity. Returns the best match, a confidence
            score, whether stock covers the quantity, the line cost and alternates if stock
            is short. Use when the user describes a BOM item they need to buy.
            """)
    public String matchBomLine(
            @ToolParam(description = "Part description, for example: Resistor 10k ohm 0402 1%. Empty if unknown") String description,
            @ToolParam(description = "Manufacturer part number if known, otherwise empty") String mpn,
            @ToolParam(description = "Required quantity, or 0 if not given") int quantity) {

        try {
            BomLineResult result = bomMatchingService.matchLine(
                    1,
                    new BomLine(null, description, mpn, null, quantity > 0 ? quantity : null),
                    true);
            return formatBomResult(result);
        } catch (IllegalArgumentException ex) {
            return ex.getMessage();
        }
    }

    // ------------------------------------------------------------------

    private Optional<ProductDocument> findByMpn(String mpn) {
        String trimmed = mpn.trim();
        Optional<ProductDocument> exact = searchRepository.findByMpn(trimmed);
        return exact.isPresent()
                ? exact
                : searchRepository.findByMpn(trimmed.toUpperCase(Locale.ROOT));
    }

    private String formatAll(List<ProductDocument> products, String emptyMessage) {
        return products.isEmpty()
                ? emptyMessage
                : products.stream().map(this::format).collect(Collectors.joining("\n\n"));
    }

    private String formatBomResult(BomLineResult r) {

        StringBuilder sb = new StringBuilder();
        sb.append("Status: ").append(r.getMatchStatus())
                .append(" (confidence ").append(r.getConfidence()).append("/100)")
                .append("\nReason: ").append(r.getReason());

        BomMatchedProduct m = r.getMatch();
        if (m != null) {
            sb.append("\nMatched part: ").append(m.getMpn()).append(" - ").append(m.getItemName())
                    .append(", ").append(m.getManufacturer())
                    .append("\nCategory: ").append(m.getCategory()).append(", package: ").append(m.getPackageType())
                    .append("\nSpecifications: ").append(m.getSpecifications())
                    .append("\nUnit price: ").append(m.getPrice()).append(" ").append(m.getCurrency())
                    .append("\nAvailable: ").append(m.getQuantityAvailable());
        }
        if (r.getStockSufficient() != null) {
            sb.append(r.getStockSufficient()
                    ? "\nStock: sufficient for the requested quantity"
                    : "\nStock: SHORT by " + r.getShortfall() + " units");
        }
        if (r.getLineCost() != null && m != null) {
            sb.append("\nLine cost: ").append(r.getLineCost()).append(" ").append(m.getCurrency());
        }
        if (!r.getOtherCandidates().isEmpty()) {
            sb.append("\n\nOther candidates:");
            for (BomCandidate c : r.getOtherCandidates()) {
                sb.append("\n- ").append(c.product().getMpn()).append(" (").append(c.product().getItemName())
                        .append(", confidence ").append(c.confidence()).append(")");
            }
        }
        if (!r.getAlternates().isEmpty()) {
            sb.append("\n\nAlternates:\n").append(
                    r.getAlternates().stream().map(this::formatAlternate).collect(Collectors.joining("\n\n")));
        }
        return sb.toString();
    }

    private String formatAlternate(AlternateProductResponse a) {
        String diff = a.priceDifferencePercent() == null
                ? "n/a"
                : (a.priceDifferencePercent() > 0 ? "+" : "") + a.priceDifferencePercent() + "%";
        return "MPN: " + a.mpn()
                + "\nProduct: " + a.itemName()
                + "\nManufacturer: " + a.manufacturer() + (a.sameManufacturer() ? " (same as source)" : "")
                + "\nPrice: " + a.price() + " " + a.currency() + " (vs source: " + diff + ")"
                + "\nCategory: " + a.category() + ", package: " + a.packageType()
                + "\nSpecifications: " + a.specifications()
                + "\nQuantity: " + a.quantity()
                + "\nSimilarity: " + a.similarity();
    }

    private String format(ProductDocument p) {
        Integer qty = p.getQuantity();
        String stock = qty == null ? "unknown" : (qty > 0 ? "in stock" : "out of stock");
        return "MPN: " + p.getMpn()
                + "\nProduct: " + p.getItemName()
                + "\nManufacturer: " + p.getManufacturer()
                + "\nPrice: " + p.getPrice() + " " + p.getCurrency()
                + "\nQuantity: " + (qty == null ? "unknown" : qty) + " (" + stock + ")"
                + "\nStatus: " + p.getStatus();
    }
}
