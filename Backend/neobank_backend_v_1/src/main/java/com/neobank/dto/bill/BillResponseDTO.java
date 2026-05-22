package com.neobank.dto.bill;

import lombok.AllArgsConstructor;
import lombok.*;

@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class BillResponseDTO {
    private Long    id;
    private String  billerName;
    private String  amount;
    private String  dueDate;
    private String  status;
    private Boolean remindMe;
    private Boolean overdue;
    private String  description;
    private String  paidAt;
    private String  createdAt;
}

// BillStatusUpdateDTO.java
