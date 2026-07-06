package com.neobank.service;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.neobank.dto.LoanAccountDTO;
import com.neobank.entity.LoanAccount;
import com.neobank.entity.LoanApplication;
import com.neobank.entity.User;
import com.neobank.repository.LoanAccountRepository;
import com.neobank.repository.UserRepository;

@Service
public class LoanAccountService {

    @Autowired
    private LoanAccountRepository loanAccountRepository;

    @Autowired
    private UserRepository userRepository;

    private User getAuthenticatedUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authentication is required");
        }
        Object principal = authentication.getPrincipal();
        String email;
        if (principal instanceof UserDetails userDetails) {
            email = userDetails.getUsername();
        } else if (principal instanceof String str && !"anonymousUser".equals(str)) {
            email = str;
        } else {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authentication is required");
        }
        return userRepository.findByEmail(email).orElseThrow(
                () -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authenticated user not found"));
    }

    @Transactional
    public LoanAccount createAccountInternal(LoanApplication application) {
        BigDecimal emi = EmiCalculatorUtil.calculateEMI(
                application.getRequestedAmount(),
                application.getLoanProduct().getAnnualInterestRate(),
                application.getRequestedTenureMonths()
        );

        LoanAccount account = new LoanAccount();
        account.setLoanApplication(application);
        account.setUser(application.getUser());
        account.setPrincipalAmount(application.getRequestedAmount());
        account.setAnnualInterestRate(application.getLoanProduct().getAnnualInterestRate());
        account.setTenureMonths(application.getRequestedTenureMonths());
        account.setEmiAmount(emi);

        return loanAccountRepository.save(account);
    }

    @Transactional(readOnly = true)
    public List<LoanAccountDTO> getMyAccounts() {
        User user = getAuthenticatedUser();
        return loanAccountRepository.findAllByUserId(user.getId()).stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    public LoanAccountDTO toDTO(LoanAccount account) {
        return new LoanAccountDTO(
                account.getId(),
                account.getLoanApplication().getId(),
                account.getUser().getId(),
                account.getUser().getEmail(),
                account.getLoanApplication().getLoanProduct().getProductName(),
                account.getPrincipalAmount(),
                account.getAnnualInterestRate(),
                account.getTenureMonths(),
                account.getEmiAmount(),
                account.getDisbursedAt()
        );
    }
}
