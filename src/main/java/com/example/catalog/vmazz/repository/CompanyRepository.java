package com.example.catalog.vmazz.repository;

import com.example.catalog.vmazz.domain.Company;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CompanyRepository extends JpaRepository<Company, Long> {

    boolean existsByGstNumberIgnoreCase(String gstNumber);
}
