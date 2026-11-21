package com.example.catalog.vmazz.service;

import com.example.catalog.vmazz.domain.AppUser;
import com.example.catalog.vmazz.domain.Company;
import com.example.catalog.vmazz.dto.CompanyResponse;
import com.example.catalog.vmazz.dto.UpdateCompanyRequest;
import com.example.catalog.vmazz.exception.ConflictException;
import com.example.catalog.vmazz.exception.ResourceNotFoundException;
import com.example.catalog.vmazz.repository.CompanyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;

@Service
@RequiredArgsConstructor
public class CompanyService {

    private final CompanyRepository companyRepository;
    private final CurrentUserService currentUser;

    @Transactional(readOnly = true)
    public CompanyResponse getMyCompany() {
        AppUser admin = currentUser.requireAdmin();
        return CompanyResponse.from(load(admin));
    }

    @Transactional
    public CompanyResponse updateMyCompany(UpdateCompanyRequest request) {

        AppUser admin = currentUser.requireAdmin();
        Company company = load(admin);

        String gst = request.gstNumber() == null || request.gstNumber().isBlank()
                ? null
                : request.gstNumber().trim().toUpperCase(Locale.ROOT);

        boolean gstChanged = gst != null && !gst.equalsIgnoreCase(company.getGstNumber());
        if (gstChanged && companyRepository.existsByGstNumberIgnoreCase(gst)) {
            throw new ConflictException("A company with this GSTIN is already registered");
        }

        company.setCompanyName(request.companyName().trim());
        company.setGstNumber(gst);
        company.setEmail(request.email().trim().toLowerCase(Locale.ROOT));
        company.setPhone(request.phone() == null || request.phone().isBlank() ? null : request.phone().trim());
        company.setAddress(request.address().toEntity());

        return CompanyResponse.from(company);
    }

    private Company load(AppUser admin) {
        return companyRepository.findById(admin.getCompany().getId())
                .orElseThrow(() -> new ResourceNotFoundException("Company not found"));
    }
}
