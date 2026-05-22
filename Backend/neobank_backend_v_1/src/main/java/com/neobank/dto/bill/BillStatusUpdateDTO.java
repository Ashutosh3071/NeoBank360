package com.neobank.dto.bill;

import lombok.*;

@Data @NoArgsConstructor @AllArgsConstructor
public class BillStatusUpdateDTO {
    private String status;  // PAID or OVERDUE
}