// src/main/java/com/neobank/service/AccountService.java

package com.neobank.service;

import com.neobank.dto.account.*;
import com.neobank.entity.Account;
import com.neobank.entity.Account.AccountStatus;
import com.neobank.entity.User;
import com.neobank.exception.NeoBankException;
import com.neobank.repository.AccountRepository;
import com.neobank.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.format.DateTimeFormatter;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class AccountService {

    private final AccountRepository accountRepository;
    private final UserRepository    userRepository;

    private static final DateTimeFormatter FMT =
            DateTimeFormatter.ofPattern("dd MMM yyyy");

    // ─────────────────────────────────────────────
    //  CUSTOMER: GET MY ACCOUNTS
    // ─────────────────────────────────────────────

    public List<AccountResponseDTO> getMyAccounts(String username) {
        User user = findUser(username);
        return accountRepository.findByUserIdOrderByCreatedAtDesc(user.getId())
                .stream().map(this::buildResponse).toList();
    }

    // ─────────────────────────────────────────────
    //  ADMIN: GET ALL ACCOUNTS
    // ─────────────────────────────────────────────

    public Page<AccountResponseDTO> getAllAccounts(
            AccountStatus status, Pageable pageable) {
        Page<Account> page = (status != null)
                ? accountRepository.findByStatusOrderByCreatedAtDesc(status, pageable)
                : accountRepository.findAll(pageable);
        return page.map(this::buildResponse);
    }

    // ─────────────────────────────────────────────
    //  ADMIN: UPDATE ACCOUNT STATUS
    // ─────────────────────────────────────────────

    @Transactional
    public AccountResponseDTO updateAccountStatus(
            AccountStatusUpdateDTO req, String adminUsername) {

        Account account = accountRepository.findByAccountNumber(req.getAccountNumber())
                .orElseThrow(() -> new NeoBankException(
                        HttpStatus.NOT_FOUND, "ACCOUNT_NOT_FOUND",
                        "Account not found: " + req.getAccountNumber()));

        AccountStatus newStatus;
        try {
            newStatus = AccountStatus.valueOf(req.getStatus().toUpperCase());
        } catch (Exception e) {
            throw new NeoBankException(HttpStatus.BAD_REQUEST, "INVALID_STATUS",
                    "Invalid status: " + req.getStatus());
        }

        account.setStatus(newStatus);
        account.setStatusReason(req.getReason());
        account.setUpdatedBy(adminUsername);
        accountRepository.save(account);

        log.info("Account {} status → {} by {}", account.getAccountNumber(), newStatus, adminUsername);
        return buildResponse(account);
    }

    // ─────────────────────────────────────────────
    //  PRIVATE
    // ─────────────────────────────────────────────

    private User findUser(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new NeoBankException(
                        HttpStatus.NOT_FOUND, "USER_NOT_FOUND", "User not found."));
    }

    public AccountResponseDTO buildResponse(Account a) {
        return AccountResponseDTO.builder()
                .id(a.getId())
                .accountNumber(a.getAccountNumber())
                .accountType(a.getAccountType().name())
                .accountCategory(a.getAccountCategory().name())
                .balance(a.getBalance().toPlainString())
                .availableBalance(a.getAvailableBalance().toPlainString())
                .minimumBalance(a.getMinimumBalance().toPlainString())
                .currency(a.getCurrency())
                .branchName(a.getBranchName())
                .ifscCode(a.getIfscCode())
                .interestRate(a.getInterestRate() != null ? a.getInterestRate().toPlainString() : "0")
                .status(a.getStatus().name())
                .openedOn(a.getOpenedOn() != null ? a.getOpenedOn().format(FMT) : null)
                .nomineeName(a.getNomineeName())
                .nomineeRelation(a.getNomineeRelation() != null ? a.getNomineeRelation().name() : null)
                .lastTransactionAt(a.getLastTransactionAt() != null
                        ? a.getLastTransactionAt().format(DateTimeFormatter.ofPattern("dd MMM yyyy, hh:mm a"))
                        : null)
                .netBankingEnabled(Boolean.TRUE.equals(a.getNetBankingEnabled()))
                .upiEnabled(Boolean.TRUE.equals(a.getUpiEnabled()))
                .debitCardEnabled(Boolean.TRUE.equals(a.getDebitCardEnabled()))
                .build();
    }
}