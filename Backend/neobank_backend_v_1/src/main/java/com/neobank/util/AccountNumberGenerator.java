// src/main/java/com/neobank/util/AccountNumberGenerator.java

package com.neobank.util;

import com.neobank.repository.AccountRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

@Component
@RequiredArgsConstructor
public class AccountNumberGenerator {

    private final AccountRepository accountRepository;

    /**
     * Format: NB + YEAR + 10-digit-sequence
     * e.g. NB202600000000001
     * First account in 2026 → NB20260000000001
     */
    public String generate() {
        String year   = String.valueOf(LocalDate.now().getYear());
        String prefix = "NB" + year;

        return accountRepository
                .findLastAccountNumberByPrefix(prefix)
                .map(last -> {
                    String seq  = last.substring(prefix.length());
                    long nextSeq = Long.parseLong(seq) + 1;
                    return buildNumber(prefix, nextSeq);
                })
                .orElse(buildNumber(prefix, 1L));
    }

    private String buildNumber(String prefix, long seq) {
        return prefix + String.format("%010d", seq);
    }
}