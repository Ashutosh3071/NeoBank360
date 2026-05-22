// src/main/java/com/neobank/util/UpiReferenceGenerator.java

package com.neobank.util;

import com.neobank.repository.UpiTransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

@Component
@RequiredArgsConstructor
public class UpiReferenceGenerator {

    private final UpiTransactionRepository upiTransactionRepository;

    /**
     * Format: UPI + YEAR + MONTH + 10-digit-sequence
     * e.g. UPI2026050000000001
     *
     * Guarantees uniqueness within the month by reading the last
     * reference stored in upi_transactions for the current prefix.
     * Falls back to sequence 1 if no record exists yet this month.
     */
    public String generate() {
        String year   = String.valueOf(LocalDate.now().getYear());
        String month  = String.format("%02d", LocalDate.now().getMonthValue());
        String prefix = "UPI" + year + month;

        return upiTransactionRepository
                .findLastReferenceByPrefix(prefix)          // add this query — see note below
                .map(last -> {
                    String seq     = last.substring(prefix.length());
                    long   nextSeq = Long.parseLong(seq) + 1;
                    return buildReference(prefix, nextSeq);
                })
                .orElse(buildReference(prefix, 1L));
    }

    private String buildReference(String prefix, long seq) {
        return prefix + String.format("%010d", seq);
    }
}