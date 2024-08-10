Automated Teller Machine (ATM) System
=====================================

This project is a Java-based Automated Teller Machine (ATM) system that allows users to interact with a simulated bank ATM. The system supports basic ATM functionalities such as account login, balance checking, depositing, withdrawing, transferring funds, and transaction history.

Features
--------

*   **Admin and Customer Access:**
    
    *   Admins can view ATM balance, transaction history, and perform other management tasks.
        
    *   Customers can check account balances, deposit, withdraw funds, and view transaction histories.
        
*   **PIN Encryption and Decryption:**
    
    *   Secure PIN management using AES encryption to ensure customer security.
        
*   **Transaction Logging:**
    
    *   Transaction history is logged and can be retrieved for review.
        
*   **Multi-Bank Support:**
    
    *   The system supports transactions for customers from different banks with relevant service charges applied.
        

Prerequisites
-------------

*   Java Development Kit (JDK) 8 or above
    
*   SQLite JDBC Driver
    
*   A Java IDE or a text editor with terminal access for running the application
    

Installation
------------

    git clone https://github.com/PRIYADARSHAN-MAHENDRAN/Automated-Teller-Machine.git
    
    cd Automated-Teller-Machine-main

    javac -cp .;sqlite-jdbc-3.36.0.3.jar AutomatedTellerMachine.java
    
    java -cp .;sqlite-jdbc-3.36.0.3.jar AutomatedTellerMachine



Usage
-----

1.  **Login:**
    
    *   Insert your card by entering your customer ID and PIN. Admins can log in with their credentials as well.
        
    *   New users can set their PIN by selecting the "Forgot PIN / Set PIN" option.
        
2.  **Admin Operations:**
    
    *   View the ATM balance, check transaction history, and manage the system.
        
3.  **Customer Operations:**
    
    *   View account details, check balance, deposit, withdraw, transfer funds, and view a mini statement of transactions.
        

Database
--------

The system uses an SQLite database (atm.db) to store user information, transaction logs, and ATM balance.

**Tables:**

*   machine: Stores ATM balance and time of last update.
    
*   user: Stores customer information including customer ID, PIN (encrypted), account type, bank name, account balance, and wallet balance.
    
*   transactionlog: Logs all transactions including deposits, withdrawals, and transfers.
    

Security
--------

*   The system uses AES encryption to securely store and manage customer PINs.
    
*   A secret key is generated and used for encrypting and decrypting PINs.
    

Contributing
------------

If you'd like to contribute to this project, please fork the repository and use a feature branch. Pull requests are warmly welcome.

License
-------

This project is licensed under the MIT License - see the LICENSE file for details.
