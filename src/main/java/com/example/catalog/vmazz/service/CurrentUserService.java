package com.example.catalog.vmazz.service;

import com.example.catalog.vmazz.domain.AppUser;
import com.example.catalog.vmazz.domain.RecordStatus;
import com.example.catalog.vmazz.domain.Role;
import com.example.catalog.vmazz.repository.AppUserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

/** Loads the logged-in user (from the JWT subject) and enforces role rules inside the services. */
@Service
@RequiredArgsConstructor
public class CurrentUserService {

    private final AppUserRepository userRepository;

    public AppUser requireUser() {

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        if (auth == null || !auth.isAuthenticated() || auth instanceof AnonymousAuthenticationToken) {
            throw new BadCredentialsException("Authentication required");
        }

        AppUser user = userRepository.findByEmailIgnoreCase(auth.getName())
                .orElseThrow(() -> new BadCredentialsException("User not found"));

        if (user.getStatus() != RecordStatus.ACTIVE) {
            throw new AccessDeniedException("This account is inactive");
        }
        if (user.getCompany() != null && user.getCompany().getStatus() != RecordStatus.ACTIVE) {
            throw new AccessDeniedException("The company account is inactive");
        }
        return user;
    }

    /** ADMIN only. */
    public AppUser requireAdmin() {
        AppUser user = requireUser();
        if (user.getRole() != Role.ADMIN || user.getCompany() == null) {
            throw new AccessDeniedException("Only a company administrator can do this");
        }
        return user;
    }

    /** ADMIN or SUB_ADMIN of a company. */
    public AppUser requireCompanyUser() {
        AppUser user = requireUser();
        if ((user.getRole() != Role.ADMIN && user.getRole() != Role.SUB_ADMIN) || user.getCompany() == null) {
            throw new AccessDeniedException("Only company users can do this");
        }
        return user;
    }

    /** CUSTOMER only. */
    public AppUser requireCustomer() {
        AppUser user = requireUser();
        if (user.getRole() != Role.CUSTOMER) {
            throw new AccessDeniedException("Only customers can do this");
        }
        return user;
    }
}
