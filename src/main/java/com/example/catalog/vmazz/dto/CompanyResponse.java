package com.example.catalog.vmazz.dto;

import com.example.catalog.vmazz.domain.Company;
import com.example.catalog.vmazz.domain.RecordStatus;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class CompanyResponse {

    private final Long id;
    private final String companyCode;
    private final String companyName;
    private final String gstNumber;
    private final String email;
    private final String phone;
    private final AddressDto address;
    private final RecordStatus status;
    private final LocalDateTime createdAt;

    public static CompanyResponse from(Company c) {
        return CompanyResponse.builder()
                .id(c.getId())
                .companyCode(c.getCompanyCode())
                .companyName(c.getCompanyName())
                .gstNumber(c.getGstNumber())
                .email(c.getEmail())
                .phone(c.getPhone())
                .address(AddressDto.from(c.getAddress()))
                .status(c.getStatus())
                .createdAt(c.getCreatedAt())
                .build();
    }
}
