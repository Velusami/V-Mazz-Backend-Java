package com.example.catalog.vmazz.repository;

import com.example.catalog.vmazz.domain.OrderStatus;
import com.example.catalog.vmazz.domain.PurchaseOrder;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PurchaseOrderRepository extends JpaRepository<PurchaseOrder, Long> {

    Page<PurchaseOrder> findByCustomer_Id(Long customerId, Pageable pageable);

    Optional<PurchaseOrder> findByIdAndCustomer_Id(Long id, Long customerId);

    Page<PurchaseOrder> findBySupplierCompany_Id(Long companyId, Pageable pageable);

    Page<PurchaseOrder> findBySupplierCompany_IdAndStatus(Long companyId, OrderStatus status, Pageable pageable);

    Optional<PurchaseOrder> findByIdAndSupplierCompany_Id(Long id, Long companyId);
}
