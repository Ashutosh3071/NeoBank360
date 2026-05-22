// src/main/java/com/neobank/util/ApplicationIdGenerator.java

package com.neobank.util;

import com.neobank.repository.ApplicationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

@Component
@RequiredArgsConstructor
public class ApplicationIdGenerator {

    private final ApplicationRepository applicationRepository;

    /**
     * Generates applicationId in format: NB + YEAR + 7-digit-sequence
     * Example:
     *   First ever app in 2026  → NB20260000001
     *   Second app in 2026      → NB20260000002
     *   First app in 2027       → NB20270000001  (resets per year)
     */
    public String generate() {
        String year = String.valueOf(LocalDate.now().getYear()); // "2026"
        String prefix = "NB" + year;                            // "NB2026"

        return applicationRepository
                .findLastApplicationIdByPrefix(prefix)
                .map(lastId -> {
                    // lastId = "NB20260000042"
                    // Extract last 7 digits → "0000042" → 42 → +1 → 43
                    String sequencePart = lastId.substring(prefix.length()); // "0000042"
                    long nextSeq = Long.parseLong(sequencePart) + 1;
                    return buildId(prefix, nextSeq);
                })
                .orElse(buildId(prefix, 1L)); // No records yet → start at 1
    }

    /**
     * Pads sequence to 7 digits
     * e.g. prefix="NB2026", seq=1 → "NB20260000001"
     */
    private String buildId(String prefix, long seq) {
        return prefix + String.format("%07d", seq);
    }
}