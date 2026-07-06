package com.neobank.service;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.neobank.dto.LoanProductDTO;
import com.neobank.entity.LoanProduct;
import com.neobank.repository.LoanProductRepository;

@Service
public class LoanProductService {

    @Autowired
    private LoanProductRepository loanProductRepository;

    @org.springframework.cache.annotation.Cacheable(value = "loanProducts", key = "'all'")
    @Transactional(readOnly = true)
    public List<LoanProductDTO> getAllProducts() {
        return loanProductRepository.findAll().stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public LoanProductDTO getProductById(Long id) {
        LoanProduct product = loanProductRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Loan product not found"));
        return toDTO(product);
    }

    @org.springframework.cache.annotation.CacheEvict(value = "loanProducts", allEntries = true)
    @Transactional
    public LoanProductDTO createProduct(LoanProductDTO dto) {
        if (loanProductRepository.existsByProductName(dto.getProductName())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Product name already exists");
        }
        if (dto.getMinAmount() == null || dto.getMinAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Minimum amount must be greater than 0");
        }
        if (dto.getMaxAmount() == null || dto.getMaxAmount().compareTo(dto.getMinAmount()) <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Maximum amount must be greater than minimum amount");
        }
        if (dto.getAnnualInterestRate() == null || dto.getAnnualInterestRate().compareTo(BigDecimal.ZERO) <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Annual interest rate must be greater than 0");
        }
        if (dto.getAllowedTenures() == null || dto.getAllowedTenures().trim().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Allowed tenures list must not be empty");
        }

        LoanProduct product = new LoanProduct();
        product.setProductName(dto.getProductName());
        product.setMinAmount(dto.getMinAmount());
        product.setMaxAmount(dto.getMaxAmount());
        product.setAnnualInterestRate(dto.getAnnualInterestRate());
        product.setAllowedTenures(dto.getAllowedTenures());

        LoanProduct saved = loanProductRepository.save(product);
        return toDTO(saved);
    }

    public LoanProductDTO toDTO(LoanProduct product) {
        return new LoanProductDTO(
                product.getId(),
                product.getProductName(),
                product.getMinAmount(),
                product.getMaxAmount(),
                product.getAnnualInterestRate(),
                product.getAllowedTenures()
        );
    }
}
