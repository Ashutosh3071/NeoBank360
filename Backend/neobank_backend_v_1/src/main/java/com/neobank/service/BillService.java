// src/main/java/com/neobank/service/BillService.java

package com.neobank.service;

import com.neobank.dto.bill.*;
import com.neobank.entity.*;
import com.neobank.entity.Bill.BillStatus;
import com.neobank.exception.NeoBankException;
import com.neobank.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class BillService {

    private final BillRepository  billRepository;
    private final UserRepository  userRepository;
    private final RewardService   rewardService;

    private static final int    REMINDER_DAYS = 3;
    private static final int    BILL_PAY_POINTS = 10;
    private static final DateTimeFormatter FMT =
            DateTimeFormatter.ofPattern("dd MMM yyyy");

    // ─────────────────────────────────────────────
    //  CREATE BILL
    //  BR-01: due date must be future
    //  BR-02: no duplicate biller/month
    // ─────────────────────────────────────────────

    @Transactional
    public BillResponseDTO createBill(BillRequestDTO req, String username) {

        User user = findUser(username);

        // Validate amount
        BigDecimal amount;
        try {
            amount = new BigDecimal(req.getAmount());
        } catch (Exception e) {
            throw new NeoBankException(HttpStatus.BAD_REQUEST, "INVALID_AMOUNT",
                    "Invalid amount.");
        }
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new NeoBankException(HttpStatus.BAD_REQUEST, "INVALID_AMOUNT",
                    "Bill amount must be greater than zero.");
        }

        // Parse + validate due date (must be future)
        LocalDate dueDate;
        try {
            dueDate = LocalDate.parse(req.getDueDate());
        } catch (Exception e) {
            throw new NeoBankException(HttpStatus.BAD_REQUEST, "INVALID_DATE",
                    "Due date must be in YYYY-MM-DD format.");
        }
        if (!dueDate.isAfter(LocalDate.now())) {
            throw new NeoBankException(HttpStatus.BAD_REQUEST, "PAST_DUE_DATE",
                    "Due date must be a future date.");
        }

        // Duplicate check: same biller + same month + same year
        if (billRepository.existsDuplicate(
                user.getId(), req.getBillerName().trim(),
                dueDate.getYear(), dueDate.getMonthValue())) {
            throw new NeoBankException(HttpStatus.CONFLICT, "BILL_EXISTS",
                    "A bill for '" + req.getBillerName() +
                    "' already exists for " + dueDate.getMonth().name() +
                    " " + dueDate.getYear() + ".");
        }

        Bill bill = Bill.builder()
                .user(user)
                .billerName(req.getBillerName().trim())
                .amount(amount)
                .dueDate(dueDate)
                .status(BillStatus.PENDING)
                .description(req.getDescription())
                .build();

        Bill saved = billRepository.save(bill);
        log.info("Bill created: {} ₹{} due {} for {}", req.getBillerName(), amount, dueDate, username);

        return buildResponse(saved);
    }

    // ─────────────────────────────────────────────
    //  GET ALL BILLS
    //  Computes remindMe and overdue flags
    // ─────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<BillResponseDTO> getMyBills(String username) {
        User user = findUser(username);
        return billRepository
                .findByUserIdOrderByDueDateAsc(user.getId())
                .stream()
                .map(b -> {
                    // Auto-flag overdue
                    if (b.isOverdue()) b.setStatus(BillStatus.OVERDUE);
                    return buildResponse(b);
                })
                .toList();
    }

    // ─────────────────────────────────────────────
    //  GET ONE BILL
    // ─────────────────────────────────────────────

    @Transactional(readOnly = true)
    public BillResponseDTO getBill(Long billId, String username) {
        Bill bill = findBill(billId, username);
        return buildResponse(bill);
    }

    // ─────────────────────────────────────────────
    //  UPDATE STATUS
    //  BR-04: PENDING → PAID or PENDING → OVERDUE only
    // ─────────────────────────────────────────────

    @Transactional
    public BillResponseDTO updateStatus(
            Long billId, BillStatusUpdateDTO req, String username) {

        Bill bill = findBill(billId, username);

        BillStatus newStatus;
        try {
            newStatus = BillStatus.valueOf(req.getStatus().toUpperCase());
        } catch (Exception e) {
            throw new NeoBankException(HttpStatus.BAD_REQUEST, "INVALID_STATUS",
                    "Valid statuses: PAID, OVERDUE.");
        }

        // Validate transition
        if (bill.getStatus() != BillStatus.PENDING) {
            throw new NeoBankException(HttpStatus.BAD_REQUEST, "INVALID_TRANSITION",
                    "Only PENDING bills can be updated. Current status: " +
                    bill.getStatus().name());
        }
        if (newStatus == BillStatus.PENDING) {
            throw new NeoBankException(HttpStatus.BAD_REQUEST, "INVALID_TRANSITION",
                    "Cannot set status back to PENDING.");
        }

        bill.setStatus(newStatus);
        if (newStatus == BillStatus.PAID) {
            bill.setPaidAt(LocalDateTime.now());
            // Award reward points for paying a bill
            try {
                rewardService.earnPoints(
                        bill.getUser().getId(),
                        BILL_PAY_POINTS,
                        "Bill payment: " + bill.getBillerName()
                );
            } catch (Exception e) {
                log.warn("Reward accrual failed for bill {}", billId);
            }
        }

        billRepository.save(bill);
        log.info("Bill {} status → {} by {}", billId, newStatus, username);

        return buildResponse(bill);
    }

    // ─────────────────────────────────────────────
    //  DELETE BILL
    // ─────────────────────────────────────────────

    @Transactional
    public void deleteBill(Long billId, String username) {
        Bill bill = findBill(billId, username);
        billRepository.delete(bill);
    }

    // ─────────────────────────────────────────────
    //  PRIVATE
    // ─────────────────────────────────────────────

    private Bill findBill(Long billId, String username) {
        User user = findUser(username);
        Bill bill = billRepository.findById(billId)
                .orElseThrow(() -> new NeoBankException(
                        HttpStatus.NOT_FOUND, "BILL_NOT_FOUND",
                        "Bill not found."));
        if (!bill.getUser().getId().equals(user.getId())) {
            throw new NeoBankException(HttpStatus.FORBIDDEN, "ACCESS_DENIED",
                    "Access denied.");
        }
        return bill;
    }

    private User findUser(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new NeoBankException(
                        HttpStatus.NOT_FOUND, "USER_NOT_FOUND", "User not found."));
    }

    private BillResponseDTO buildResponse(Bill b) {
        return BillResponseDTO.builder()
                .id(b.getId())
                .billerName(b.getBillerName())
                .amount(b.getAmount().toPlainString())
                .dueDate(b.getDueDate().format(FMT))
                .status(b.getStatus().name())
                .remindMe(b.isDueSoon(REMINDER_DAYS))
                .overdue(b.isOverdue())
                .description(b.getDescription())
                .paidAt(b.getPaidAt() != null
                        ? b.getPaidAt().format(
                            DateTimeFormatter.ofPattern("dd MMM yyyy, hh:mm a")) : null)
                .createdAt(b.getCreatedAt() != null
                        ? b.getCreatedAt().format(
                            DateTimeFormatter.ofPattern("dd MMM yyyy")) : null)
                .build();
    }
}