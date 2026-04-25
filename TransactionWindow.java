package bankingsystemcrud;

import java.awt.*;
import java.awt.event.*;
import java.text.DecimalFormat;
import java.sql.*;
import javax.swing.*;
import javax.swing.table.*;

public class TransactionWindow extends JFrame {

    private JComboBox<String> cmbAccount;
    private JTextField txtCurrentBal, txtAmount;
    private JButton btnDeposit, btnWithdraw, btnRefresh, btnViewLogs;
    private JTable tblTransactions;

    public TransactionWindow() {
        setTitle("Transaction Management");
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setSize(700, 500);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());

        // ── Form ──────────────────────────────────────────────────────
        JPanel formPanel = new JPanel(new GridLayout(3, 2, 5, 5));
        formPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        cmbAccount    = new JComboBox<>();
        txtCurrentBal = new JTextField();
        txtCurrentBal.setEditable(false);
        txtAmount     = new JTextField();

        formPanel.add(new JLabel("Select Account:")); formPanel.add(cmbAccount);
        formPanel.add(new JLabel("Current Balance:")); formPanel.add(txtCurrentBal);
        formPanel.add(new JLabel("Amount:"));         formPanel.add(txtAmount);

        // ── Buttons ───────────────────────────────────────────────────
        JPanel btnPanel = new JPanel(new GridLayout(4, 1, 5, 5));
        btnPanel.setBorder(BorderFactory.createEmptyBorder(10, 5, 10, 10));

        btnDeposit  = new JButton("Deposit");
        btnWithdraw = new JButton("Withdraw");
        btnRefresh  = new JButton("Refresh");
        btnViewLogs = new JButton("View Logs");

        btnPanel.add(btnDeposit);
        btnPanel.add(btnWithdraw);
        btnPanel.add(btnRefresh);
        btnPanel.add(btnViewLogs);

        JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.add(formPanel, BorderLayout.CENTER);
        topPanel.add(btnPanel,  BorderLayout.EAST);

        // ── Table ─────────────────────────────────────────────────────
        tblTransactions = new JTable();
        JScrollPane scroll = new JScrollPane(tblTransactions);

        add(topPanel, BorderLayout.NORTH);
        add(scroll,   BorderLayout.CENTER);

        // ── Events ────────────────────────────────────────────────────
        cmbAccount.addActionListener(e  -> showBalance());
        btnDeposit.addActionListener(e  -> doTransaction("Deposit"));
        btnWithdraw.addActionListener(e -> doTransaction("Withdraw"));
        btnRefresh.addActionListener(e  -> { loadAccounts(); loadTable(); });
        btnViewLogs.addActionListener(e -> new TransactionLogsWindow().setVisible(true));

        loadAccounts();
        loadTable();
        setVisible(true);
    }

    private void loadAccounts() {
        try {
            cmbAccount.removeAllItems();
            ResultSet rs = Databasemanager.connect().prepareStatement(
                "SELECT a.account_id, c.first_name, c.last_name, a.account_type " +
                "FROM Account a JOIN Customer c ON a.customer_id = c.customer_id")
                .executeQuery();
            while (rs.next()) {
                cmbAccount.addItem(rs.getInt("account_id") + " - " +
                    rs.getString("first_name") + " " + rs.getString("last_name") +
                    " (" + rs.getString("account_type") + ")");
            }
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Error: " + e.getMessage());
        }
    }

    private void showBalance() {
        try {
            if (cmbAccount.getSelectedItem() == null) return;
            PreparedStatement pst = Databasemanager.connect()
                .prepareStatement("SELECT balance FROM Account WHERE account_id=?");
            pst.setInt(1, getAccountId());
            ResultSet rs = pst.executeQuery();
            if (rs.next())
                txtCurrentBal.setText(new DecimalFormat("#,##0.00").format(rs.getDouble("balance")));
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Error: " + e.getMessage());
        }
    }

    private void doTransaction(String type) {
        try {
            if (cmbAccount.getSelectedItem() == null) {
                JOptionPane.showMessageDialog(this, "Select an account!"); return;
            }
            if (txtAmount.getText().trim().isEmpty()) {
                JOptionPane.showMessageDialog(this, "Enter an amount!"); return;
            }
            double amount = Double.parseDouble(txtAmount.getText().trim());
            if (amount <= 0) {
                JOptionPane.showMessageDialog(this, "Amount must be greater than zero!"); return;
            }

            int accountId = getAccountId();
            Connection conn = Databasemanager.connect();

            PreparedStatement pstBal = conn.prepareStatement("SELECT balance FROM Account WHERE account_id=?");
            pstBal.setInt(1, accountId);
            ResultSet rs = pstBal.executeQuery();
            double balance = 0;
            if (rs.next()) balance = rs.getDouble("balance");

            if (type.equals("Withdraw") && amount > balance) {
                JOptionPane.showMessageDialog(this, "Insufficient balance!"); return;
            }

            double newBalance = type.equals("Deposit") ? balance + amount : balance - amount;

            PreparedStatement pstUpd = conn.prepareStatement("UPDATE Account SET balance=? WHERE account_id=?");
            pstUpd.setDouble(1, newBalance); pstUpd.setInt(2, accountId);
            pstUpd.executeUpdate();

            PreparedStatement pstTrans = conn.prepareStatement(
                "INSERT INTO Transaction (account_id, transaction_type, amount) VALUES (?,?,?)");
            pstTrans.setInt(1, accountId); pstTrans.setString(2, type); pstTrans.setDouble(3, amount);
            pstTrans.executeUpdate();

            JOptionPane.showMessageDialog(this, type + " successful! New balance: " +
                new DecimalFormat("#,##0.00").format(newBalance));
            txtAmount.setText("");
            showBalance();
            loadTable();
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this, "Enter a valid number!");
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Error: " + e.getMessage());
        }
    }

    private void loadTable() {
        try {
            ResultSet rs = Databasemanager.connect().prepareStatement(
                "SELECT t.transaction_id, t.account_id, " +
                "CONCAT(c.first_name,' ',c.last_name) AS customer_name, " +
                "t.transaction_type, t.amount, t.transaction_date " +
                "FROM Transaction t " +
                "JOIN Account a ON t.account_id = a.account_id " +
                "JOIN Customer c ON a.customer_id = c.customer_id " +
                "ORDER BY t.transaction_date DESC").executeQuery();

            DefaultTableModel model = new DefaultTableModel(
                new String[]{"Trans ID","Account ID","Customer","Type","Amount","Date"}, 0) {
                public boolean isCellEditable(int r, int c) { return false; }
            };
            tblTransactions.setModel(model);
            DecimalFormat df = new DecimalFormat("#,##0.00");
            while (rs.next()) {
                model.addRow(new Object[]{
                    rs.getInt("transaction_id"), rs.getInt("account_id"),
                    rs.getString("customer_name"), rs.getString("transaction_type"),
                    df.format(rs.getDouble("amount")), rs.getTimestamp("transaction_date")
                });
            }
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Error: " + e.getMessage());
        }
    }

    private int getAccountId() {
        return Integer.parseInt(cmbAccount.getSelectedItem().toString().split(" - ")[0].trim());
    }
}
