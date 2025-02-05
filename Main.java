package main;

import query.QueryService;
import auth.UserAuthentication;
import admin.AdminService;
import banking.BankingService;
import loan.LoanService;
import utils.UserNotFoundException;

import java.util.Scanner;
import utils.ColorCodes;

public class Main {

    public static void main(String[] args) throws InterruptedException {
        UserAuthentication userAuth = new UserAuthentication();
        BankingService bankingService = new BankingService();
        AdminService adminService = new AdminService();
        LoanService loanService = new LoanService();
        Scanner sc = new Scanner(System.in);
        
        String asciiArt = """
                _                           ___  _   ___  _       _      _ 
               |_)  /\\  |\\ | |/    /\\  |\\ |  |  |_    |  |_ |\\/| |_) |  |_ 
               |_) /--\\ | \\| |\\   /--\\ | \\|  |  |_    |  |_ |  | |   |_ |_ 
                                                                           
               """;
               // Print the ASCII art in one color
               System.out.println(ColorCodes.BOLD+ColorCodes.PURPLE+ColorCodes.ITALIC + asciiArt + ColorCodes.RESET);


        System.out.println(ColorCodes.BOLD + ColorCodes.colorize("========== Welcome to the Banking System ==========", ColorCodes.CYAN) + ColorCodes.RESET);
        System.out.println(ColorCodes.colorize("1. Admin Login", ColorCodes.BLUE));
        System.out.println(ColorCodes.colorize("2. Customer Login", ColorCodes.GREEN));
        System.out.println(ColorCodes.colorize("3. Register", ColorCodes.YELLOW));
        System.out.println(ColorCodes.BOLD + ColorCodes.colorize("============================================", ColorCodes.CYAN) + ColorCodes.RESET);
        System.out.print(ColorCodes.colorize("Choose an option: ", ColorCodes.PURPLE));

        int choice = sc.nextInt();
        sc.nextLine(); // Consume newline character

        switch (choice) {
            case 1 -> handleAdminLogin(userAuth, adminService, loanService, sc);
            case 2 -> handleCustomerLogin(userAuth, bankingService, loanService, sc);
            case 3 -> handleRegistration(userAuth, sc);
            default -> System.out.println(ColorCodes.colorize("Invalid choice. Exiting...", ColorCodes.RED));
        }

        sc.close();
    }

    private static void handleAdminLogin(UserAuthentication userAuth, AdminService adminService, LoanService loanService, Scanner sc) throws InterruptedException {
        System.out.print(ColorCodes.colorize("Are you an Admin? (yes/no): ", ColorCodes.PURPLE));
        String isAdmin = sc.nextLine();
        if (isAdmin.equalsIgnoreCase("yes")) {
            System.out.print(ColorCodes.colorize("Enter admin email: ", ColorCodes.PURPLE));
            String email = sc.nextLine();
            System.out.print(ColorCodes.colorize("Enter password: ", ColorCodes.PURPLE));
            String password = sc.nextLine();

            // Loading animation
            System.out.print(ColorCodes.colorize("Processing", ColorCodes.CYAN));
            for (int i = 0; i < 3; i++) {
                System.out.print(".");
                Thread.sleep(500); // Simulate processing delay
            }
            System.out.println();

            if (userAuth.adminLogin(email, password)) {
                boolean exit = false;
                while (!exit) {
                    System.out.println(ColorCodes.ITALIC + ColorCodes.colorize("\n--- Admin Menu ---", ColorCodes.BLUE) + ColorCodes.RESET);
                    System.out.println(ColorCodes.colorize("1. View All Transactions", ColorCodes.GREEN));
                    System.out.println(ColorCodes.colorize("2. View All Customers", ColorCodes.GREEN));
                    System.out.println(ColorCodes.colorize("3. View Account Details", ColorCodes.GREEN));
                    System.out.println(ColorCodes.colorize("4. Manage Accounts", ColorCodes.GREEN));
                    System.out.println(ColorCodes.colorize("5. Audit Logs", ColorCodes.GREEN));
                    System.out.println(ColorCodes.colorize("6. View Customer Queries", ColorCodes.GREEN));
                    System.out.println(ColorCodes.colorize("7. Update Query Status", ColorCodes.GREEN));
                    System.out.println(ColorCodes.colorize("8. View Loan Applications", ColorCodes.GREEN));
                    System.out.println(ColorCodes.colorize("9. Approve/Reject Loan Applications", ColorCodes.GREEN));
                    System.out.println(ColorCodes.colorize("10. Exit", ColorCodes.RED));
                    System.out.print(ColorCodes.colorize("Choose an option: ", ColorCodes.PURPLE));

                    int adminChoice = sc.nextInt();
                    sc.nextLine(); // Consume newline character
                    QueryService queryService = new QueryService();
                    switch (adminChoice) {
                        case 1 -> adminService.getAllTransactions();
                        case 2 -> adminService.viewAllCustomers();
                        case 3 -> {
                            System.out.print(ColorCodes.colorize("Enter Customer ID to view account details: ", ColorCodes.PURPLE));
                            int customerId = sc.nextInt();
                            adminService.viewAccountDetails(customerId);
                        }
                        case 4 -> manageAccounts(adminService, sc);
                        case 5 -> adminService.viewAuditLogs();
                        case 6 -> queryService.viewAllQueries();
                        case 7 -> {
                            System.out.print(ColorCodes.colorize("Enter Query ID: ", ColorCodes.PURPLE));
                            int qId = sc.nextInt();
                            sc.nextLine(); // Consume leftover newline character

                            System.out.print(ColorCodes.colorize("Enter status of query: ", ColorCodes.PURPLE));
                            String status = sc.nextLine();

                            System.out.print(ColorCodes.colorize("Enter Resolution Message: ", ColorCodes.PURPLE));
                            String resolutionMessage = sc.nextLine();

                            queryService.updateQueryStatus(qId, status, resolutionMessage, email);
                        }
                        case 8 -> loanService.viewAllLoanApplications();
                        case 9 -> {
                            System.out.print(ColorCodes.colorize("Enter Loan ID: ", ColorCodes.PURPLE));
                            int loanId = sc.nextInt();
                            sc.nextLine(); // Consume newline character

                            System.out.print(ColorCodes.colorize("Approve or Reject? (approve/reject): ", ColorCodes.PURPLE));
                            String status = sc.nextLine();

                            loanService.updateLoanStatus(loanId, status);
                        }
                        case 10 -> {
                            exit = true;
                            System.out.println(ColorCodes.colorize("Exiting Admin Menu.", ColorCodes.RED));
                        }
                        default -> System.out.println(ColorCodes.colorize("Invalid option! Please try again.", ColorCodes.RED));
                    }
                }
            } else {
                System.out.println(ColorCodes.colorize("Invalid admin credentials!", ColorCodes.RED));
            }
        } else {
            System.out.println(ColorCodes.colorize("Access restricted to Admins only.", ColorCodes.RED));
        }
    }

    private static void handleCustomerLogin(UserAuthentication userAuth, BankingService bankingService, LoanService loanService, Scanner sc) throws InterruptedException {
        System.out.print(ColorCodes.colorize("Enter email: ", ColorCodes.PURPLE));
        String email = sc.nextLine();
        System.out.print(ColorCodes.colorize("Enter password: ", ColorCodes.PURPLE));
        String password = sc.nextLine();

        // Loading animation
        System.out.print(ColorCodes.colorize("Processing", ColorCodes.CYAN));
        for (int i = 0; i < 3; i++) {
            System.out.print(".");
            Thread.sleep(500); // Simulate processing delay
        }
        System.out.println();

        try {
            if (userAuth.login(email, password)) {
                int customerId = userAuth.getCustomerId(email);
                System.out.println(ColorCodes.colorize("Login successful!", ColorCodes.GREEN));
                boolean exit = false;

                while (!exit) {
                    System.out.println(ColorCodes.BOLD + ColorCodes.colorize("\n--- Customer Banking Menu ---", ColorCodes.BLUE) + ColorCodes.RESET);
                    System.out.println(ColorCodes.colorize("1. Deposit", ColorCodes.GREEN));
                    System.out.println(ColorCodes.colorize("2. Withdraw", ColorCodes.GREEN));
                    System.out.println(ColorCodes.colorize("3. Balance Inquiry", ColorCodes.GREEN));
                    System.out.println(ColorCodes.colorize("4. Transfer Money", ColorCodes.GREEN));
                    System.out.println(ColorCodes.colorize("5. View Transaction History", ColorCodes.GREEN));
                    System.out.println(ColorCodes.colorize("6. Change Password", ColorCodes.GREEN));
                    System.out.println(ColorCodes.colorize("7. Raise a Query", ColorCodes.GREEN));
                    System.out.println(ColorCodes.colorize("8. Query Status", ColorCodes.GREEN));
                    System.out.println(ColorCodes.colorize("9. Apply for Loan", ColorCodes.GREEN));
                    System.out.println(ColorCodes.colorize("10. View Loan Status", ColorCodes.GREEN));
                    System.out.println(ColorCodes.colorize("11. Pay EMI", ColorCodes.GREEN));
                    System.out.println(ColorCodes.colorize("12. Exit", ColorCodes.RED));
                    System.out.print(ColorCodes.colorize("Choose an option: ", ColorCodes.PURPLE));

                    int customerChoice = sc.nextInt();
                    sc.nextLine(); // Consume newline character

                    QueryService queryService = new QueryService();

                    switch (customerChoice) {
                        case 1 -> {
                            System.out.print(ColorCodes.colorize("Enter amount to deposit: ", ColorCodes.PURPLE));
                            double amount = sc.nextDouble();
                            bankingService.deposit(customerId, amount);
                        }
                        case 2 -> {
                            System.out.print(ColorCodes.colorize("Enter amount to withdraw: ", ColorCodes.PURPLE));
                            double amount = sc.nextDouble();
                            bankingService.withdraw(customerId, amount);
                        }
                        case 3 -> {
                            double balance = bankingService.getBalance(customerId);
                            System.out.println(ColorCodes.colorize("Current Balance: ", ColorCodes.CYAN) + balance);
                        }
                        case 4 -> {
                            System.out.print(ColorCodes.colorize("Enter destination account ID: ", ColorCodes.PURPLE));
                            int destinationAccountId = sc.nextInt();
                            System.out.print(ColorCodes.colorize("Enter amount to transfer: ", ColorCodes.PURPLE));
                            double amount = sc.nextDouble();
                            bankingService.transferMoney(customerId, destinationAccountId, amount);
                        }
                        case 5 -> bankingService.getTransactionHistory(customerId);
                        case 6 -> {
                            System.out.print(ColorCodes.colorize("Enter old password: ", ColorCodes.PURPLE));
                            String oldPassword = sc.nextLine();
                            System.out.print(ColorCodes.colorize("Enter new password: ", ColorCodes.PURPLE));
                            String newPassword = sc.nextLine();
                            userAuth.changePassword(customerId, oldPassword, newPassword);
                        }
                        case 7 -> {
                            System.out.print(ColorCodes.colorize("Enter your query: ", ColorCodes.PURPLE));
                            String query = sc.nextLine();
                            System.out.print(ColorCodes.colorize("Enter subject: ", ColorCodes.PURPLE));
                            String subject = sc.nextLine();
                            queryService.submitQuery(customerId, subject, query);
                        }
                        case 8 -> queryService.viewQueryStatus(customerId);
                        case 9 -> {
                            System.out.print(ColorCodes.colorize("Enter Loan Type ID (e.g., 1 for Education Loan): ", ColorCodes.PURPLE));
                            int loanTypeId = sc.nextInt();
                            System.out.print(ColorCodes.colorize("Enter Loan Amount: ", ColorCodes.PURPLE));
                            double amount = sc.nextDouble();
                            System.out.print(ColorCodes.colorize("Enter Tenure (in months): ", ColorCodes.PURPLE));
                            int tenure = sc.nextInt();

                            // Loading animation for loan application
                            System.out.print(ColorCodes.colorize("Processing Loan Application", ColorCodes.CYAN));
                            for (int i = 0; i < 3; i++) {
                                System.out.print(".");
                                Thread.sleep(500); // Simulate processing delay
                            }
                            System.out.println();

                            loanService.applyForLoan(customerId, loanTypeId, amount, tenure);
                        }
                        case 10 -> loanService.viewLoanStatus(customerId);
                        case 11 -> {
                            System.out.print(ColorCodes.colorize("Enter Loan ID to pay EMI: ", ColorCodes.PURPLE));
                            int loanId = sc.nextInt();

                            System.out.print(ColorCodes.colorize("Enter Amount: ", ColorCodes.PURPLE));
                            double amount = sc.nextDouble();

                            // Loading animation for EMI payment
                            System.out.print(ColorCodes.colorize("Processing EMI Payment", ColorCodes.CYAN));
                            for (int i = 0; i < 3; i++) {
                                System.out.print(".");
                                Thread.sleep(500); // Simulate processing delay
                            }
                            System.out.println();

                            loanService.payEmi(loanId, amount);
                        }
                        case 12 -> {
                            exit = true;
                            System.out.println(ColorCodes.colorize("Thank you for using the Banking System!", ColorCodes.GREEN));
                        }
                        default -> System.out.println(ColorCodes.colorize("Invalid option! Please try again.", ColorCodes.RED));
                    }
                }
            }
        } catch (UserNotFoundException e) {
            System.err.println(ColorCodes.colorize(e.getMessage(), ColorCodes.RED));
        }
    }

    private static void handleRegistration(UserAuthentication userAuth, Scanner sc) {
        System.out.print(ColorCodes.colorize("Enter your name: ", ColorCodes.PURPLE));
        String name = sc.nextLine();
        System.out.print(ColorCodes.colorize("Enter email: ", ColorCodes.PURPLE));
        String email = sc.nextLine();
        System.out.print(ColorCodes.colorize("Enter password: ", ColorCodes.PURPLE));
        String password = sc.nextLine();

        userAuth.register(name, email, password);
    }

    private static void manageAccounts(AdminService adminService, Scanner sc) {
        System.out.println(ColorCodes.colorize("1. Delete Account", ColorCodes.GREEN));
        System.out.println(ColorCodes.colorize("2. Lock Account", ColorCodes.GREEN));
        System.out.println(ColorCodes.colorize("3. Unlock Account", ColorCodes.GREEN));
        System.out.print(ColorCodes.colorize("Choose an option: ", ColorCodes.PURPLE));

        int manageChoice = sc.nextInt();
        System.out.print(ColorCodes.colorize("Enter admin email: ", ColorCodes.PURPLE));
        String adminEmail = sc.next();
        System.out.print(ColorCodes.colorize("Enter Account ID: ", ColorCodes.PURPLE));
        int accountId = sc.nextInt();

        switch (manageChoice) {
            case 1 -> adminService.deleteAccount(accountId, adminEmail);
            case 2 -> adminService.lockAccount(accountId, adminEmail);
            case 3 -> adminService.unlockAccount(accountId);
            default -> System.out.println(ColorCodes.colorize("Invalid option! Please try again.", ColorCodes.RED));
        }
    }
}
