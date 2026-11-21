package com.example.catalog.vmazz.service;

import com.example.catalog.vmazz.domain.AppUser;
import com.example.catalog.vmazz.domain.Company;
import com.example.catalog.vmazz.domain.RecordStatus;
import com.example.catalog.vmazz.domain.Role;
import com.example.catalog.vmazz.dto.AddressDto;
import com.example.catalog.vmazz.dto.AuthResponse;
import com.example.catalog.vmazz.dto.LoginRequest;
import com.example.catalog.vmazz.dto.RegisterCompanyRequest;
import com.example.catalog.vmazz.dto.RegisterCustomerRequest;
import com.example.catalog.vmazz.exception.ConflictException;
import com.example.catalog.vmazz.repository.AppUserRepository;
import com.example.catalog.vmazz.repository.CompanyRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock AppUserRepository userRepository;
    @Mock CompanyRepository companyRepository;
    @Mock JwtService jwtService;
    @Mock CurrentUserService currentUserService;

    private final PasswordEncoder encoder = new BCryptPasswordEncoder();

    AuthService service;

    @BeforeEach
    void setUp() {
        service = new AuthService(userRepository, companyRepository, encoder, jwtService, currentUserService);
    }

    private void tokenIsIssued() {
        when(jwtService.issue(any(AppUser.class))).thenReturn("tok");
        when(jwtService.ttlSeconds()).thenReturn(7200L);
    }

    @Test
    void registerCustomer_hashesPassword_normalisesEmail_andReturnsToken() {
        when(userRepository.existsByEmailIgnoreCase("buyer@x.com")).thenReturn(false);
        when(userRepository.save(any(AppUser.class))).thenAnswer(inv -> {
            AppUser u = inv.getArgument(0);
            u.setId(9L);
            return u;
        });
        tokenIsIssued();

        AuthResponse res = service.registerCustomer(
                new RegisterCustomerRequest(" Buyer One ", " Buyer@X.com ", null, "Passw0rdX", "Acme"));

        assertEquals("tok", res.accessToken());
        assertEquals("Bearer", res.tokenType());
        assertEquals(7200, res.expiresInSeconds());

        ArgumentCaptor<AppUser> captor = ArgumentCaptor.forClass(AppUser.class);
        verify(userRepository).save(captor.capture());
        AppUser saved = captor.getValue();
        assertEquals("buyer@x.com", saved.getEmail());
        assertEquals("Buyer One", saved.getFullName());
        assertEquals(Role.CUSTOMER, saved.getRole());
        assertNull(saved.getCompany());
        assertNotEquals("Passw0rdX", saved.getPasswordHash());
        assertTrue(encoder.matches("Passw0rdX", saved.getPasswordHash()));
    }

    @Test
    void registerCustomer_duplicateEmail_isAConflict() {
        when(userRepository.existsByEmailIgnoreCase("buyer@x.com")).thenReturn(true);

        assertThrows(ConflictException.class, () -> service.registerCustomer(
                new RegisterCustomerRequest("Buyer", "buyer@x.com", null, "Passw0rdX", null)));
    }

    @Test
    void registerCompany_createsCompanyCode_andAdminUser() {
        when(userRepository.existsByEmailIgnoreCase("ann@acme.com")).thenReturn(false);
        when(companyRepository.save(any(Company.class))).thenAnswer(inv -> {
            Company c = inv.getArgument(0);
            c.setId(7L);
            return c;
        });
        when(userRepository.save(any(AppUser.class))).thenAnswer(inv -> {
            AppUser u = inv.getArgument(0);
            u.setId(8L);
            return u;
        });
        tokenIsIssued();

        service.registerCompany(new RegisterCompanyRequest(
                "Acme Foods", null, "info@acme.com", null,
                new AddressDto("1 Main St", null, "Chennai", "Tamil Nadu", "600001", "India"),
                "Ann Admin", "ann@acme.com", null, "Passw0rdX"));

        ArgumentCaptor<Company> companyCaptor = ArgumentCaptor.forClass(Company.class);
        verify(companyRepository).save(companyCaptor.capture());
        assertEquals("CMP00007", companyCaptor.getValue().getCompanyCode());
        assertEquals("Acme Foods", companyCaptor.getValue().getCompanyName());

        ArgumentCaptor<AppUser> userCaptor = ArgumentCaptor.forClass(AppUser.class);
        verify(userRepository).save(userCaptor.capture());
        assertEquals(Role.ADMIN, userCaptor.getValue().getRole());
        assertEquals(7L, userCaptor.getValue().getCompany().getId());
    }

    private AppUser existingUser(RecordStatus status) {
        return AppUser.builder().id(1L).email("a@x.com").fullName("A").role(Role.ADMIN)
                .passwordHash(encoder.encode("Passw0rdX")).status(status).build();
    }

    @Test
    void login_withCorrectPassword_returnsToken_andRecordsLastLogin() {
        AppUser user = existingUser(RecordStatus.ACTIVE);
        when(userRepository.findByEmailIgnoreCase("a@x.com")).thenReturn(Optional.of(user));
        tokenIsIssued();

        AuthResponse res = service.login(new LoginRequest("A@X.com", "Passw0rdX"));

        assertEquals("tok", res.accessToken());
        assertNotNull(user.getLastLoginAt());
    }

    @Test
    void login_wrongPasswordOrUnknownEmail_giveTheSameError() {
        when(userRepository.findByEmailIgnoreCase("a@x.com"))
                .thenReturn(Optional.of(existingUser(RecordStatus.ACTIVE)));
        when(userRepository.findByEmailIgnoreCase("nobody@x.com")).thenReturn(Optional.empty());

        BadCredentialsException wrong = assertThrows(BadCredentialsException.class,
                () -> service.login(new LoginRequest("a@x.com", "WrongPass1")));
        BadCredentialsException unknown = assertThrows(BadCredentialsException.class,
                () -> service.login(new LoginRequest("nobody@x.com", "WrongPass1")));

        assertEquals(wrong.getMessage(), unknown.getMessage());
    }

    @Test
    void login_inactiveAccount_isForbidden() {
        when(userRepository.findByEmailIgnoreCase("a@x.com"))
                .thenReturn(Optional.of(existingUser(RecordStatus.INACTIVE)));

        assertThrows(AccessDeniedException.class,
                () -> service.login(new LoginRequest("a@x.com", "Passw0rdX")));
    }
}
