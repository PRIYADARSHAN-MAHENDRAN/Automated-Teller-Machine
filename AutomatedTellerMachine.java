
import java.sql.*;
import java.util.Random;
import java.util.Scanner;

public class AutomatedTellerMachine {

    private static final Scanner scanner = new Scanner(System.in);
    private static Connection connection;
    private static String accountType;
    private static String bankname;
    private static int customerId;

    public static void main(String[] args) {
        try {
            connect();
            createTables();
            accountType = login();
            clearScreen();

            if (accountType != null) {
                System.out.println("Login successful!");

                while (true) {
                    if (accountType.equals("A")) {
                        adminOptions();
                    } else if (accountType.equals("C")) {
                        customerOptions();
                    }

                    int choice = scanner.nextInt();
                    scanner.nextLine();  // Consume newline

                    switch (choice) {
                        case 1:
                            if (accountType.equals("A")) {
                                viewAtmBalance();
                            } else if (accountType.equals("C")) {
                                viewMyAccount();
                            }
                            break;
                        case 2:
                            if (accountType.equals("A")) {
                                viewTransactionHistory();
                            } else if (accountType.equals("C")) {
                                viewMyBalance();
                            }
                            break;
                        case 3:
                            deposit();
                            break;
                        case 4:
                            if (accountType.equals("A")) {
                                changeAdminPin();
                            } else if (accountType.equals("C")) {
                                withdraw();
                            }
                            break;
                        case 5:
                            viewMiniStatement();
                            break;
                        case 6:
                            transferFunds();
                            break;
                        case 7:
                            changeCustomerPin();
                            break;
                        case 0:
                            System.out.println("Logging out...");
                            accountType = login(); // Re-login the user
                            clearScreen();
                            break;
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
        String[] sqlQuery = {
            "CREATE TABLE IF NOT EXISTS machine (time TEXT NOT NULL, atmbalance INTEGER NOT NULL)",
            "CREATE TABLE IF NOT EXISTS user (customerid INTEGER NOT NULL UNIQUE, pin TEXT NOT NULL, accounttype TEXT NOT NULL, bankname TEXT NOT NULL, useraccountbalance INTEGER NOT NULL, userwalletbalance INTEGER NOT NULL)",
            "CREATE TABLE IF NOT EXISTS transactionlog (customerid INTEGER NOT NULL, amount INTEGER NOT NULL, type TEXT NOT NULL, time TEXT NOT NULL, FOREIGN KEY(customerid) REFERENCES user(customerid))"
        };

        try (Statement stmt = connection.createStatement()) {
            for (String sql : sqlQuery) {
                stmt.execute(sql);
            }
        }
    }

    private static String login() throws SQLException {
        int choice;

        while (true) {
            clearScreen();
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
                    customerId = id;
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
        String sql = "SELECT accounttype, bankname FROM user WHERE customerid = ? AND pin = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setInt(1, id);
            pstmt.setString(2, pin);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                bankname = rs.getString("bankname");
                return rs.getString("accounttype");
            } else {
                return null;
            }
        } catch (SQLException e) {
            System.out.println("Error during authentication: " + e.getMessage());
            return null;
        }
    }

    private static void adminOptions() {
        System.out.println("1. ATM Balance");
        System.out.println("2. Transaction History");
        System.out.println("3. Deposit");
        System.out.println("4. Change PIN");
        System.out.println("0. Logout");
        System.out.print("Choose an option: ");
    }

    private static void customerOptions() {
        System.out.println("1. My Account");
        System.out.println("2. Balance");
        System.out.println("3. Deposit");
        System.out.println("4. Withdraw");
        System.out.println("5. Mini Statement");
        System.out.println("6. Transfer");
        System.out.println("7. Change PIN");
        System.out.println("0. Logout");
        System.out.print("Choose an option: ");
    }

    private static void viewAtmBalance() throws SQLException {
        System.out.print("ATM BALANCE : $");
        String sql = "SELECT * FROM machine";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                System.out.println(rs.getInt("atmbalance") + "  time :" + rs.getString("time"));
            } else {
                System.out.println("No ATM balance found.");
            }
        } catch (SQLException e) {
            System.out.println("Error retrieving ATM balance: " + e.getMessage());
        }
    }

    private static void viewTransactionHistory() throws SQLException {
        System.out.println("Transaction History");
        String sql = "SELECT * FROM transactionlog ";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                String transactionnum = rs.getString("transaction_number");
                int id = rs.getInt("customerid");
                int amount = rs.getInt("amount");
                String type = rs.getString("type");
                String time = rs.getString("time");
                System.out.printf("Transaction id: %s,ID: %d, Amount: %d, Type: %s, Time: %s%n", transactionnum, id, amount, type, time);
            }
        } catch (SQLException e) {
            System.out.println("Error retrieving transaction history: " + e.getMessage());
        }
    }

    private static void viewMyAccount() throws SQLException {
        String sql = "SELECT customerid, bankname, useraccountbalance, userwalletbalance FROM user WHERE customerid = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setInt(1, customerId);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                int id = rs.getInt("customerid");
                String bankName = rs.getString("bankname");
                int accountBalance = rs.getInt("useraccountbalance");
                int walletBalance = rs.getInt("userwalletbalance");
                System.out.printf("ID: %d, Bank: %s, Account Balance: %d, Wallet Balance: %d%n", id, bankName, accountBalance, walletBalance);
            } else {
                System.out.println("No account information found for customer ID: " + customerId);
            }
        } catch (SQLException e) {
            System.out.println("Error retrieving account information: " + e.getMessage());
        }
    }

    private static void viewMyBalance() throws SQLException {
        String sql = "SELECT useraccountbalance, userwalletbalance FROM user WHERE customerid = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setInt(1, customerId);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                int accountBalance = rs.getInt("useraccountbalance");
                int walletBalance = rs.getInt("userwalletbalance");
                System.out.printf("Account Balance: %d%nWallet Balance: %d%n", accountBalance, walletBalance);
            } else {
                System.out.println("No account information found for customer ID: " + customerId);
            }
        } catch (SQLException e) {
            System.out.println("Error retrieving account information: " + e.getMessage());
        }
    }

    private static void deposit() throws SQLException {
        int amount = -1; // Initialize amount to an invalid value
        int walletBalance = getWalletBalance(); // Get current wallet balance

        // Prompt for valid deposit amount
        while (amount <= 0 || amount > walletBalance) {
            System.out.print("Enter amount to deposit: ");
            amount = scanner.nextInt();

            if (amount > walletBalance) {
                System.out.println("Insufficient wallet balance. Available balance: " + walletBalance);
            } else if (amount <= 0) {
                System.out.println("Please enter a positive amount.");
            }
        }

        scanner.nextLine();
        int depositAmount = !bankname.equalsIgnoreCase("kdfc") ? amount - ((amount / 100) * 5) : amount;
        String sql;
        // Begin transaction to ensure atomicity
        connection.setAutoCommit(false);
        try {
            // Update user's account balance and wallet balance
            if (accountType.equals("C")) {
                sql = "UPDATE user SET useraccountbalance = useraccountbalance + ?, userwalletbalance = userwalletbalance - ? WHERE customerid = ?";
                try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
                    pstmt.setInt(1, depositAmount);
                    pstmt.setInt(2, amount);
                    pstmt.setInt(3, customerId);
                    int rowsUpdated = pstmt.executeUpdate();
                    if (rowsUpdated <= 0) {
                        System.out.println("Failed to deposit. Please try again.");
                        connection.rollback();
                        return;
                    }
                }
            } else {
                sql = "UPDATE user SET userwalletbalance = userwalletbalance - ? WHERE customerid = ?";
                try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
                    pstmt.setInt(1, amount);
                    pstmt.setInt(2, customerId);
                    int rowsUpdated = pstmt.executeUpdate();
                    if (rowsUpdated <= 0) {
                        System.out.println("Failed to deposit. Please try again.");
                        connection.rollback();
                        return;
                    }
                }
            }

            // Update ATM balance
            sql = "UPDATE machine SET time = datetime('now'), atmbalance = atmbalance + ? WHERE time IS NOT NULL";
            try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
                pstmt.setInt(1, amount);
                int rowsUpdated = pstmt.executeUpdate();
                if (rowsUpdated <= 0) {
                    System.out.println("Failed to update ATM balance.");
                    connection.rollback();
                    return;
                }
            }

            // Generate a unique 16-digit transaction number
            String transactionNumber = generateUniqueTransactionNumber();

            // Insert transaction log entry
            sql = "INSERT INTO transactionlog (transaction_number, customerid, amount, type, time) VALUES (?, ?, ?, 'DEPOSIT', datetime('now'))";
            try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
                pstmt.setString(1, transactionNumber);
                pstmt.setInt(2, customerId);
                pstmt.setInt(3, amount);
                pstmt.executeUpdate();
            }

            // Commit transaction if all operations succeed
            connection.commit();
            System.out.println("Deposit successful. Your new wallet balance is: " + getWalletBalance());

        } catch (SQLException e) {
            System.out.println("Error during deposit: " + e.getMessage());
            connection.rollback(); // Roll back transaction in case of error
        } finally {
            connection.setAutoCommit(true); // Restore auto-commit mode
        }
    }

    private static String generateUniqueTransactionNumber() throws SQLException {
        Random random = new Random();
        String transactionNumber;
        boolean isUnique = false;

        do {
            transactionNumber = String.format("%016d", Math.abs(random.nextLong())); // Generate a 16-digit number
            String sql = "SELECT COUNT(*) FROM transactionlog WHERE transaction_number = ?";
            try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
                pstmt.setString(1, transactionNumber);
                ResultSet rs = pstmt.executeQuery();
                if (rs.next() && rs.getInt(1) == 0) {
                    isUnique = true;
                }
            }
        } while (!isUnique); // Repeat until a unique transaction number is generated

        return transactionNumber;
    }

    private static int getWalletBalance() {
        String sql = "SELECT userwalletbalance FROM user WHERE customerid = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setInt(1, customerId);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return rs.getInt("userwalletbalance");
            } else {
                System.out.println("No wallet balance found for customer ID: " + customerId);
                return 0; // Return 0 if no balance is found
            }
        } catch (SQLException e) {
            System.out.println("Error retrieving wallet balance: " + e.getMessage());
            return 0;
        }
    }

    private static void withdraw() throws SQLException {
        int amount = -1; // Initialize amount to an invalid value
        int accountBalance = getAccountBalance(); // Get current account balance

        // Prompt for valid withdraw amount
        while (amount < 0 || amount > accountBalance) {
            System.out.print("Enter amount to withdraw: ");
            amount = scanner.nextInt();

            if (amount > accountBalance) {
                System.out.println("Insufficient account balance. Available balance: " + accountBalance);
            } else if (amount <= 0) {
                System.out.println("Please enter a positive amount.");
            }
        }

        scanner.nextLine();
        int withdrawAmount = !bankname.equalsIgnoreCase("kdfc") ? amount - ((amount / 100) * 5) : amount;
        String sql;
        // Begin transaction to ensure atomicity
        connection.setAutoCommit(false);
        try {
            // Update user's account balance and wallet balance

            sql = "UPDATE user SET useraccountbalance = useraccountbalance - ?, userwalletbalance = userwalletbalance + ? WHERE customerid = ?";
            try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
                pstmt.setInt(1, amount);
                pstmt.setInt(2, withdrawAmount);
                pstmt.setInt(3, customerId);
                int rowsUpdated = pstmt.executeUpdate();
                if (rowsUpdated <= 0) {
                    System.out.println("Failed to deposit. Please try again.");
                    connection.rollback();
                    return;
                }
            }

            // Update ATM balance
            sql = "UPDATE machine SET time = datetime('now'), atmbalance = atmbalance - ? WHERE time IS NOT NULL";
            try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
                pstmt.setInt(1, amount);
                int rowsUpdated = pstmt.executeUpdate();
                if (rowsUpdated <= 0) {
                    System.out.println("Failed to update ATM balance.");
                    connection.rollback();
                    return;
                }
            }

            // Generate a unique 16-digit transaction number
            String transactionNumber = generateUniqueTransactionNumber();

            // Insert transaction log entry
            sql = "INSERT INTO transactionlog (transaction_number, customerid, amount, type, time) VALUES (?, ?, ?, 'WITHDRAW', datetime('now'))";
            try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
                pstmt.setString(1, transactionNumber);
                pstmt.setInt(2, customerId);
                pstmt.setInt(3, amount);
                pstmt.executeUpdate();
            }

            // Commit transaction if all operations succeed
            connection.commit();
            System.out.println("Withdraw successful. Your new account balance is: " + getAccountBalance());

        } catch (SQLException e) {
            System.out.println("Error during withdraw: " + e.getMessage());
            connection.rollback(); // Roll back transaction in case of error
        } finally {
            connection.setAutoCommit(true); // Restore auto-commit mode
        }
    }

    private static int getAccountBalance() {
        String sql = "SELECT useraccountbalance FROM user WHERE customerid = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setInt(1, customerId);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return rs.getInt("useraccountbalance");
            } else {
                System.out.println("No wallet balance found for customer ID: " + customerId);
                return 0; // Return 0 if no balance is found
            }
        } catch (SQLException e) {
            System.out.println("Error retrieving wallet balance: " + e.getMessage());
            return 0;
        }
    }

    private static void viewMiniStatement() throws SQLException {
        System.out.println("Transaction History");
        String sql = "SELECT * FROM transactionlog WHERE customerid=?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setInt(1, customerId);
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                String transactionnum = rs.getString("transaction_number");
                int amount = rs.getInt("amount");
                String type = rs.getString("type");
                String time = rs.getString("time");
                System.out.printf("Transaction id: %s, Amount: %d, Type: %s, Time: %s%n", transactionnum, amount, type, time);
            }
        } catch (SQLException e) {
            System.out.println("Error retrieving transaction history: " + e.getMessage());
        }
    }

    private static void transferFunds() throws SQLException {
        // Implement fund transfer logic here
    }

    private static void changeAdminPin() throws SQLException {
        // Implement admin PIN change logic here
    }

    private static void changeCustomerPin() throws SQLException {
        // Implement customer PIN change logic here
    }

    private static void clearScreen() {
        System.out.print("\033[H\033[2J");
        System.out.flush();
    }
}
