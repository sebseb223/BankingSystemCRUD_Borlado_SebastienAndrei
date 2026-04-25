# Banking Account Management System

## 1. System Description
The **Banking Account Management System** is a Java desktop application designed to manage customer profiles and financial accounts. Built with **Java Swing**, it provides a streamlined interface for banking operations.

### Core Features:
* **Add Account:** Register new customers and link them to accounts.
* **View Accounts:** Real-time data display with formatted balances.
* **Update Account:** Modify existing account types and funds.
* **Delete Account:** Remove redundant or old records.

---

## 2. ERD Explanation
The system uses a relational structure with two primary tables:

### Table: `Customer`
Stores identity information.
* `customer_id` (PK): Unique identifier for each client.
* `first_name` & `last_name`: Client's legal name.

### Table: `Account`
Stores financial data linked to a customer.
* `account_id` (PK): Unique identifier for the account.
* `customer_id` (FK): Links the account back to the owner.
* `account_type`: Categories like Savings or Current.
* `balance`: Numerical value of the account funds.

**Relationship:** **One-to-Many**. A single customer can own multiple accounts, but each account is assigned to only one customer.



---

## 3. How to Run the Program

### Prerequisites:
1. **Java Development Kit (JDK)** installed.
2. **NetBeans IDE** (or preferred Java IDE).
3. **MySQL JDBC Driver** included in the project libraries.

### Steps:
1. **Prepare Database:** Ensure your SQL server is active and the required tables are created.
2. **Project Setup:** Open the project in your IDE. Verify that the database connection string in `Databasemanager.java` matches your local server credentials.
3. **Execution:** Locate the main UI class, right-click, and select **Run File** (Shift + F6).
