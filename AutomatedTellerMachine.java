
import java.sql.*;
import java.util.Base64;
import java.util.Random;
import java.util.Scanner;
import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

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
                while (true) {
                    System.out.println("```` KDFC ATM ````");
                    System.out.println();
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
                                changePin();
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
                            changePin();
                            break;
                        case 0:
                            System.out.println("Get your ATM card.");
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
            "CREATE TABLE IF NOT EXISTS user (customerid INTEGER NOT NULL UNIQUE, accounttype TEXT NOT NULL, bankname TEXT NOT NULL, useraccountbalance INTEGER NOT NULL, `userwalletbalance` INTEGER NOT NULL DEFAULT 0, `name` TEXT, `pin` TEXT, `secretKey` TEXT)",
            "CREATE TABLE IF NOT EXISTS transactionlog (transaction_number TEXT UNIQUE ,customerid INTEGER NOT NULL, amount INTEGER NOT NULL, type TEXT NOT NULL, time TEXT NOT NULL,FOREIGN KEY(customerid) REFERENCES user(customerid))"
        };

        try (Statement stmt = connection.createStatement()) {
            for (String sql : sqlQuery) {
                stmt.execute(sql);
            }
        }
    }

    private static String login() throws SQLException, Exception {
        int choice;

        while (true) {
            clearScreen();
            System.out.println("```` Welcome to KDFC ATM ````");
            System.out.println();
            System.out.println("1. Insert Card");
            System.out.println("2. Forgot PIN / Set PIN");
            System.out.println("0. Exit");
            System.out.println();
            System.out.print("Enter your choice: ");
            choice = scanner.nextInt();
            scanner.nextLine();

            if (choice == 0) {
                System.out.println();
                System.out.println("~~~~ Get your ATM card. ~~~~");
                System.out.println();
                System.exit(0);
            } else if (choice == 1) {
                clearScreen();
                System.out.print("Enter ID num: ");
                int id = scanner.nextInt();
                scanner.nextLine();  // Consume newline

                System.out.print("Enter PIN: ");
                int pin = scanner.nextInt();
                scanner.nextLine();

                String accountType = authenticate(id, pin);
                if (accountType != null) {
                    customerId = id;
                    System.out.println();
                    System.out.println("Successfully login");
                    waitForEnter();
                    return accountType;
                } else {

                }
            } else if (choice == 2) {
                clearScreen();
                System.out.print("Enter ID num: ");
                int id = scanner.nextInt();
                scanner.nextLine();  // Consume newline

                System.out.print("Enter name: ");
                String name = scanner.nextLine();
                setPIN(id, name);

            } else {
                System.out.println();
                System.out.println("Please enter a valid choice number (0 or 1) only.");
                waitForEnter();

            }
        }
    }

    private static String authenticate(int id, int enteredPin) throws SQLException {
        String sql = "SELECT accounttype, bankname, pin, secretKey FROM user WHERE customerid = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setInt(1, id);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                String encryptedPin = rs.getString("pin");
                String encodedKey = rs.getString("secretKey");
                int decryptedPin = decrypt(encryptedPin, encodedKey);

                if (decryptedPin == enteredPin) {
                    bankname = rs.getString("bankname");
                    return rs.getString("accounttype");
                } else {
                    System.out.println();
                    System.out.println("Invalid PIN. Please try again.");
                    waitForEnter();
                    return null;
                }
            } else {
                System.out.println();
                System.out.println("Invalid ID or PIN. Please try again.");
                waitForEnter();
                return null;
            }
        } catch (SQLException e) {
            System.out.println("Error during authentication: " + e.getMessage());
            return null;
        } catch (Exception e) {
            System.out.println();
            System.out.println("Please set PIN.");
            waitForEnter();
            return null;
        }
    }

    private static void setPIN(int Id, String name) throws SQLException, Exception {
        String sql = "SELECT * FROM user WHERE customerid = ? AND name = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setInt(1, Id);
            pstmt.setString(2, name);
            ResultSet rs = pstmt.executeQuery();
            if (!rs.next()) {
                System.out.println("Invalid ID or Name. Please try again.");
                waitForEnter();
                return;
            }
        } catch (SQLException e) {
            System.out.println("Error during authentication: " + e.getMessage());
            return;
        }

        System.out.print("Enter New PIN (4 digits): ");
        int newPIN = scanner.nextInt();
        scanner.nextLine(); // Consume newline

        System.out.print("Confirm New PIN (4 digits): ");
        int confirmPIN = scanner.nextInt();
        scanner.nextLine(); // Consume newline

        if (newPIN != confirmPIN) {
            System.out.println("New PIN and confirmation do not match. Please try again.");
            waitForEnter();
            return;
        }
        if (newPIN < 1000 || newPIN > 9999) {
            System.out.println("New PIN must be 4 digits.");
            waitForEnter();
            return;
        }

        // Generate and encrypt the PIN
        SecretKey secretKey = generateKey();
        String encryptedPin = encrypt(newPIN, secretKey);

        // Convert SecretKey to Base64 for storage
        String encodedKey = Base64.getEncoder().encodeToString(secretKey.getEncoded());

        sql = "UPDATE user SET pin = ?, secretKey = ? WHERE customerid = ? AND name = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, encryptedPin);
            pstmt.setString(2, encodedKey);
            pstmt.setInt(3, Id);
            pstmt.setString(4, name);

            int rowsUpdated = pstmt.executeUpdate();
            if (rowsUpdated > 0) {
                System.out.println("PIN successfully set.");
                waitForEnter();
                return;
            } else {
                System.out.println("Error updating PIN. Please try again.");
                waitForEnter();
                return;
            }
        } catch (SQLException e) {
            System.out.println("Error changing PIN: " + e.getMessage());
        }
    }

// Generate a secret key for encryption
    private static SecretKey generateKey() throws Exception {
        KeyGenerator keyGenerator = KeyGenerator.getInstance("AES");
        keyGenerator.init(128); // AES-128 bit key
        return keyGenerator.generateKey();
    }

// Encrypt the PIN
    public static String encrypt(int pin, SecretKey secretKey) throws Exception {
        Cipher cipher = Cipher.getInstance("AES");
        cipher.init(Cipher.ENCRYPT_MODE, secretKey);
        byte[] encryptedBytes = cipher.doFinal(String.valueOf(pin).getBytes());
        return Base64.getEncoder().encodeToString(encryptedBytes);
    }

// Decrypt the PIN
    public static int decrypt(String encryptedPin, String encodedKey) throws Exception {
        // Decode the stored key
        byte[] decodedKey = Base64.getDecoder().decode(encodedKey);
        SecretKey originalKey = new SecretKeySpec(decodedKey, 0, decodedKey.length, "AES");

        Cipher cipher = Cipher.getInstance("AES");
        cipher.init(Cipher.DECRYPT_MODE, originalKey);
        byte[] decodedBytes = Base64.getDecoder().decode(encryptedPin);
        byte[] decryptedBytes = cipher.doFinal(decodedBytes);
        return Integer.parseInt(new String(decryptedBytes));
    }

    private static void adminOptions() {
        System.out.println("1. ATM Balance");
        System.out.println("2. Transaction History");
        System.out.println("3. Deposit");
        System.out.println("4. Change PIN");
        System.out.println("0. Logout");
        System.out.println();
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
        System.out.println();
        System.out.print("Choose an option: ");
    }

    private static void viewAtmBalance() throws SQLException {
        clearScreen();
        System.out.print("~~~~ ATM BALANCE ~~~~");
        System.out.println();
        System.out.println();
        String sql = "SELECT * FROM machine";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                System.out.println("Amount :" + rs.getInt("atmbalance") + "  time :" + rs.getString("time"));
            } else {
                System.out.println("No ATM balance found.");
            }
        } catch (SQLException e) {
            System.out.println("Error retrieving ATM balance: " + e.getMessage());
        }
        waitForEnter();
        clearScreen();
    }

    private static void viewTransactionHistory() throws SQLException {
        clearScreen();
        String sql = "SELECT * FROM transactionlog ORDER BY time DESC LIMIT 10";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            ResultSet rs = pstmt.executeQuery();

            System.out.println("~~~~ Last 10 Transactions ~~~~");
            System.out.println();
            System.out.printf("%-20s %-10s %-10s %-30s\n", "Transaction Number", "Amount", "Type", "Time");
            System.out.println("----------------------------------------------------------------------");

            while (rs.next()) {
                String transactionNumber = rs.getString("transaction_number");
                int amount = rs.getInt("amount");
                String type = rs.getString("type");
                String time = rs.getString("time");

                System.out.printf("%-20s %-10d %-10s %-30s\n", transactionNumber, amount, type, time);
            }
        } catch (SQLException e) {
            System.out.println("Error retrieving transactions: " + e.getMessage());
        }
        waitForEnter();
        clearScreen();
    }

    private static void viewMyAccount() throws SQLException {
        clearScreen();
        System.out.println("~~~~ My account ~~~~");
        System.out.println();
        System.out.printf("ID: %d%nBank: %s%nAccount Balance: %d%nWallet Balance: %d%n", customerId, getCustomerBankName(customerId), getAccountBalance(customerId), getWalletBalance(customerId));
        waitForEnter();
        clearScreen();
        return;

    }

    private static void viewMyBalance() throws SQLException {

        clearScreen();
        System.out.println("~~~~ Balance ~~~~");
        System.out.println();
        System.out.printf("Account Balance: %d%nWallet Balance: %d%n", getAccountBalance(customerId), getWalletBalance(customerId));
        waitForEnter();
        clearScreen();
        return;

    }

    private static void deposit() throws SQLException {
        clearScreen();
        int walletBalance = getWalletBalance(customerId); // Get current wallet balance
        System.out.println("~~~~ Deposit ~~~~");
        System.out.println();
        System.out.println("(#Important note 5% service charge if other bank customer)");
        System.out.println();
        System.out.print("Enter amount to deposit: ");
        int amount = scanner.nextInt();
        scanner.nextLine(); // Clear invalid input
        System.out.println();

        if (amount > walletBalance) {
            System.out.println("Insufficient wallet balance. Available balance: " + walletBalance);
            waitForEnter();
            clearScreen();
            return;
        }
        int depositAmount = !bankname.equalsIgnoreCase("kdfc") ? amount - ((amount / 100) * 5) : amount;

        // Begin transaction to ensure atomicity
        connection.setAutoCommit(false);
        try {
            // Update user's account balance and wallet balance
            if (accountType.equals("C")) {
                if (!updateAccountBalance(customerId, depositAmount)) {
                    System.out.println("Failed to debit amount from account. Transaction aborted.");
                    connection.rollback();
                    waitForEnter();
                    clearScreen();
                    return;
                }
                if (!updateWalletBalance(customerId, -amount)) {
                    System.out.println("Failed to credit amount to wallet. Transaction aborted.");
                    connection.rollback();
                    waitForEnter();
                    clearScreen();
                    return;
                }
            } else {
                if (!updateWalletBalance(customerId, -amount)) {
                    System.out.println("Failed to credit amount to wallet. Transaction aborted.");
                    connection.rollback();
                    waitForEnter();
                    clearScreen();
                    return;
                }
            }
            // Update ATM balance
            if (!updateAtmBalance(amount)) {
                System.out.println("Failed to debit amount from atm. Transaction aborted.");
                connection.rollback();
                waitForEnter();
                clearScreen();
                return;
            }

            // Generate a unique 16-digit transaction number
            String transactionNumber = generateUniqueTransactionNumber();

            if (!logTransaction(transactionNumber, customerId, amount, "DEPOSIT")) {
                System.out.println("Failed to log transaction. Transaction aborted.");
                connection.rollback();
                waitForEnter();
                clearScreen();
                return;
            }
            // Commit transaction if all operations succeed
            connection.commit();
            System.out.println("Deposit successful. Your new wallet balance is: " + getWalletBalance(customerId));

        } catch (SQLException e) {
            System.out.println("Error during deposit: " + e.getMessage());
            connection.rollback(); // Roll back transaction in case of error
        } finally {
            connection.setAutoCommit(true); // Restore auto-commit mode
        }
        waitForEnter();
        clearScreen();
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

    private static int getWalletBalance(int Id) {
        String sql = "SELECT userwalletbalance FROM user WHERE customerid = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setInt(1, Id);
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
        clearScreen();
        int accountBalance = getAccountBalance(customerId); // Get current account balance
        System.out.println("~~~~ Withdraw ~~~~");
        System.out.println();
        System.out.println("(#Important note 5% service charge if other bank customer)");
        System.out.println();
        System.out.print("Enter amount to withdraw :");
        int amount = scanner.nextInt();
        scanner.nextLine(); // Clear invalid input
        System.out.println();
        if (amount > accountBalance) {
            System.out.println("Insufficient account balance. Available balance: " + accountBalance);
            waitForEnter();
            clearScreen();
            return;
        }

        if (getATMBalance() < amount) {
            System.out.println("Insufficient ATM balance. Please comeback after some time.");
            waitForEnter();
            clearScreen();
            return;
        }

        int withdrawAmount = !bankname.equalsIgnoreCase("kdfc") ? amount + ((amount / 100) * 5) : amount;

        // Begin transaction to ensure atomicity
        connection.setAutoCommit(false);
        try {
            // Update user's account balance and wallet balance

            if (!updateAccountBalance(customerId, -withdrawAmount)) {
                System.out.println("Failed to debit amount from account. Transaction aborted.");
                connection.rollback();
                waitForEnter();
                clearScreen();
                return;
            }
            if (!updateWalletBalance(customerId, amount)) {
                System.out.println("Failed to credit amount to wallet. Transaction aborted.");
                connection.rollback();
                waitForEnter();
                clearScreen();
                return;
            }

            // Update ATM balance
            if (!updateAtmBalance(-amount)) {
                System.out.println("Failed to debit amount from atm. Transaction aborted.");
                connection.rollback();
                waitForEnter();
                clearScreen();
                return;
            }

            // Generate a unique 16-digit transaction number
            String transactionNumber = generateUniqueTransactionNumber();

            if (!logTransaction(transactionNumber, customerId, amount, "WITHDRAW")) {
                System.out.println("Failed to log transaction. Transaction aborted.");
                connection.rollback();
                waitForEnter();
                clearScreen();
                return;
            }

            // Commit transaction if all operations succeed
            connection.commit();
            System.out.println("Withdraw successful. Your new account balance is: " + getAccountBalance(customerId));

        } catch (SQLException e) {
            System.out.println("Error during withdraw: " + e.getMessage());
            connection.rollback(); // Roll back transaction in case of error
        } finally {
            connection.setAutoCommit(true); // Restore auto-commit mode
        }
        waitForEnter();
        clearScreen();
    }

    private static void viewMiniStatement() throws SQLException {
        clearScreen();
        System.out.println("~~~~ Mini Statement ~~~~");
        System.out.println();
        String sql = "SELECT * FROM transactionlog WHERE customerid = ? ORDER BY time DESC LIMIT 10";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setInt(1, customerId);
            ResultSet rs = pstmt.executeQuery();
            System.out.printf("%-20s %-10s %-10s %-30s\n", "Transaction Number", "Amount", "Type", "Time");
            System.out.println("----------------------------------------------------------------------");

            while (rs.next()) {
                String transactionNumber = rs.getString("transaction_number");
                int amount = rs.getInt("amount");
                String type = rs.getString("type");
                String time = rs.getString("time");

                System.out.printf("%-20s %-10d %-10s %-30s\n", transactionNumber, amount, type, time);
            }
        } catch (SQLException e) {
            System.out.println("Error retrieving transaction history: " + e.getMessage());
        }
        waitForEnter();
        clearScreen();
    }

    private static void transferFunds() throws SQLException {
        clearScreen();
        System.out.println("~~~~ Fund Transfer ~~~~");
        System.out.println();
        System.out.println("(#Important note 10% service charge if other bank customer)");
        System.out.println();
        // Prompt for the customer ID to transfer funds to
        System.out.print("Enter Customer ID to transfer to: ");
        int transferId = scanner.nextInt();
        scanner.nextLine();
        System.out.println();

        // Check if the transfer ID exists in the database
        if (!isValidCustomer(transferId)) {
            System.out.println("Entered customer ID is incorrect. Please try again.");
            waitForEnter();
            clearScreen();
            return;
        }

        // Prompt for the amount to transfer
        System.out.print("Enter amount to transfer: ");
        int transferAmount = scanner.nextInt();
        scanner.nextLine();
        System.out.println();
        // Get current account balance and bank name
        int accountBalance = getAccountBalance(customerId);
        String customerBank = getCustomerBankName(customerId);

        // Check if the account has sufficient balance
        if (accountBalance < transferAmount) {
            System.out.println("Insufficient account balance. Available balance: " + accountBalance);
            waitForEnter();
            clearScreen();
            return;
        }

        // Calculate amount to be credited, applying transaction fee if applicable
        int amountTodebit = customerBank.equals("kdfc") ? transferAmount : transferAmount + (transferAmount / 10); // 10% fee

        // Begin transaction to ensure atomicity
        connection.setAutoCommit(false);
        try {
            // Debit amount from sender's account
            if (!updateAccountBalance(customerId, -amountTodebit)) {
                System.out.println("Failed to debit amount. Transaction aborted.");
                connection.rollback();
                waitForEnter();
                clearScreen();
                return;
            }

            // Credit amount to recipient's account
            if (!updateAccountBalance(transferId, transferAmount)) {
                System.out.println("Failed to credit amount. Transaction aborted.");
                connection.rollback();
                waitForEnter();
                clearScreen();
                return;
            }

            // Log transaction with a unique transaction number
            String transactionNumber = generateUniqueTransactionNumber();

            if (!logTransaction(transactionNumber, customerId, transferAmount, "TRANSFER")) {
                System.out.println("Failed to log transaction. Transaction aborted.");
                connection.rollback();
                waitForEnter();
                clearScreen();
                return;
            }

            // Commit transaction if all operations succeed
            connection.commit();
            System.out.println("Transfer successful!");

        } catch (SQLException e) {
            System.out.println("Error during fund transfer: " + e.getMessage());
            connection.rollback(); // Roll back transaction in case of error
        } finally {
            connection.setAutoCommit(true); // Restore auto-commit mode
        }
        waitForEnter();
        clearScreen();
    }

    private static boolean isValidCustomer(int id) throws SQLException {
        String sql = "SELECT COUNT(*) FROM user WHERE customerid = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setInt(1, id);
            ResultSet rs = pstmt.executeQuery();
            return rs.next() && rs.getInt(1) > 0;
        }
    }

    private static int getAccountBalance(int id) throws SQLException {
        String sql = "SELECT useraccountbalance FROM user WHERE customerid = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setInt(1, id);
            ResultSet rs = pstmt.executeQuery();
            return rs.next() ? rs.getInt("useraccountbalance") : 0;
        }
    }

    private static int getATMBalance() throws SQLException {
        String sql = "SELECT atmbalance FROM machine";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            ResultSet rs = pstmt.executeQuery();
            return rs.next() ? rs.getInt("atmbalance") : 0;
        }
    }

    private static String getCustomerBankName(int id) throws SQLException {
        String sql = "SELECT bankname FROM user WHERE customerid = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setInt(1, id);
            ResultSet rs = pstmt.executeQuery();
            return rs.next() ? rs.getString("bankname") : "";
        }
    }

    private static boolean updateAccountBalance(int id, int amount) throws SQLException {
        String sql = "UPDATE user SET useraccountbalance = useraccountbalance + ? WHERE customerid = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setInt(1, amount);
            pstmt.setInt(2, id);
            return pstmt.executeUpdate() > 0;
        }
    }

    private static boolean updateWalletBalance(int id, int amount) throws SQLException {
        String sql = "UPDATE user SET userwalletbalance = userwalletbalance + ? WHERE customerid = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setInt(1, amount);
            pstmt.setInt(2, id);
            return pstmt.executeUpdate() > 0;
        }
    }

    private static boolean updateAtmBalance(int amount) throws SQLException {
        String sql = "UPDATE machine SET time = datetime('now'), atmbalance = atmbalance + ? WHERE time IS NOT NULL";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setInt(1, amount);
            return pstmt.executeUpdate() > 0;
        }
    }

    private static boolean logTransaction(String transactionNumber, int id, int amount, String type) throws SQLException {
        String sql = "INSERT INTO transactionlog (transaction_number, customerid, amount, type, time) VALUES (?, ?, ?, ?, datetime('now'))";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, transactionNumber);
            pstmt.setInt(2, id);
            pstmt.setInt(3, amount);
            pstmt.setString(4, type);
            return pstmt.executeUpdate() > 0;
        }
    }

    private static void changePin() throws SQLException {
        clearScreen();
        System.out.println("~~~~ Change PIN ~~~~");
        System.out.println();

        System.out.print("Enter Current PIN: ");
        int currentPIN = scanner.nextInt();
        scanner.nextLine();
        System.out.println();

        System.out.print("Enter New PIN (4 digits): ");
        int newPIN = scanner.nextInt();
        scanner.nextLine();
        System.out.println();

        System.out.print("Enter Confirm New PIN (4 digits): ");
        int confirmPIN = scanner.nextInt();
        scanner.nextLine();
        System.out.println();

        if (newPIN != confirmPIN) {
            System.out.println("New PIN and confirmation do not match. Please try again.");
            waitForEnter();
            clearScreen();
            return;
        }

        if (newPIN < 1000 || newPIN > 9999) {
            System.out.println("New PIN must be 4 digits.");
            waitForEnter();
            clearScreen();
            return;
        }

        String sql = "SELECT pin, secretKey FROM user WHERE customerid = ?";
        String encryptedPin;
        String secretKeyString;

        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setInt(1, customerId);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                encryptedPin = rs.getString("pin");
                secretKeyString = rs.getString("secretKey");
            } else {
                System.out.println("PIN not found.");
                waitForEnter();
                clearScreen();
                return;
            }
        } catch (SQLException e) {
            System.out.println("Error retrieving PIN: " + e.getMessage());
            return;
        }

        try {
            int decryptedPin = decrypt(encryptedPin, secretKeyString);
            if (decryptedPin != currentPIN) {
                System.out.println("Entered Current PIN is incorrect.");
                waitForEnter();
                clearScreen();
                return;
            }
        } catch (Exception e) {
            System.out.println("Error decrypting PIN: " + e.getMessage());
            return;
        }

        try {
            SecretKey newSecretKey = generateKey();
            String newEncryptedPin = encrypt(newPIN, newSecretKey);
            sql = "UPDATE user SET pin = ?, secretKey = ? WHERE customerid = ?";

            try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
                pstmt.setString(1, newEncryptedPin);
                pstmt.setString(2, Base64.getEncoder().encodeToString(newSecretKey.getEncoded()));
                pstmt.setInt(3, customerId);
                int rowsUpdated = pstmt.executeUpdate();
                if (rowsUpdated > 0) {
                    System.out.println("PIN changed successfully.");
                } else {
                    System.out.println("Failed to update PIN. Please try again.");
                    waitForEnter();
                    clearScreen();
                    return;
                }
            }
        } catch (Exception e) {
            System.out.println("Error encrypting new PIN: " + e.getMessage());
        }

        waitForEnter();
        clearScreen();
    }

    private static void clearScreen() {
        System.out.print("\033[H\033[2J");
        System.out.flush();

    }

    private static void waitForEnter() {
        System.out.println();
        System.out.println("Press Enter to continue...");
        scanner.nextLine(); // Wait for the user to press Enter
    }
}
