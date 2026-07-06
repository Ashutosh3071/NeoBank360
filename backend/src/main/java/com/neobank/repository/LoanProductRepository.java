package com.neobank.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import com.neobank.entity.LoanProduct;

public interface LoanProductRepository extends JpaRepository<LoanProduct, Long> {
    Optional<LoanProduct> findByProductName(String productName);
    boolean existsByProductName(String productName);
}
