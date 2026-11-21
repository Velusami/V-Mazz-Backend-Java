package com.example.catalog.vmazz.repository;

import com.example.catalog.vmazz.domain.AppUser;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AppUserRepository extends JpaRepository<AppUser, Long> {

    Optional<AppUser> findByEmailIgnoreCase(String email);

    boolean existsByEmailIgnoreCase(String email);

    Page<AppUser> findByCompany_Id(Long companyId, Pageable pageable);

    Optional<AppUser> findByIdAndCompany_Id(Long id, Long companyId);
}
