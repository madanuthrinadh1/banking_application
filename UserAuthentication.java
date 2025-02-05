package auth;
import notification.EmailService;
import utils.DatabaseConnection;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import utils.UserNotFoundException;
public class UserAuthentication {
	
	// Inside UserAuthentication.java
	public boolean adminLogin(String email, String password) {
	    String query = "SELECT * FROM admins WHERE email = ? AND password = ?";

	    try (Connection connection = DatabaseConnection.getConnection();
	         PreparedStatement preparedStatement = connection.prepareStatement(query)) {

	        preparedStatement.setString(1, email);
	        preparedStatement.setString(2, password);
	        ResultSet rs = preparedStatement.executeQuery();

	        if (rs.next()) {
	            System.out.println("Admin Login successful! Welcome, " + rs.getString("name"));
	            return true;
	        } else {
	            System.out.println("Invalid Admin credentials. Please try again.");
	            return false;
	        }

	    } catch (SQLException e) {
	        System.err.println("Error during Admin login: " + e.getMessage());
	        return false;
	    }
	}


    // Register a new customer
    public void register(String name, String email, String password) {
        String insertCustomerQuery = "INSERT INTO customers (name, email, password) VALUES (?, ?, ?)";

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(insertCustomerQuery, PreparedStatement.RETURN_GENERATED_KEYS)) {

            preparedStatement.setString(1, name);
            preparedStatement.setString(2, email);
            preparedStatement.setString(3, password);
            preparedStatement.executeUpdate();

            // Get the generated customer ID
            ResultSet rs = preparedStatement.getGeneratedKeys();
            if (rs.next()) {
                int customerId = rs.getInt(1);

                // Create a default savings account for the new customer
                createBankAccount(customerId);
            }

            System.out.println("Registration successful!");

        } catch (SQLException e) {
            System.err.println("Error during registration: " + e.getMessage());
        }
    }

    // Login a customer
    public boolean login(String email, String password) {
        String loginQuery = "SELECT * FROM customers WHERE email = ? AND password = ?";

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(loginQuery)) {

            preparedStatement.setString(1, email);
            preparedStatement.setString(2, password);

            ResultSet rs = preparedStatement.executeQuery();

            if (rs.next()) {
                System.out.println("Login successful! Welcome, " + rs.getString("name"));
                return true;
            } else {
                System.out.println("Invalid credentials. Please try again.");
                return false;
            }

        } catch (SQLException e) {
            System.err.println("Error during login: " + e.getMessage());
            return false;
        }
    }
    public int getCustomerId(String email) throws UserNotFoundException {
        try (Connection connection = DatabaseConnection.getConnection()) {
            String query = "SELECT id FROM customers WHERE email = ?";
            PreparedStatement preparedStatement = connection.prepareStatement(query);
            preparedStatement.setString(1, email);

            ResultSet resultSet = preparedStatement.executeQuery();

            if (resultSet.next()) {
                return resultSet.getInt("id"); // Return the customer ID
            } else {
                throw new UserNotFoundException("Customer not found for email: " + email);
            }
        } catch (SQLException e) {
            e.printStackTrace();
            throw new UserNotFoundException("Error fetching customer ID!");
        }
    }
    // Helper method to create a default bank account
    private void createBankAccount(int customerId) {
        String createAccountQuery = "INSERT INTO bank_accounts (customer_id, account_type, balance) VALUES (?, 'Savings', 0.00)";

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(createAccountQuery, PreparedStatement.RETURN_GENERATED_KEYS)) {

            preparedStatement.setInt(1, customerId);
            preparedStatement.executeUpdate();

            // Get the generated account ID
            ResultSet rs = preparedStatement.getGeneratedKeys();
            if (rs.next()) {
                int accountId = rs.getInt(1);
                EmailService obj = new EmailService();
                obj.sendWelcomeMessage(accountId);
                System.out.println("Your account has been created. Your Account ID is: " + accountId);
            }

        } catch (SQLException e) {
            System.err.println("Error during account creation: " + e.getMessage());
        }
    }
    
    //change password
    public void changePassword(int customerId, String oldPassword, String newPassword) {
        String checkPasswordQuery = "SELECT password FROM customers WHERE id = ?";
        String updatePasswordQuery = "UPDATE customers SET password = ? WHERE id = ?";

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement checkStmt = connection.prepareStatement(checkPasswordQuery);
             PreparedStatement updateStmt = connection.prepareStatement(updatePasswordQuery)) {

            // Verify old password
            checkStmt.setInt(1, customerId);
            ResultSet rs = checkStmt.executeQuery();

            if (rs.next()) {
                String currentPassword = rs.getString("password");
                if (!currentPassword.equals(oldPassword)) {
                    System.out.println("Incorrect old password!");
                    return;
                }
            } else {
                System.out.println("Customer not found!");
                return;
            }

            // Update to new password
            updateStmt.setString(1, newPassword);
            updateStmt.setInt(2, customerId);
            int rowsAffected = updateStmt.executeUpdate();

            if (rowsAffected > 0) {
                System.out.println("Password updated successfully!");
            } else {
                System.out.println("Failed to update password.");
            }
        } catch (SQLException e) {
            System.err.println("Error updating password: " + e.getMessage());
        }
    }

    
    
}
