package com.ATM;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Scanner;

class ATM {
    private User currentUser;
    private Scanner scanner;

    public ATM() {
        scanner = new Scanner(System.in);
    }

    public void run() {
        System.out.println("Welcome to the ATM.");
        System.out.println("Select an option to move ahead:-");
        System.out.println("Press 1 to create new account.");
        System.out.println("Press 2 to use your existing account.");
        System.out.print("Enter here: ");
        int option= scanner.nextInt();
        String userId="", pinNum="";
        if(option==1){
             userId= generateRandomUserId();
            System.out.print("Set your new password of 4 digits:- ");
             pinNum = scanner.next();
             System.out.println();
             if(pinNum.length()==4) {
                 User.addUser(userId, pinNum, 1000.0);
             }else{
                 System.out.println("Your pin is not of valid length, Please try again.");
             }
        } else if(option==2){
            System.out.println("Enter your User ID: ");
             userId = scanner.next();
            System.out.println("Enter your PIN: ");
             pinNum = scanner.next();
        }else {
            System.out.print("Invalid input, Please try again...");
            return;
        }
        // Authenticate user
        User user = authenticateUser(userId, pinNum);
        if (user != null) {
            currentUser = user;
            System.out.println("Authentication successful.");
            showMainMenu();
        } else {
            System.out.println("Authentication failed. Exiting...");
        }
    }
    public static String generateRandomUserId() {
        Random random = new Random();
        StringBuilder userIdBuilder = new StringBuilder();

        // Generate 6 random digits
        for (int i = 0; i < 6; i++) {
            int digit = random.nextInt(10); // Generate a random digit (0-9)
            userIdBuilder.append(digit);
        }

        String newUserId = userIdBuilder.toString();

        // Check if the new userId exists in the database
        if (userIdExistsInDatabase(newUserId)) {
            generateRandomUserId();
        }
        return newUserId;
    }

    public static boolean userIdExistsInDatabase(String userId) {
        Connection connection = null;
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            connection = DriverManager.getConnection(DatabaseConfig.JDBC_URL, DatabaseConfig.JDBC_USER, DatabaseConfig.JDBC_PASSWORD);

            String query = "SELECT COUNT(*) AS count FROM users WHERE userId = ?";
            PreparedStatement preparedStatement = connection.prepareStatement(query);
            preparedStatement.setString(1, userId);

            ResultSet resultSet = preparedStatement.executeQuery();

            if (resultSet.next()) {
                int count = resultSet.getInt("count");
                return count > 0; // If count > 0, the userId exists; otherwise, it doesn't
            }

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                if (connection != null)
                    connection.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
        return false; // If an exception occurs or no result is found, assume the userId does not exist
    }

    public static User authenticateUser(String userId, String pin) {
        Connection connection = null;
        User user = null;

        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            connection = DriverManager.getConnection(DatabaseConfig.JDBC_URL, DatabaseConfig.JDBC_USER, DatabaseConfig.JDBC_PASSWORD);
            String query = "SELECT * FROM users WHERE userId = ? AND pin = ?";
            PreparedStatement preparedStatement = connection.prepareStatement(query);
            preparedStatement.setString(1, userId);
            preparedStatement.setString(2, pin);

            ResultSet resultSet = preparedStatement.executeQuery();

            if (resultSet.next()) {
                String dbUserId = resultSet.getString("userId");
                String dbPin = resultSet.getString("pin");
                double dbBalance = resultSet.getDouble("balance");
                user = new User(dbUserId, dbPin, dbBalance);
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                if (connection != null)
                    connection.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        return user;
    }

    public void transfer(User sender, String recipientUserId, double amount) {
        // Check if the recipient exists based on the userId
        User recipient = findUserByUserId(recipientUserId);

        if (recipient != null) {
            // Recipient found, proceed with the transfer
            if (sender.getBalance() >= amount) {
                sender.withdraw(amount);
                recipient.deposit(amount);

                // Insert a transaction history record
                recordTransactionHistory(sender.getUserId(), "Transferred $" + amount + " to " + recipientUserId, amount,"Transfer");
                recordTransactionHistory(recipientUserId, "Received $" + amount + " from " + sender.getUserId(), amount,"Recieved");

                System.out.println("Transfer successful.");
            } else {
                System.out.println("Insufficient balance.");
            }
        } else {
            System.out.println("Recipient with userId '" + recipientUserId + "' not found.");
        }
    }

//    private void recordTransactionHistory(String userId, String description, double amount) {
//        Connection connection = null;
//        try {
//            Class.forName("com.mysql.cj.jdbc.Driver");
//            connection = DriverManager.getConnection(DatabaseConfig.JDBC_URL, DatabaseConfig.JDBC_USER, DatabaseConfig.JDBC_PASSWORD);
//
//            String insertQuery = "INSERT INTO transaction_history (user_id, description, amount) VALUES (?, ?, ?)";
//            PreparedStatement preparedStatement = connection.prepareStatement(insertQuery);
//            preparedStatement.setString(1, userId);
//            preparedStatement.setString(2, description);
//            preparedStatement.setDouble(3, amount);
//
//            preparedStatement.executeUpdate();
//        } catch (Exception e) {
//            e.printStackTrace();
//        } finally {
//            try {
//                if (connection != null)
//                    connection.close();
//            } catch (SQLException e) {
//                e.printStackTrace();
//            }
//        }
//    }

    private void recordTransactionHistory(String userId, String description, double amount, String transactionType) {
        Connection connection = null;
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            connection = DriverManager.getConnection(DatabaseConfig.JDBC_URL, DatabaseConfig.JDBC_USER, DatabaseConfig.JDBC_PASSWORD);

            String insertQuery = "INSERT INTO transaction_history (user_id, description, amount, transaction_type) VALUES (?, ?, ?, ?)";
            PreparedStatement preparedStatement = connection.prepareStatement(insertQuery);
            preparedStatement.setString(1, userId);
            preparedStatement.setString(2, description);
            preparedStatement.setDouble(3, amount);
            preparedStatement.setString(4, transactionType);
//            preparedStatement.setTimestamp(5, new Timestamp(new Date().getTime()));

            preparedStatement.executeUpdate();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                if (connection != null)
                    connection.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }



    public List<History> getTransactionHistoryByUserId(String userId) {
        return getTransactionHistoryByUserIdAndType(userId, null);
        }

    private void recordWithdrawalTransaction(String userId, double amount) {
        String description = "Withdrawal: $" + amount;
        recordTransactionHistory(userId, description, amount, "Withdrawal");
    }

    private void recordDepositTransaction(String userId, double amount) {
        String description = "Deposit: $" + amount;
        recordTransactionHistory(userId, description, amount, "Deposit");
    }

    public List<History> getWithdrawalHistoryByUserId(String userId) {
        return getTransactionHistoryByUserIdAndType(userId, "Withdrawal");
    }

    public List<History> getDepositHistoryByUserId(String userId) {
        return getTransactionHistoryByUserIdAndType(userId, "Deposit");
    }

    public List<History> getTransactionHistoryByUserIdAndType(String userId, String transactionTypeFilter) {
        List<History> transactionHistory = new ArrayList<History>();
        Connection connection = null;
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            connection = DriverManager.getConnection(DatabaseConfig.JDBC_URL, DatabaseConfig.JDBC_USER, DatabaseConfig.JDBC_PASSWORD);

            // Modify the SQL query based on the transaction type filter
            String query;
            if (transactionTypeFilter != null) {
                query = "SELECT * FROM transaction_history WHERE user_id = ? AND transaction_type = ?";
            } else {
                query = "SELECT * FROM transaction_history WHERE user_id = ?";
            }

            PreparedStatement preparedStatement = connection.prepareStatement(query);
            preparedStatement.setString(1, userId);

            if (transactionTypeFilter != null) {
                preparedStatement.setString(2, transactionTypeFilter);
            }

            ResultSet resultSet = preparedStatement.executeQuery();

            while (resultSet.next()) {
                // Retrieve the columns you need from the result set
                String description = resultSet.getString("description");
                double amount = resultSet.getDouble("amount");
                Timestamp transactionDate = resultSet.getTimestamp("transaction_date");
                String transactionType = resultSet.getString("transaction_type");

                // Create a TransactionRecord object and add it to the list
                transactionHistory.add(new History(description, amount, transactionDate, transactionType));
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                if (connection != null)
                    connection.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
        return transactionHistory;
    }


    private User findUserByUserId(String userId) {
        Connection connection = null;
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            connection = DriverManager.getConnection(DatabaseConfig.JDBC_URL, DatabaseConfig.JDBC_USER, DatabaseConfig.JDBC_PASSWORD);

            String query = "SELECT * FROM users WHERE userId = ?";
            PreparedStatement preparedStatement = connection.prepareStatement(query);
            preparedStatement.setString(1, userId);

            ResultSet resultSet = preparedStatement.executeQuery();

            if (resultSet.next()) {
                String dbUserId = resultSet.getString("userId");
                String dbPin = resultSet.getString("pin");
                double dbBalance = resultSet.getDouble("balance");
                return new User(dbUserId, dbPin, dbBalance);
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                if (connection != null)
                    connection.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }

        return null; // User not found in the database
    }

    private void showMainMenu() {
        while (true) {
            System.out.println("\nMain Menu:");
            System.out.println("1. Check Balance");
            System.out.println("2. Deposit");
            System.out.println("3. Withdraw");
            System.out.println("4. Transfer");
            System.out.println("5. Transaction History");
            System.out.println("6. Deposited History");
            System.out.println("7. Withdrawal History");
            System.out.println("8. Quit");

            System.out.print("Select an option: ");
            int choice = scanner.nextInt();
            scanner.nextLine(); // Consume newline

            switch (choice) {
                case 1:
                    System.out.println("Balance: $" + currentUser.getBalance());
                    break;
                case 2:
                    System.out.print("Enter deposit amount: $");
                    double depositAmount = scanner.nextDouble();
                    currentUser.deposit(depositAmount);
                    recordDepositTransaction(currentUser.getUserId(),depositAmount);
                    System.out.println("Deposited: $" + depositAmount);
                    break;
                case 3:
                    System.out.print("Enter withdrawal amount: $");
                    double withdrawalAmount = scanner.nextDouble();
                    currentUser.withdraw(withdrawalAmount);
                    recordWithdrawalTransaction(currentUser.getUserId(),withdrawalAmount);
                    break;
                case 4:
                    System.out.print("Enter recipient's User ID: ");
                    String recipientId = scanner.nextLine();
                    System.out.print("Enter the amount: ");
                    double amount = scanner.nextDouble();
                    transfer(currentUser,recipientId,amount);
                    break;
                case 5:
                    List<History> listOfHistory=getTransactionHistoryByUserId(currentUser.getUserId());
                    for(History data: listOfHistory) {
                        System.out.println(data.getDescription() + " " + data.getTransactionDate());
                    }
                    break;
                case 6:
                    List<History> listOfDeposites=getDepositHistoryByUserId(currentUser.getUserId());
                    for(History data: listOfDeposites) {
                        System.out.println(data.getDescription() + " " + data.getTransactionDate());
                    }
                    break;
                case 7:
                    List<History> listOfWithdrawals=getWithdrawalHistoryByUserId(currentUser.getUserId());
                    for(History data: listOfWithdrawals) {
                        System.out.println(data.getDescription() + " " + data.getTransactionDate());
                    }
                    break;
                case 8:
                    System.out.println("Thank you for using the ATM. Goodbye!");
                    return;
                default:
                    System.out.println("Invalid option. Try again.");
                    break;
            }
        }
    }

    public void shutdown() {
        scanner.close();
    }
}