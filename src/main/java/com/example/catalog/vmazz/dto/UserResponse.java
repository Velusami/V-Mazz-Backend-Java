package com.example.catalog.vmazz.dto;

import com.example.catalog.vmazz.domain.AppUser;
import com.example.catalog.vmazz.domain.RecordStatus;
import com.example.catalog.vmazz.domain.Role;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class UserResponse {

    private final Long id;
    private final String fullName;
    private final String email;
    private final String mobile;
    private final Role role;
    private final RecordStatus status;
    private final String organizationName;
    private final String profileImageUrl;
    private final Long companyId;
    private final String companyName;
    private final LocalDateTime createdAt;
    private final LocalDateTime lastLoginAt;

    public static UserResponse from(AppUser u) {
        return UserResponse.builder()
                .id(u.getId())
                .fullName(u.getFullName())
                .email(u.getEmail())
                .mobile(u.getMobile())
                .role(u.getRole())
                .status(u.getStatus())
                .organizationName(u.getOrganizationName())
                .profileImageUrl(u.getProfileImageUrl())
                .companyId(u.getCompany() == null ? null : u.getCompany().getId())
                .companyName(u.getCompany() == null ? null : u.getCompany().getCompanyName())
                .createdAt(u.getCreatedAt())
                .lastLoginAt(u.getLastLoginAt())
                .build();
    }
}
