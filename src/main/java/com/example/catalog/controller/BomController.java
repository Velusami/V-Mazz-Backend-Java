package com.example.catalog.controller;

import com.example.catalog.dto.BomLine;
import com.example.catalog.dto.BomMatchRequest;
import com.example.catalog.dto.BomMatchResponse;
import com.example.catalog.service.BomCsvParser;
import com.example.catalog.service.BomMatchingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/api/v1/ai/bom")
@RequiredArgsConstructor
@Tag(name = "BOM Matching", description = "Match bill-of-materials lines to catalog products")
public class BomController {

    private final BomMatchingService bomMatchingService;
    private final BomCsvParser csvParser;

    @PostMapping("/match")
    @Operation(summary = "Match BOM rows (JSON) to catalog products")
    public BomMatchResponse match(@Valid @RequestBody BomMatchRequest request) {
        boolean includeAlternates = request.includeAlternates() == null || request.includeAlternates();
        return bomMatchingService.match(request.rows(), includeAlternates);
    }

    @PostMapping(value = "/match/csv", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Upload a BOM CSV and match it to catalog products",
            description = "Columns (any order, flexible names): Reference, Description, MPN, Manufacturer, Quantity. "
                    + "Each row needs a Description or an MPN.")
    public BomMatchResponse matchCsv(
            @RequestParam("file") MultipartFile file,
            @RequestParam(defaultValue = "true") boolean includeAlternates) throws IOException {

        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Please upload a non-empty CSV file");
        }

        List<BomLine> rows = csvParser.parse(file.getInputStream());
        return bomMatchingService.match(rows, includeAlternates);
    }
}
