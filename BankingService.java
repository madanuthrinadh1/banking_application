package banking;
import notification.EmailService;
import utils.DatabaseConnection;

import java.sql.*;

public class BankingService {
    // Deposit
	public static EmailService emailService = new EmailService(); 
    public void deposit(int accountId, double amount) {
        try (Connection connection = DatabaseConnection.getConnection()) {
            String updateBalanceQuery = "UPDATE bank_accounts SET balance = balance + ? WHERE account_id = ?";
            PreparedStatement pstmt = connection.prepareStatement(updateBalanceQuery);
            pstmt.setDouble(1, amount);
            pstmt.setInt(2, accountId);
            int rowsAffected = pstmt.executeUpdate();

            if (rowsAffected > 0) {
                System.out.println("Deposit successful!");
                emailService.sendDepositNotification(accountId, amount);
                logTransaction(accountId, "DEPOSIT", amount);
            } else {
                System.out.println("Failed to deposit. Account not found.");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // Withdraw
    public void withdraw(int accountId, double amount) {
        try (Connection connection = DatabaseConnection.getConnection()) {
            String checkBalanceQuery = "SELECT balance FROM bank_accounts  WHERE account_id = ?";
            PreparedStatement checkStmt = connection.prepareStatement(checkBalanceQuery);
            checkStmt.setInt(1, accountId);
            ResultSet rs = checkStmt.executeQuery();

            if (rs.next() && rs.getDouble("balance") >= amount) {
                String updateBalanceQuery = "UPDATE bank_accounts SET balance = balance - ? WHERE account_id = ?";
                PreparedStatement updateStmt = connection.prepareStatement(updateBalanceQuery);
                updateStmt.setDouble(1, amount);
                updateStmt.setInt(2, accountId);
                updateStmt.executeUpdate();

                System.out.println("Withdrawal successful!");
                emailService.sendWithdrawalNotification(accountId, amount);
                logTransaction(accountId, "WITHDRAW", amount);
            } else {
                System.out.println("Insufficient balance or account not found.");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // Balance Inquiry
    public double getBalance(int accountId) {
        try (Connection connection = DatabaseConnection.getConnection()) {
            String query = "SELECT balance FROM bank_accounts WHERE account_id = ?";
            PreparedStatement pstmt = connection.prepareStatement(query);
            pstmt.setInt(1, accountId);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                return rs.getDouble("balance");
            } else {
                System.out.println("Account not found.");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0.0;
    }

    // Log Transaction
    private void logTransaction(int accountId, String transactionType, double amount) {
        try (Connection connection = DatabaseConnection.getConnection()) {
            String insertTransactionQuery = "INSERT INTO transactions (account_id, transaction_type, amount) VALUES (?, ?, ?)";
            PreparedStatement pstmt = connection.prepareStatement(insertTransactionQuery);
            pstmt.setInt(1, accountId);
            pstmt.setString(2, transactionType);
            pstmt.setDouble(3, amount);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
    
    //transfer money
    public void transferMoney(int sourceAccountId, int destinationAccountId, double amount) {
        try (Connection connection = DatabaseConnection.getConnection()) {
            connection.setAutoCommit(false);

            // Withdraw from source
            String withdrawQuery = "UPDATE bank_accounts SET balance = balance - ? WHERE account_id = ?";
            try (PreparedStatement withdrawStmt = connection.prepareStatement(withdrawQuery)) {
                withdrawStmt.setDouble(1, amount);
                withdrawStmt.setInt(2, sourceAccountId);
                withdrawStmt.executeUpdate();
            }

            // Deposit to destination
            String depositQuery = "UPDATE bank_accounts SET balance = balance + ? WHERE account_id = ?";
            try (PreparedStatement depositStmt = connection.prepareStatement(depositQuery)) {
                depositStmt.setDouble(1, amount);
                depositStmt.setInt(2, destinationAccountId);
                depositStmt.executeUpdate();
            }

            // Log transactions
            logTransaction(connection, sourceAccountId, "TRANSFER OUT", amount);
            logTransaction(connection, destinationAccountId, "TRANSFER IN", amount);

            connection.commit();
            System.out.println("Transfer successful!");
            emailService.sendTransferNotification(sourceAccountId, destinationAccountId, amount);
        } catch (SQLException e) {
            e.printStackTrace();
            throw new RuntimeException("Transaction failed", e);
        }
    }
    
    //log transaction
    private void logTransaction(Connection connection, int accountId, String transactionType, double amount) throws SQLException {
        String insertTransactionQuery = "INSERT INTO transactions (account_id, transaction_type, amount) VALUES (?, ?, ?)";
        try (PreparedStatement pstmt = connection.prepareStatement(insertTransactionQuery)) {
            pstmt.setInt(1, accountId);
            pstmt.setString(2, transactionType);
            pstmt.setDouble(3, amount);
            pstmt.executeUpdate();
        }
    }

 
    // View Customer Transactions
    public void getTransactionHistory(int accountId) {
        try (Connection connection = DatabaseConnection.getConnection()) {
            String query = "SELECT * FROM transactions WHERE account_id = ?";
            try (PreparedStatement stmt = connection.prepareStatement(query)) {
                stmt.setInt(1, accountId);
                try (ResultSet rs = stmt.executeQuery()) {
                    while (rs.next()) {
                        System.out.println("Transaction ID: " + rs.getInt("transaction_id") +
                                ", Type: " + rs.getString("transaction_type") +
                                ", Amount: " + rs.getDouble("amount") +
                                ", Date: " + rs.getTimestamp("transaction_date"));
                    }
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
    
    

}
