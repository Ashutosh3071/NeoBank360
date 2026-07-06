package com.neobank.service;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class EmiCalculatorUtil {

    public static BigDecimal calculateEMI(BigDecimal principal, BigDecimal annualInterestRate, int tenureMonths) {
        if (principal == null || annualInterestRate == null || tenureMonths <= 0) {
            throw new IllegalArgumentException("Invalid input for EMI calculation");
        }

        double p = principal.doubleValue();
        double rate = annualInterestRate.doubleValue();
        double r = rate / 12.0 / 100.0;

        double emi;
        if (r == 0) {
            emi = p / tenureMonths;
        } else {
            emi = (p * r * Math.pow(1 + r, tenureMonths)) / (Math.pow(1 + r, tenureMonths) - 1);
        }

        return BigDecimal.valueOf(emi).setScale(2, RoundingMode.HALF_UP);
    }
}
