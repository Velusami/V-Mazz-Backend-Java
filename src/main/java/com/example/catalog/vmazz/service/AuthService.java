package com.example.catalog.vmazz.service;

import com.example.catalog.vmazz.domain.AppUser;
import com.example.catalog.vmazz.domain.Company;
import com.example.catalog.vmazz.domain.RecordStatus;
import com.example.catalog.vmazz.domain.Role;
import com.example.catalog.vmazz.dto.AuthResponse;
import com.example.catalog.vmazz.dto.LoginRequest;
import com.example.catalog.vmazz.dto.RegisterCompanyRequest;
import com.example.catalog.vmazz.dto.RegisterCustomerRequest;
import com.example.catalog.vmazz.dto.UserResponse;
import com.example.catalog.vmazz.exception.ConflictException;
import com.example.catalog.vmazz.repository.AppUserRepository;
import com.example.catalog.vmazz.repository.CompanyRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Locale;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private static final String INVALID_LOGIN = "Invalid email or password";

    private final AppUserRepository userRepository;
    private final CompanyRepository companyRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final CurrentUserService currentUserService;
    private final RegistrationEmailService registrationEmailService;

    /** A buyer signs up on the portal. */
    @Transactional
    public AuthResponse registerCustomer(RegisterCustomerRequest request) {

        String email = normaliseEmail(request.email());
        requireNewEmail(email);

        AppUser user = userRepository.save(AppUser.builder()
                .role(Role.CUSTOMER)
                .fullName(request.fullName().trim())
                .email(email)
                .mobile(blankToNull(request.mobile()))
                .passwordHash(passwordEncoder.encode(request.password()))
                .organizationName(blankToNull(request.organizationName()))
                .status(RecordStatus.ACTIVE)
                .build());

        registrationEmailService.sendWelcome(email, user.getFullName(), "customer");
        log.info("Customer registered: {}", email);
        return authResponse(user);
    }

    /** A supplier company signs up together with its first (ADMIN) user. */
    @Transactional
    public AuthResponse registerCompany(RegisterCompanyRequest request) {

        String adminEmail = normaliseEmail(request.adminEmail());
        requireNewEmail(adminEmail);

        String gst = blankToNull(request.gstNumber());
        if (gst != null) {
            gst = gst.toUpperCase(Locale.ROOT);
            if (companyRepository.existsByGstNumberIgnoreCase(gst)) {
                throw new ConflictException("A company with this GSTIN is already registered");
            }
        }

        Company company = companyRepository.save(Company.builder()
                .companyCode("TMP-" + UUID.randomUUID())          // replaced below once the id is known
                .companyName(request.companyName().trim())
                .gstNumber(gst)
                .email(normaliseEmail(request.companyEmail()))
                .phone(blankToNull(request.companyPhone()))
                .address(request.address().toEntity())
                .status(RecordStatus.ACTIVE)
                .build());
        company.setCompanyCode(String.format("CMP%05d", company.getId()));

        AppUser admin = userRepository.save(AppUser.builder()
                .company(company)
                .role(Role.ADMIN)
                .fullName(request.adminFullName().trim())
                .email(adminEmail)
                .mobile(blankToNull(request.adminMobile()))
                .passwordHash(passwordEncoder.encode(request.adminPassword()))
                .status(RecordStatus.ACTIVE)
                .build());

        registrationEmailService.sendWelcome(adminEmail, admin.getFullName(), "supplier administrator");
        log.info("Company {} registered with admin {}", company.getCompanyCode(), adminEmail);
        return authResponse(admin);
    }

    @Transactional
    public AuthResponse login(LoginRequest request) {

        AppUser user = userRepository.findByEmailIgnoreCase(normaliseEmail(request.email()))
                .orElseThrow(() -> new BadCredentialsException(INVALID_LOGIN));

        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new BadCredentialsException(INVALID_LOGIN);
        }
        if (user.getStatus() != RecordStatus.ACTIVE) {
            throw new AccessDeniedException("This account is inactive. Please contact your administrator.");
        }
        if (user.getCompany() != null && user.getCompany().getStatus() != RecordStatus.ACTIVE) {
            throw new AccessDeniedException("The company account is inactive.");
        }

        user.setLastLoginAt(LocalDateTime.now());
        return authResponse(user);
    }

    @Transactional(readOnly = true)
    public UserResponse me() {
        return UserResponse.from(currentUserService.requireUser());
    }

    // ------------------------------------------------------------------

    private AuthResponse authResponse(AppUser user) {
        return new AuthResponse(jwtService.issue(user), "Bearer", jwtService.ttlSeconds(), UserResponse.from(user));
    }

    private void requireNewEmail(String email) {
        if (userRepository.existsByEmailIgnoreCase(email)) {
            throw new ConflictException("An account with this email already exists");
        }
    }

    static String normaliseEmail(String email) {
        return email == null ? null : email.trim().toLowerCase(Locale.ROOT);
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
