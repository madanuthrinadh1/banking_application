package query;
import notification.EmailService;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import utils.DatabaseConnection;

public class QueryService {
	public void submitQuery(int customerId, String subject, String description) {
	    String insertQuery = "INSERT INTO queries (customer_id, query_subject, query_description) VALUES (?, ?, ?)";
	    try (Connection connection = DatabaseConnection.getConnection();
	         PreparedStatement pstmt = connection.prepareStatement(insertQuery)) {

	        pstmt.setInt(1, customerId);
	        pstmt.setString(2, subject);
	        pstmt.setString(3, description);
	        pstmt.executeUpdate();

	        System.out.println("Your query has been submitted successfully.");
	    } catch (SQLException e) {
	        System.err.println("Error submitting query: " + e.getMessage());
	    }
	}

	public void viewQueryStatus(int customerId) {
	    String selectQuery = "SELECT query_id, query_subject, status, updated_at FROM queries WHERE customer_id = ?";
	    try (Connection connection = DatabaseConnection.getConnection();
	         PreparedStatement pstmt = connection.prepareStatement(selectQuery)) {

	        pstmt.setInt(1, customerId);
	        ResultSet rs = pstmt.executeQuery();

	        System.out.println("\n--- Your Queries ---");
	        System.out.printf("%-10s %-30s %-15s %-20s%n", "Query ID", "Subject", "Status", "Last Updated");
	        while (rs.next()) {
	            System.out.printf("%-10d %-30s %-15s %-20s%n",
	                              rs.getInt("query_id"),
	                              rs.getString("query_subject"),
	                              rs.getString("status"),
	                              rs.getTimestamp("updated_at"));
	        }
	    } catch (SQLException e) {
	        System.err.println("Error retrieving query status: " + e.getMessage());
	    }
	}

	public void viewAllQueries() {
	    String selectQuery = "SELECT q.query_id, c.name AS customer_name, q.query_subject, q.status, q.created_at " +
	                         "FROM queries q JOIN customers c ON q.customer_id = c.id ORDER BY q.status, q.created_at";
	    try (Connection connection = DatabaseConnection.getConnection();
	         PreparedStatement pstmt = connection.prepareStatement(selectQuery);
	         ResultSet rs = pstmt.executeQuery()) {

	        System.out.println("\n--- All Customer Queries ---");
	        System.out.printf("%-10s %-20s %-30s %-15s %-20s%n", "Query ID", "Customer Name", "Subject", "Status", "Created At");
	        while (rs.next()) {
	            System.out.printf("%-10d %-20s %-30s %-15s %-20s%n",
	                              rs.getInt("query_id"),
	                              rs.getString("customer_name"),
	                              rs.getString("query_subject"),
	                              rs.getString("status"),
	                              rs.getTimestamp("created_at"));
	        }
	    } catch (SQLException e) {
	        System.err.println("Error retrieving queries: " + e.getMessage());
	    }
	}

	
	public void updateQueryStatus(int queryId, String status, String resolutionMessage, String performedBy) {
	    String updateQuery = "UPDATE queries SET status = ?, resolution_message = ?, performed_by = ?, updated_at = CURRENT_TIMESTAMP WHERE query_id = ?";
	    try (Connection connection = DatabaseConnection.getConnection();
	         PreparedStatement pstmt = connection.prepareStatement(updateQuery)) {

	        pstmt.setString(1, status);
	        pstmt.setString(2, resolutionMessage);
	        pstmt.setString(3, performedBy);
	        pstmt.setInt(4, queryId);

	        int rowsAffected = pstmt.executeUpdate();
	        if (!status.equals("Pending") && !status.equals("In Progress") && !status.equals("Resolved")) {
	            System.out.println("Invalid status value. Please enter 'Pending', 'In Progress', or 'Resolved'.");
	            return;
	        }
	        if (rowsAffected > 0) {
	            System.out.println("Query status and resolution message updated successfully!");
	            sendResolutionNotification(queryId, resolutionMessage, performedBy);
	        } else {
	            System.out.println("Query not found.");
	        }
	    } catch (SQLException e) {
	        System.err.println("Error updating query status: " + e.getMessage());
	    }
	}


	private void sendResolutionNotification(int queryId, String resolutionMessage, String performedBy) {
	    String fetchCustomerEmailQuery = "SELECT c.email FROM queries q JOIN customers c ON q.customer_id = c.id WHERE q.query_id = ?";
	    try (Connection connection = DatabaseConnection.getConnection();
	         PreparedStatement pstmt = connection.prepareStatement(fetchCustomerEmailQuery)) {

	        pstmt.setInt(1, queryId);
	        ResultSet rs = pstmt.executeQuery();

	        if (rs.next()) {
	            String customerEmail = rs.getString("email");
	            String subject = "Your Query has been Resolved";
	            String messageBody = "Dear Customer,\n\n" +
	                                 "Your query with ID " + queryId + " has been resolved. Below is the resolution:\n\n" +
	                                 resolutionMessage + "\n\n" +
	                                 "Best regards,\nThe Bank Team";
	            EmailService email = new EmailService();
	            email.sendEmail(customerEmail, subject, messageBody);
	        }
	    } catch (SQLException e) {
	        System.err.println("Error sending resolution notification: " + e.getMessage());
	    }
	}

}
