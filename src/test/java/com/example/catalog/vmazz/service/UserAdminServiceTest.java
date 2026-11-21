package com.example.catalog.vmazz.service;

import com.example.catalog.vmazz.domain.AppUser;
import com.example.catalog.vmazz.domain.Company;
import com.example.catalog.vmazz.domain.RecordStatus;
import com.example.catalog.vmazz.domain.Role;
import com.example.catalog.vmazz.dto.CreateSubUserRequest;
import com.example.catalog.vmazz.dto.UserResponse;
import com.example.catalog.vmazz.exception.ConflictException;
import com.example.catalog.vmazz.repository.AppUserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserAdminServiceTest {

    @Mock AppUserRepository userRepository;
    @Mock CurrentUserService currentUser;

    private final PasswordEncoder encoder = new BCryptPasswordEncoder();

    UserAdminService service;
    Company company = TestData.company(1L);
    AppUser admin = TestData.admin(2L, company);

    @BeforeEach
    void setUp() {
        service = new UserAdminService(userRepository, currentUser, encoder);
        when(currentUser.requireAdmin()).thenReturn(admin);
    }

    @Test
    void createSubUser_belongsToTheAdminsCompany_withSubAdminRole() {
        when(userRepository.existsByEmailIgnoreCase("sub@x.com")).thenReturn(false);
        when(userRepository.save(any(AppUser.class))).thenAnswer(inv -> {
            AppUser u = inv.getArgument(0);
            u.setId(11L);
            return u;
        });

        UserResponse res = service.createSubUser(
                new CreateSubUserRequest("Sub One", "Sub@X.com", null, "Passw0rdX"));

        assertEquals(Role.SUB_ADMIN, res.getRole());
        assertEquals(1L, res.getCompanyId());
        assertEquals("sub@x.com", res.getEmail());
        assertEquals(RecordStatus.ACTIVE, res.getStatus());
    }

    @Test
    void createSubUser_duplicateEmail_isAConflict() {
        when(userRepository.existsByEmailIgnoreCase("sub@x.com")).thenReturn(true);

        assertThrows(ConflictException.class, () -> service.createSubUser(
                new CreateSubUserRequest("Sub One", "sub@x.com", null, "Passw0rdX")));
        verify(userRepository, never()).save(any());
    }

    @Test
    void updateStatus_deactivatesASubUser() {
        AppUser sub = AppUser.builder().id(3L).company(company).role(Role.SUB_ADMIN)
                .fullName("Sub").email("sub@x.com").passwordHash("x").status(RecordStatus.ACTIVE).build();
        when(userRepository.findByIdAndCompany_Id(3L, 1L)).thenReturn(Optional.of(sub));

        UserResponse res = service.updateStatus(3L, RecordStatus.INACTIVE);

        assertEquals(RecordStatus.INACTIVE, res.getStatus());
    }

    @Test
    void updateStatus_cannotChangeAnAdministrator() {
        AppUser otherAdmin = TestData.admin(4L, company);
        when(userRepository.findByIdAndCompany_Id(4L, 1L)).thenReturn(Optional.of(otherAdmin));

        assertThrows(ConflictException.class, () -> service.updateStatus(4L, RecordStatus.INACTIVE));
    }
}
