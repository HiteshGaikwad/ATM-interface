package com.ATM;


import java.sql.Timestamp;

public class History {
    private String description;
    private double amount;
    private Timestamp transactionDate;
    private String transactionType;

    public History(String description, double amount, Timestamp transactionDate, String transactionType) {
        this.description = description;
        this.amount = amount;
        this.transactionDate = transactionDate;
        this.transactionType = transactionType;
    }

    public String getDescription() {
        return description;
    }

    public double getAmount() {
        return amount;
    }

    public Timestamp getTransactionDate() {
        return transactionDate;
    }

    public String getTransactionType() {
        return transactionType;
    }
}
