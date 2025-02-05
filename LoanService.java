package loan;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;

import utils.DatabaseConnection;
import notification.EmailService;
public class LoanService {

	public void viewAllLoanApplications() {
        try (Connection conn = DatabaseConnection.getConnection()) {
        	String query = """
        	        SELECT l.loan_id, l.customer_id, c.name AS customer_name, l.loan_type_id, lt.loan_name AS loan_type,
        	               l.amount, l.tenure_months, l.status, l.created_at
        	        FROM loans l
        	        INNER JOIN customers c ON l.customer_id = c.id
        	        INNER JOIN loan_types lt ON l.loan_type_id = lt.loan_type_id
        	        ORDER BY l.status, l.created_at;
        	        """;


            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery(query)) {
                System.out.println("---- Loan Applications ----");
                System.out.printf("%-10s %-15s %-15s %-15s %-15s %-10s %-10s %-20s%n",
                        "Loan ID", "Customer ID", "Customer Name", "Loan Type", "Amount", "Tenure", "Status", "Created At");

                while (rs.next()) {
                    int loanId = rs.getInt("loan_id");
                    int customerId = rs.getInt("customer_id");
                    String customerName = rs.getString("customer_name");
                    String loanType = rs.getString("loan_type");
                    double amount = rs.getDouble("amount");
                    int tenureMonths = rs.getInt("tenure_months");
                    String status = rs.getString("status");
                    String createdAt = rs.getString("created_at");

                    System.out.printf("%-10d %-15d %-15s %-15s %-15.2f %-10d %-10s %-20s%n",
                            loanId, customerId, customerName, loanType, amount, tenureMonths, status, createdAt);
                }
            }
        } catch (Exception e) {
            System.err.println("Error retrieving loan applications: " + e.getMessage());
        }
    }
	public void updateLoanStatus(int loanId, String status) {
        String query = "UPDATE loans SET status = ?, updated_at = CURRENT_TIMESTAMP WHERE loan_id = ?";
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement pstmt = connection.prepareStatement(query)) {

            pstmt.setString(1, status);
            pstmt.setInt(2, loanId);
            int rowsAffected = pstmt.executeUpdate();

            if (rowsAffected > 0) {
            	EmailService notify = new EmailService();
                System.out.println("Loan status updated to " + status + ".");
                int customerId = notify.fetchCustomerIdByLoanId(loanId);

                // Send loan status change notification
                notify.sendLoanStatusChangeNotification(customerId, status);

                // If loan is approved, generate EMI schedule
                if ("Approved".equalsIgnoreCase(status)) {
                    generateEmiSchedule(loanId);
                }
            } else {
                System.out.println("Loan not found!");
            }
        } catch (SQLException e) {
            System.err.println("Error updating loan status: " + e.getMessage());
        }
    }

	
	public void viewLoanStatus(int customerId) {
		String query = """
		        SELECT l.loan_id, lt.loan_name AS loan_type, l.amount, l.tenure_months, l.status, l.created_at
		        FROM loans l
		        INNER JOIN loan_types lt ON l.loan_type_id = lt.loan_type_id
		        WHERE l.customer_id = ?
		        ORDER BY l.created_at DESC;
		        """;


        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {

            pstmt.setInt(1, customerId);

            try (ResultSet rs = pstmt.executeQuery()) {
                System.out.println("---- Loan Status ----");
                System.out.printf("%-10s %-20s %-15s %-10s %-15s %-20s%n",
                        "Loan ID", "Loan Type", "Amount", "Tenure", "Status", "Created At");

                boolean hasLoans = false;
                while (rs.next()) {
                    hasLoans = true;
                    int loanId = rs.getInt("loan_id");
                    String loanType = rs.getString("loan_type");
                    double amount = rs.getDouble("amount");
                    int tenureMonths = rs.getInt("tenure_months");
                    String status = rs.getString("status");
                    String createdAt = rs.getString("created_at");

                    System.out.printf("%-10d %-20s %-15.2f %-10d %-15s %-20s%n",
                            loanId, loanType, amount, tenureMonths, status, createdAt);
                }

                if (!hasLoans) {
                    System.out.println("No loan applications found for this customer.");
                }
            }
        } catch (Exception e) {
            System.err.println("Error retrieving loan status: " + e.getMessage());
        }
    }
	
	public void generateEmiSchedule(int loanId) {
	    String loanQuery = "SELECT amount, tenure_months, interest_rate FROM loans l JOIN loan_types lt ON l.loan_type_id = lt.loan_type_id WHERE loan_id = ?";
	    try (Connection connection = DatabaseConnection.getConnection();
	         PreparedStatement pstmt = connection.prepareStatement(loanQuery)) {

	        pstmt.setInt(1, loanId);
	        ResultSet rs = pstmt.executeQuery();
	        if (rs.next()) {
	            double amount = rs.getDouble("amount");
	            int tenureMonths = rs.getInt("tenure_months");
	            double interestRate = rs.getDouble("interest_rate");

	            // Calculate EMI
	            double emi = (amount * (interestRate / 100) * Math.pow(1 + (interestRate / 100), tenureMonths)) /
	                         (Math.pow(1 + (interestRate / 100), tenureMonths) - 1);

	            // Prepare the email body content
	            StringBuilder emiDetails = new StringBuilder();
	            emiDetails.append("Dear Customer,\n\nYour EMI schedule has been generated successfully. Below are the details of your loan:\n\n");

	            // Insert EMIs into emis table and generate email content
	            String emiQuery = "INSERT INTO emis (loan_id, amount, due_date) VALUES (?, ?, ?)";
	            try (PreparedStatement emiStmt = connection.prepareStatement(emiQuery)) {
	                LocalDate dueDate = LocalDate.now().plusMonths(1);
	                for (int i = 0; i < tenureMonths; i++) {
	                    emiStmt.setInt(1, loanId);
	                    emiStmt.setDouble(2, emi);
	                    emiStmt.setDate(3, Date.valueOf(dueDate));
	                    emiStmt.executeUpdate();

	                    // Add details for this EMI to the email body
	                    emiDetails.append(String.format("EMI %d: %.2f, Due Date: %s\n", i + 1, emi, dueDate));

	                    dueDate = dueDate.plusMonths(1);
	                }
	            }

	            emiDetails.append("\nThank you for banking with us.\n\nBest regards,\nThe Bank Team");

	            // Send the EMI schedule email
	            EmailService notify = new EmailService();
	            int customerId = notify.fetchCustomerIdByLoanId(loanId);
	            String subject = "EMI Schedule Generated";
	            String messageBody = emiDetails.toString();
	            notify.sendCustomerNotification(customerId, subject, messageBody);

	            System.out.println("EMI schedule generated and notification sent successfully!");
	        } else {
	            System.out.println("Loan not found!");
	        }
	    } catch (SQLException e) {
	        System.err.println("Error generating EMI schedule: " + e.getMessage());
	    }
	}

	
	public void applyForLoan(int customerId, int loanTypeId, double amount, int tenureMonths) {
	    String query = "INSERT INTO loans (customer_id, loan_type_id, amount, tenure_months) VALUES (?, ?, ?, ?)";
	    try (Connection connection = DatabaseConnection.getConnection();
	         PreparedStatement pstmt = connection.prepareStatement(query)) {

	        pstmt.setInt(1, customerId);
	        pstmt.setInt(2, loanTypeId);
	        pstmt.setDouble(3, amount);
	        pstmt.setInt(4, tenureMonths);
	        pstmt.executeUpdate();

	        System.out.println("Loan application submitted successfully!");
	        EmailService notify = new EmailService();
	        notify.sendLoanApplicationNotification(customerId);
	    } catch (SQLException e) {
	        System.err.println("Error applying for loan: " + e.getMessage());
	    }
	}
	
	public void payEmi(int loanId, double amount) {
	    // Query to fetch the next pending EMI for the given loanId
	    String query = "SELECT emi_id, amount, due_date FROM emis WHERE loan_id = ? AND status = 'Pending' ORDER BY due_date ASC LIMIT 1";
	    try (Connection connection = DatabaseConnection.getConnection();
	         PreparedStatement pstmt = connection.prepareStatement(query)) {

	        pstmt.setInt(1, loanId);  // Get the specific loan using loanId
	        ResultSet rs = pstmt.executeQuery();

	        if (rs.next()) {
	            int emiId = rs.getInt("emi_id");
	            double emiAmount = rs.getDouble("amount");
	            // Check if the paid amount matches the EMI amount
	            if (emiAmount == amount) {
	                // Mark this EMI as paid
	                String updateQuery = "UPDATE emis SET status = 'Paid', paid_date = CURRENT_TIMESTAMP WHERE emi_id = ? AND loan_id = ?";
	                try (PreparedStatement updateStmt = connection.prepareStatement(updateQuery)) {
	                    updateStmt.setInt(1, emiId);
	                    updateStmt.setInt(2, loanId);
	                    updateStmt.executeUpdate();

	                    System.out.println("EMI of " + amount + " successfully paid for loan ID: " + loanId + ", EMI ID: " + emiId);

	                    // Send email notification about the EMI payment
	                    EmailService notify = new EmailService();
	                    notify.sendEmiPaymentNotification(loanId, amount);
	                }
	            } else {
	                System.out.println("Incorrect EMI amount. Expected EMI: " + emiAmount);
	            }
	        } else {
	            System.out.println("No unpaid EMIs found for the loan ID: " + loanId);
	        }
	    } catch (SQLException e) {
	        e.printStackTrace();
	    }
	}



}
