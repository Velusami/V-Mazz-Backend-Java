package com.example.catalog.vmazz.repository;

import com.example.catalog.vmazz.domain.Item;
import com.example.catalog.vmazz.domain.RecordStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ItemRepository extends JpaRepository<Item, Long>, JpaSpecificationExecutor<Item> {

    boolean existsByItemCodeIgnoreCase(String itemCode);

    Optional<Item> findByIdAndCompany_Id(Long id, Long companyId);

    /** Row lock used at checkout / cancel so two buyers cannot take the same stock. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<Item> findWithLockById(Long id);

    @Query("select i.category, count(i) from Item i "
            + "where i.status = :status and i.company.status = :status group by i.category")
    List<Object[]> countByCategory(@Param("status") RecordStatus status);
}
