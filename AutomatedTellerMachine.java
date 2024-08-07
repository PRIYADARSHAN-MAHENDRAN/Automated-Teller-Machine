
import java.sql.*;
import java.util.Scanner;

public class AutomatedTellerMachine {

    private static Scanner scanner = new Scanner(System.in);
    private static Connection connection;
    private static String accountType;

    public static void main(String[] args) {
        try {
            connect();
            createTables();
            accountType = login();
            clear();
            if (accountType != null) {
                while (true) {
                    System.out.println("Login successful!");
                    if (accountType.equals("A")) {
                        adminoption();
                    } else if (accountType.equals("C")) {
                        customeroption();
                    }

                    int choice = scanner.nextInt();
                    scanner.nextLine();

                    switch (choice) {
                        case 1:
                            if (accountType.equals("A")) {
                                atmbalance();
                            } else if (accountType.equals("C")) {
                                myaccount();
                            }
                            break;
                        case 2:
                            if (accountType.equals("A")) {
                                ministatment();
                            } else if (accountType.equals("C")) {
                                mybalance();
                            }
                            break;
                        case 3:
                            deposit();
                            break;
                        case 4:
                            if (accountType.equals("A")) {
                                changepin();
                            } else if (accountType.equals("C")) {
                                withdraw();
                            }
                            break;
                        case 5:
                            ministatment();
                            break;
                        case 6:
                            transfer();
                            break;
                        case 7:
                            changepin();
                            break;
                        case 0:
                            main(args);
                            return;
                        default:
                        System.out.println("Invalid choice. Please try again.");
                    }
                }
            } else {
                System.out.println("Login failed. Please try again.");
            }

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                disconnect();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

    private static void connect() throws SQLException {
        String url = "jdbc:sqlite:atm.db";
        connection = DriverManager.getConnection(url);
    }

    private static void disconnect() throws SQLException {
        if (connection != null) {
            connection.close();
        }
    }

    private static void createTables() throws SQLException {
        String[] sqlquery = {
            "CREATE TABLE IF NOT EXISTS machine (time TEXT NOT NULL, atmbalance INTEGER NOT NULL)",
            "CREATE TABLE IF NOT EXISTS user (customerid INTEGER NOT NULL UNIQUE, pin TEXT NOT NULL, accounttype TEXT NOT NULL, bankname TEXT NOT NULL, userbalance INTEGER NOT NULL)",
            "CREATE TABLE IF NOT EXISTS transactionlog (id INTEGER NOT NULL, amount INTEGER NOT NULL, type TEXT NOT NULL, time TEXT NOT NULL)"
        };

        try (Statement stmt = connection.createStatement()) {
            for (String sql : sqlquery) {
                stmt.execute(sql);
            }
        }
    }

    private static String login() throws SQLException {

        int choice;

        while (true) {
            clear();
            System.out.println("1. Login");
            System.out.println("0. Exit");
            System.out.print("Enter your choice: ");
            choice = scanner.nextInt();

            if (choice == 0) {
                System.exit(0);
            } else if (choice == 1) {
                System.out.print("Enter ID num: ");
                int id = scanner.nextInt();
                scanner.nextLine();  // Consume newline

                System.out.print("Enter PIN: ");
                String pin = scanner.nextLine();

                String accountType = authenticate(id, pin);
                if (accountType != null) {
                    return accountType;
                } else {
                    System.out.println("Invalid ID or PIN. Please try again.");
                }
            } else {
                System.out.println("Please enter a valid choice number (0 or 1) only.");
            }
        }
    }

    private static String authenticate(int id, String pin) throws SQLException {
        String sql = "SELECT accounttype FROM user WHERE customerid = ? AND pin = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setInt(1, id);
            pstmt.setString(2, pin); // Changed to setString for pin
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return rs.getString("accounttype");
            } else {
                return null;
            }
        } catch (SQLException e) {
            System.out.println("Error during authentication: " + e.getMessage());
            return null;
        }

    }

    private static void adminoption() {
        System.out.println("1.ATM Balance");
        System.out.println("2.Transaction History");
        System.out.println("3.Deposit");
        System.out.println("4.Change PIN");
        System.out.println("0.Logout");
        System.out.print("Choose an option: ");
    }

    private static void customeroption() {
        System.out.println("1.My Account");
        System.out.println("2.Balance");
        System.out.println("3.Deposit");
        System.out.println("4.Withdraw");
        System.out.println("5.Mini Statment");
        System.out.println("6.Transfer");
        System.out.println("7.Change PIN");
        System.out.println("0.Exit");
        System.out.print("Choose an option: ");
    }

    private static void clear() {
        System.out.print("\033[H\033[2J");
        System.out.flush();
    }
}
