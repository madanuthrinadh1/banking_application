package admin;
import notification.EmailService;
import utils.DatabaseConnection;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public class AdminService {
	
	public static EmailService emailService = new EmailService();
	//unlock account
	public void unlockAccount(int accountId) {
	    String unlockQuery = "UPDATE bank_accounts SET account_status = 'ACTIVE' WHERE account_id = ?";
	    try (Connection connection = DatabaseConnection.getConnection();
	         PreparedStatement preparedStatement = connection.prepareStatement(unlockQuery)) {

	        preparedStatement.setInt(1, accountId);
	        int rowsAffected = preparedStatement.executeUpdate();

	        if (rowsAffected > 0) {
	            System.out.println("Account unlocked successfully!");
	            emailService.sendUnlockAccountNotification(accountId);
	        } else {
	            System.out.println("Account not found or already active.");
	        }
	    } catch (SQLException e) {
	        System.err.println("Error unlocking account: " + e.getMessage());
	    }
	}

	//lock account
	public void lockAccount(int accountId, String performedBy) {
	    String lockQuery = "UPDATE bank_accounts SET account_status = 'LOCKED' WHERE account_id = ?";
	    try (Connection connection = DatabaseConnection.getConnection();
	         PreparedStatement pstmt = connection.prepareStatement(lockQuery)) {

	        pstmt.setInt(1, accountId);
	        int rowsAffected = pstmt.executeUpdate();

	        if (rowsAffected > 0) {
	            System.out.println("Account locked successfully!");
	            emailService.sendLockAccountNotification(accountId);
	            logAudit("LOCK_ACCOUNT", "Locked account with ID " + accountId, performedBy);
	        } else {
	            System.out.println("Account not found or already locked.");
	        }
	    } catch (SQLException e) {
	        System.err.println("Error locking account: " + e.getMessage());
	    }
	}


	//delete account
	public void deleteAccount(int accountId, String performedBy) {
	    String deleteTransactionsQuery = "DELETE FROM transactions WHERE account_id = ?";
	    String deleteAccountQuery = "DELETE FROM bank_accounts WHERE account_id = ?";
	    
	    try (Connection connection = DatabaseConnection.getConnection()) {
	        // Start transaction
	        connection.setAutoCommit(false);

	        // Delete associated transactions
	        try (PreparedStatement deleteTransactionsStmt = connection.prepareStatement(deleteTransactionsQuery)) {
	            deleteTransactionsStmt.setInt(1, accountId);
	            deleteTransactionsStmt.executeUpdate();
	        }

	        // Delete the bank account
	        try (PreparedStatement deleteAccountStmt = connection.prepareStatement(deleteAccountQuery)) {
	            deleteAccountStmt.setInt(1, accountId);
	            int rowsAffected = deleteAccountStmt.executeUpdate();

	            if (rowsAffected > 0) {
	                System.out.println("Account deleted successfully!");
	                logAudit("DELETE_ACCOUNT", "Deleted account with ID " + accountId, performedBy);
	            } else {
	                System.out.println("Account not found or already deleted.");
	            }
	        }

	        // Commit transaction
	        connection.commit();
	    } catch (SQLException e) {
	        System.err.println("Error deleting account: " + e.getMessage());
	    }
	}



	
    // View all registered customers
    public void viewAllCustomers() {
        String query = "SELECT id, name, email FROM customers";

        try (Connection connection = DatabaseConnection.getConnection();
             Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery(query)) {

            System.out.println("\n--- All Registered Customers ---");
            System.out.printf("%-10s %-20s %-30s%n", "ID", "Name", "Email");
            System.out.println("-----------------------------------------------");

            while (resultSet.next()) {
                int id = resultSet.getInt("id");
                String name = resultSet.getString("name");
                String email = resultSet.getString("email");
                System.out.printf("%-10d %-20s %-30s%n", id, name, email);
            }

        } catch (SQLException e) {
            System.err.println("Error fetching customers: " + e.getMessage());
        }
    }
    
 // View All Transactions (Admin)
    public void getAllTransactions() {
        try (Connection connection = DatabaseConnection.getConnection()) {
            String query = "SELECT * FROM transactions";
            try (Statement stmt = connection.createStatement();
                 ResultSet rs = stmt.executeQuery(query)) {
                while (rs.next()) {
                    System.out.println("Transaction ID: " + rs.getInt("transaction_id") +
                            ", Account ID: " + rs.getInt("account_id") +
                            ", Type: " + rs.getString("transaction_type") +
                            ", Amount: " + rs.getDouble("amount") +
                            ", Date: " + rs.getTimestamp("transaction_date"));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

 // Inside AdminService.java
    public void viewAccountDetails(int customerId) {
        String query = """
            SELECT 
                c.id AS customer_id, c.name AS customer_name, c.email AS customer_email,
                a.account_id AS account_id, a.account_type AS account_type, a.balance AS balance
            FROM customers c
            LEFT JOIN bank_accounts a ON c.id = a.customer_id
            WHERE c.id = ?
        """;

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(query)) {

            preparedStatement.setInt(1, customerId);
            ResultSet resultSet = preparedStatement.executeQuery();

            if (resultSet.next()) {
                System.out.println("\n--- Account Details ---");
                System.out.println("Customer ID: " + resultSet.getInt("customer_id"));
                System.out.println("Name: " + resultSet.getString("customer_name"));
                System.out.println("Email: " + resultSet.getString("customer_email"));
                System.out.println("\n--- Linked Bank Account ---");
                System.out.printf("%-15s %-15s %-15s%n", "Account ID", "Account Type", "Balance");
                System.out.println("-------------------------------------------");

                do {
                    int accountId = resultSet.getInt("account_id");
                    String accountType = resultSet.getString("account_type");
                    double balance = resultSet.getDouble("balance");

                    if (accountId > 0) {
                        System.out.printf("%-15d %-15s %-15.2f%n", accountId, accountType, balance);
                    } else {
                        System.out.println("No accounts linked to this customer.");
                    }
                } while (resultSet.next());
            } else {
                System.out.println("No customer found with ID: " + customerId);
            }

        } catch (SQLException e) {
            System.err.println("Error fetching account details: " + e.getMessage());
        }
    }
    
    	
    //log audit
    public void logAudit(String actionType, String description, String performedBy) {
        String insertLogQuery = "INSERT INTO audit_logs (action_type, action_description, performed_by) VALUES (?, ?, ?)";

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement pstmt = connection.prepareStatement(insertLogQuery)) {

            pstmt.setString(1, actionType);
            pstmt.setString(2, description);
            pstmt.setString(3, performedBy);
            pstmt.executeUpdate();

        } catch (SQLException e) {
            System.err.println("Error logging audit: " + e.getMessage());
        }
    }
    
    //view log audit
    
    public void viewAuditLogs() {
        String fetchLogsQuery = "SELECT * FROM audit_logs ORDER BY action_timestamp DESC";

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement pstmt = connection.prepareStatement(fetchLogsQuery);
             ResultSet rs = pstmt.executeQuery()) {

            System.out.println("\n--- Audit Logs ---");
            while (rs.next()) {
                System.out.println("Log ID: " + rs.getInt("log_id"));
                System.out.println("Action Type: " + rs.getString("action_type"));
                System.out.println("Description: " + rs.getString("action_description"));
                System.out.println("Timestamp: " + rs.getTimestamp("action_timestamp"));
                System.out.println("Performed By: " + rs.getString("performed_by"));
                System.out.println("----------------------------------------");
            }

        } catch (SQLException e) {
            System.err.println("Error fetching audit logs: " + e.getMessage());
        }
    }

}
