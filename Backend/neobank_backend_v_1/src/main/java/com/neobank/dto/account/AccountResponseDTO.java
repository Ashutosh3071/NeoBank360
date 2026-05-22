// src/main/java/com/neobank/dto/account/AccountResponseDTO.java

package com.neobank.dto.account;
import lombok.*;

@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class AccountResponseDTO {
    private Long   id;
    private String accountNumber;
    private String accountType;
    private String accountCategory;
    private String balance;
    private String availableBalance;
    private String minimumBalance;
    private String currency;
    private String branchName;
    private String ifscCode;
    private String interestRate;
    private String status;
    private String openedOn;
    private String nomineeName;
    private String nomineeRelation;
    private String lastTransactionAt;
    private boolean netBankingEnabled;
    private boolean upiEnabled;
    private boolean debitCardEnabled;
}