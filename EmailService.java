package notification;

import java.io.UnsupportedEncodingException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Properties;
import javax.mail.*;
import javax.mail.internet.*;

import utils.DatabaseConnection;

public class EmailService {

    private static final String SENDER_EMAIL = System.getenv("SENDER_EMAIL"); // Fetch from environment variable
    private static final String SENDER_PASSWORD = System.getenv("SENDER_PASSWORD"); // Fetch from environment variable
    private static final String SENDER_NAME = "Bank Team"; // Can also be set as an environment variable

    // Method to fetch email address and send the welcome message
    public void sendWelcomeMessage(int accountId) {
        try (Connection connection = DatabaseConnection.getConnection()) {
            String query = "SELECT email FROM customers WHERE id = ?";
            try (PreparedStatement stmt = connection.prepareStatement(query)) {
                stmt.setInt(1, accountId);
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        String recipientEmail = rs.getString("email");
                        String subject = "Welcome to Our Bank!";
                        String messageBody = String.format(
                            "Dear Customer,\n\nWelcome to our bank! Your account ID is: %d\n\nBest regards,\n%s",
                            accountId, SENDER_NAME
                        );
                        sendEmail(recipientEmail, subject, messageBody);
                    } else {
                        System.out.println("No customer found with account ID: " + accountId);
                    }
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // Method to send email
    public void sendEmail(String recipient, String subject, String messageBody) {
        if (SENDER_EMAIL == null || SENDER_PASSWORD == null) {
            System.err.println("Email or password not set in environment variables!");
            return;
        }

        Properties properties = new Properties();
        properties.put("mail.smtp.auth", "true");
        properties.put("mail.smtp.starttls.enable", "true");
        properties.put("mail.smtp.host", "smtp.gmail.com");
        properties.put("mail.smtp.port", "587");

        // Setting up the email session
        Session session = Session.getInstance(properties, new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(SENDER_EMAIL, SENDER_PASSWORD);
            }
        });

        try {
            Message message = new MimeMessage(session);
            message.setFrom(new InternetAddress(SENDER_EMAIL, SENDER_NAME)); // Include sender name
            message.setRecipient(Message.RecipientType.TO, new InternetAddress(recipient));
            message.setSubject(subject);
            message.setText(messageBody);
            Transport.send(message);
            System.out.println("Email sent successfully to: " + recipient);
        } catch (MessagingException e) {
            e.printStackTrace();
            System.err.println("Failed to send email to: " + recipient);
        } catch (UnsupportedEncodingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    }
    
    public void sendDepositNotification(int accountId, double amount) {
        String subject = "Deposit Notification";
        double updatedBalance = fetchUpdatedBalance(accountId);
        String messageBody = "Dear Customer,\n\n" +
                             "A deposit of INR " + amount + " has been made to your account (ID: " + accountId + ").\n" +
                             "Your updated account balance is INR " + updatedBalance + ".\n\n" +
                             "Thank you for banking with us.\nThe Bank Team";
        sendTransactionNotification(accountId, subject, messageBody);
    }
    
    public void sendWithdrawalNotification(int accountId, double amount) {
        String subject = "Withdrawal Notification";
        double updatedBalance = fetchUpdatedBalance(accountId);
        String messageBody = "Dear Customer,\n\n" +
                             "A withdrawal of INR " + amount + " has been processed from your account (ID: " + accountId + ").\n" +
                             "Your updated account balance is INR " + updatedBalance + ".\n\n" +
                             "Thank you for banking with us.\nThe Bank Team";
        sendTransactionNotification(accountId, subject, messageBody);
    }
    
    public void sendTransferNotification(int fromAccountId, int toAccountId, double amount) {
        // Notify sender
        String senderSubject = "Transfer Notification";
        double senderBalance = fetchUpdatedBalance(fromAccountId);
        String senderMessageBody = "Dear Customer,\n\n" +
                                   "An amount of INR " + amount + " has been transferred from your account (ID: " + fromAccountId + ") " +
                                   "to account (ID: " + toAccountId + ").\n" +
                                   "Your updated account balance is INR " + senderBalance + ".\n\n" +
                                   "Thank you for banking with us.\nThe Bank Team";
        sendTransactionNotification(fromAccountId, senderSubject, senderMessageBody);

        // Notify recipient
        String recipientSubject = "Credit Notification";
        double recipientBalance = fetchUpdatedBalance(toAccountId);
        String recipientMessageBody = "Dear Customer,\n\n" +
                                      "An amount of INR " + amount + " has been credited to your account (ID: " + toAccountId + ") " +
                                      "by account (ID: " + fromAccountId + ").\n" +
                                      "Your updated account balance is INR " + recipientBalance + ".\n\n" +
                                      "Thank you for banking with us.\nThe Bank Team";
        sendTransactionNotification(toAccountId, recipientSubject, recipientMessageBody);
    }
    
    private void sendTransactionNotification(int accountId, String subject, String messageBody) {
        try (Connection connection = DatabaseConnection.getConnection()) {
            String query = "SELECT email FROM customers INNER JOIN bank_accounts ON customers.id = bank_accounts.customer_id WHERE bank_accounts.account_id = ?";
            try (PreparedStatement stmt = connection.prepareStatement(query)) {
                stmt.setInt(1, accountId);
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        String email = rs.getString("email");
                        sendEmail(email, subject, messageBody);
                    } else {
                        System.out.println("No email found for account ID: " + accountId);
                    }
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
    
    public void sendDeleteAccountNotification(int accountId) {
        String subject = "Account Deletion Notification";
        String messageBody = "Dear Customer,\n\n" +
                             "We regret to inform you that your account (ID: " + accountId + ") has been deleted.\n" +
                             "If you have any questions or concerns, please contact our bank team.\n\n" +
                             "Thank you for your time with us.\nThe Bank Team";
        sendAccountActionNotification(accountId, subject, messageBody);
    }
    
    public void sendLockAccountNotification(int accountId) {
        String subject = "Account Lock Notification";
        String messageBody = "Dear Customer,\n\n" +
                             "We would like to inform you that your account (ID: " + accountId + ") has been locked due to certain reasons.\n" +
                             "Please contact our bank team for further assistance.\n\n" +
                             "Thank you for banking with us.\nThe Bank Team";
        sendAccountActionNotification(accountId, subject, messageBody);
    }

    public void sendUnlockAccountNotification(int accountId) {
        String subject = "Account Unlock Notification";
        String messageBody = "Dear Customer,\n\n" +
                             "We are pleased to inform you that your account (ID: " + accountId + ") has been unlocked.\n" +
                             "You can now access your account as usual.\n\n" +
                             "Thank you for banking with us.\nThe Bank Team";
        sendAccountActionNotification(accountId, subject, messageBody);
    }

    private void sendAccountActionNotification(int accountId, String subject, String messageBody) {
        try (Connection connection = DatabaseConnection.getConnection()) {
            String query = "SELECT email FROM customers INNER JOIN bank_accounts ON customers.id = bank_accounts.customer_id WHERE bank_accounts.account_id = ?";
            try (PreparedStatement stmt = connection.prepareStatement(query)) {
                stmt.setInt(1, accountId);
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        String email = rs.getString("email");
                        sendEmail(email, subject, messageBody);
                    } else {
                        System.out.println("No email found for account ID: " + accountId);
                    }
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    
    
    private double fetchUpdatedBalance(int accountId) {
        String query = "SELECT balance FROM bank_accounts WHERE account_id = ?";
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement stmt = connection.prepareStatement(query)) {

            stmt.setInt(1, accountId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getDouble("balance");
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0.0;
    }
    
    public void sendEmiPaymentNotification(int loanId, double amount) {
        String subject = "EMI Payment Confirmation";
        String messageBody = String.format(
            "Dear Customer,\n\nYour EMI payment of INR %.2f for loan ID: %d has been successfully processed.\n\n" +
            "Thank you for banking with us.\nThe Bank Team", amount, loanId);

        // Here, you should pass the customer ID to fetch the customer's email.
        int customerId = fetchCustomerIdByLoanId(loanId); // You will need to implement this method
        sendTransactionNotification(customerId, subject, messageBody);
    }

 
    public int fetchCustomerIdByLoanId(int loanId) {
        String query = "SELECT customer_id FROM loans WHERE loan_id = ?";
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement pstmt = connection.prepareStatement(query)) {

            pstmt.setInt(1, loanId);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("customer_id");
                } else {
                    System.out.println("Loan not found for ID: " + loanId);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return -1;  // Returning -1 if loan is not found
    }

 // Inside EmailService class

    public void sendLoanApplicationNotification(int customerId) {
        String subject = "Loan Application Received";
        String messageBody = "Dear Customer,\n\n" +
                             "We have received your loan application. Our team will review it and notify you about the status.\n\n" +
                             "Thank you for choosing our bank.\n\nBest regards,\nThe Bank Team";
        sendCustomerNotification(customerId, subject, messageBody);
    }

    public void sendLoanStatusChangeNotification(int customerId, String status) {
        String subject = "Loan Status Update";
        String messageBody = String.format("Dear Customer,\n\nYour loan status has been updated to: %s.\n\nThank you for banking with us.\n\nBest regards,\nThe Bank Team", status);
        sendCustomerNotification(customerId, subject, messageBody);
    }

    public void sendEmiScheduleNotification(int loanId) {
        int customerId = fetchCustomerIdByLoanId(loanId);
        String subject = "EMI Schedule Generated";
        String messageBody = "Dear Customer,\n\nYour EMI schedule has been generated successfully. Please review the details for your loan.\n\nThank you for banking with us.\n\nBest regards,\nThe Bank Team";
        sendCustomerNotification(customerId, subject, messageBody);
    }

    public void sendCustomerNotification(int customerId, String subject, String messageBody) {
        try (Connection connection = DatabaseConnection.getConnection()) {
            String query = "SELECT email FROM customers WHERE id = ?";
            try (PreparedStatement stmt = connection.prepareStatement(query)) {
                stmt.setInt(1, customerId);
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        String email = rs.getString("email");
                        sendEmail(email, subject, messageBody);
                    } else {
                        System.out.println("No email found for customer ID: " + customerId);
                    }
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    
    
}
