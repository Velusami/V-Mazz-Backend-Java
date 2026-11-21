package com.example.catalog.vmazz.controller;

import com.example.catalog.vmazz.dto.CompanyResponse;
import com.example.catalog.vmazz.dto.UpdateCompanyRequest;
import com.example.catalog.vmazz.service.CompanyService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/company")
@RequiredArgsConstructor
@Tag(name = "4. Company users (admin only)")
public class AdminCompanyController {

    private final CompanyService companyService;

    @GetMapping
    @Operation(summary = "My company profile")
    public CompanyResponse get() {
        return companyService.getMyCompany();
    }

    @PutMapping
    @Operation(summary = "Update my company profile")
    public CompanyResponse update(@Valid @RequestBody UpdateCompanyRequest request) {
        return companyService.updateMyCompany(request);
    }
}
