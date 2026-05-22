// src/main/java/com/neobank/util/TransactionReferenceGenerator.java

package com.neobank.util;

import com.neobank.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

@Component
@RequiredArgsConstructor
public class TransactionReferenceGenerator {

    private final TransactionRepository transactionRepository;

    public String generate() {
        String year   = String.valueOf(LocalDate.now().getYear());
        String prefix = "TXN" + year;

        return transactionRepository
                .findLastReferenceByPrefix(prefix)
                .map(last -> {
                    long next = Long.parseLong(last.substring(prefix.length())) + 1;
                    return prefix + String.format("%010d", next);
                })
                .orElse(prefix + String.format("%010d", 1L));
    }
}