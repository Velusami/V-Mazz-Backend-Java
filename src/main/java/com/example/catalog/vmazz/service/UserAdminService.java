package com.example.catalog.vmazz.service;

import com.example.catalog.vmazz.domain.AppUser;
import com.example.catalog.vmazz.domain.RecordStatus;
import com.example.catalog.vmazz.domain.Role;
import com.example.catalog.vmazz.dto.CreateSubUserRequest;
import com.example.catalog.vmazz.dto.PageResponse;
import com.example.catalog.vmazz.dto.UpdateUserRequest;
import com.example.catalog.vmazz.dto.UserResponse;
import com.example.catalog.vmazz.exception.ConflictException;
import com.example.catalog.vmazz.exception.ResourceNotFoundException;
import com.example.catalog.vmazz.repository.AppUserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** The company admin creates and manages the company's sub users. */
@Service
@RequiredArgsConstructor
public class UserAdminService {

    private final AppUserRepository userRepository;
    private final CurrentUserService currentUser;
    private final PasswordEncoder passwordEncoder;
    private final RegistrationEmailService registrationEmailService;

    @Transactional
    public UserResponse createSubUser(CreateSubUserRequest request) {

        AppUser admin = currentUser.requireAdmin();
        String email = AuthService.normaliseEmail(request.email());

        if (userRepository.existsByEmailIgnoreCase(email)) {
            throw new ConflictException("An account with this email already exists");
        }

        AppUser sub = userRepository.save(AppUser.builder()
                .company(admin.getCompany())
                .role(Role.SUB_ADMIN)
                .fullName(request.fullName().trim())
                .email(email)
                .mobile(request.mobile() == null || request.mobile().isBlank() ? null : request.mobile().trim())
                .passwordHash(passwordEncoder.encode(request.password()))
                .status(RecordStatus.ACTIVE)
                .build());

        registrationEmailService.sendWelcome(email, sub.getFullName(), "company sub-user");
        return UserResponse.from(sub);
    }

    @Transactional(readOnly = true)
    public PageResponse<UserResponse> list(Pageable pageable) {
        AppUser admin = currentUser.requireAdmin();
        return PageResponse.of(
                userRepository.findByCompany_Id(admin.getCompany().getId(), pageable),
                UserResponse::from);
    }

    @Transactional(readOnly = true)
    public UserResponse get(Long id) {
        AppUser admin = currentUser.requireAdmin();
        return UserResponse.from(findInCompany(id, admin));
    }

    @Transactional
    public UserResponse update(Long id, UpdateUserRequest request) {
        AppUser admin = currentUser.requireAdmin();
        AppUser user = findInCompany(id, admin);

        user.setFullName(request.fullName().trim());
        user.setMobile(request.mobile() == null || request.mobile().isBlank() ? null : request.mobile().trim());
        return UserResponse.from(user);
    }

    /** Activate or deactivate a sub user. Administrators cannot be changed here. */
    @Transactional
    public UserResponse updateStatus(Long id, RecordStatus status) {
        AppUser admin = currentUser.requireAdmin();
        AppUser user = findInCompany(id, admin);

        if (user.getRole() != Role.SUB_ADMIN) {
            throw new ConflictException("Only sub users can be activated or deactivated");
        }
        user.setStatus(status);
        return UserResponse.from(user);
    }

    private AppUser findInCompany(Long id, AppUser admin) {
        return userRepository.findByIdAndCompany_Id(id, admin.getCompany().getId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + id));
    }
}
