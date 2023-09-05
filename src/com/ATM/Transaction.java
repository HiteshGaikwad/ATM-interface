package com.ATM;

class Transaction {
    // Class to handle financial transactions (e.g., transfer between accounts)
    public static void transfer(User sender, User receiver, double amount) {
        sender.transfer(receiver, amount);
    }
}