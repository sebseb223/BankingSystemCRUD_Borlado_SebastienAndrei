package bankingsystemcrud;

import java.awt.*;
import java.awt.event.*;
import java.text.DecimalFormat;
import java.sql.*;
import javax.swing.*;
import javax.swing.table.*;

public class CustomerWindow extends JFrame {

    private int selectedAccountId  = -1;
    private int selectedCustomerId = -1;

    private JTextField txtFirstName, txtLastName, txtEmail, txtPhone, txtAccountType, txtBalance, txtSearch;
    private JButton btnAdd, btnUpdate, btnDelete, btnClear, btnSearch, btnOpenTransactions;
    private JTable tblAccounts;

    public CustomerWindow() {
        setTitle("Customer & Account Management");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(750, 550);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());

        // ── Form ──────────────────────────────────────────────────────
        JPanel formPanel = new JPanel(new GridLayout(7, 2, 5, 5));
        formPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        txtFirstName   = new JTextField();
        txtLastName    = new JTextField();
        txtEmail       = new JTextField();
        txtPhone       = new JTextField();
        txtAccountType = new JTextField();
        txtBalance     = new JTextField();
        txtSearch      = new JTextField();

        formPanel.add(new JLabel("First Name:"));   formPanel.add(txtFirstName);
        formPanel.add(new JLabel("Last Name:"));    formPanel.add(txtLastName);
        formPanel.add(new JLabel("Email:"));        formPanel.add(txtEmail);
        formPanel.add(new JLabel("Phone:"));        formPanel.add(txtPhone);
        formPanel.add(new JLabel("Account Type:")); formPanel.add(txtAccountType);
        formPanel.add(new JLabel("Balance:"));      formPanel.add(txtBalance);
        formPanel.add(new JLabel("Search:"));       formPanel.add(txtSearch);

        // ── Buttons ───────────────────────────────────────────────────
        JPanel btnPanel = new JPanel(new GridLayout(6, 1, 5, 5));
        btnPanel.setBorder(BorderFactory.createEmptyBorder(10, 5, 10, 10));

        btnAdd              = new JButton("Add");
        btnUpdate           = new JButton("Update");
        btnDelete           = new JButton("Delete");
        btnClear            = new JButton("Clear");
        btnSearch           = new JButton("Search");
        btnOpenTransactions = new JButton("Transactions");

        btnPanel.add(btnAdd);
        btnPanel.add(btnUpdate);
        btnPanel.add(btnDelete);
        btnPanel.add(btnClear);
        btnPanel.add(btnSearch);
        btnPanel.add(btnOpenTransactions);

        // ── Top area ──────────────────────────────────────────────────
        JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.add(formPanel, BorderLayout.CENTER);
        topPanel.add(btnPanel,  BorderLayout.EAST);

        // ── Table ─────────────────────────────────────────────────────
        tblAccounts = new JTable();
        JScrollPane scroll = new JScrollPane(tblAccounts);

        // ── Assemble ──────────────────────────────────────────────────
        add(topPanel, BorderLayout.NORTH);
        add(scroll,   BorderLayout.CENTER);

        // ── Events ────────────────────────────────────────────────────
        btnAdd.addActionListener(e              -> addAccount());
        btnUpdate.addActionListener(e           -> updateAccount());
        btnDelete.addActionListener(e           -> deleteAccount());
        btnClear.addActionListener(e            -> clearFields());
        btnSearch.addActionListener(e           -> searchAccount());
        btnOpenTransactions.addActionListener(e -> new TransactionWindow().setVisible(true));
        tblAccounts.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) { selectRow(); }
        });

        loadTable();
        setVisible(true);
    }

    private void addAccount() {
        try {
            if (txtFirstName.getText().trim().isEmpty() || txtLastName.getText().trim().isEmpty()) {
                JOptionPane.showMessageDialog(this, "First Name and Last Name are required!"); return;
            }
            Connection conn = Databasemanager.connect();

            PreparedStatement pstCust = conn.prepareStatement(
                "INSERT INTO Customer (first_name, last_name, email, phone_number) VALUES (?,?,?,?)",
                Statement.RETURN_GENERATED_KEYS);
            pstCust.setString(1, txtFirstName.getText().trim());
            pstCust.setString(2, txtLastName.getText().trim());
            pstCust.setString(3, txtEmail.getText().trim());
            pstCust.setString(4, txtPhone.getText().trim());
            pstCust.executeUpdate();

            ResultSet keys = pstCust.getGeneratedKeys();
            int custId = 0;
            if (keys.next()) custId = keys.getInt(1);

            PreparedStatement pstAcc = conn.prepareStatement(
                "INSERT INTO Account (customer_id, account_type, balance) VALUES (?,?,?)");
            pstAcc.setInt(1, custId);
            pstAcc.setString(2, txtAccountType.getText().trim());
            pstAcc.setDouble(3, Double.parseDouble(txtBalance.getText().trim()));
            pstAcc.executeUpdate();

            JOptionPane.showMessageDialog(this, "Account Added!");
            clearFields();
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this, "Balance must be a number!");
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Error: " + e.getMessage());
        }
    }

    private void updateAccount() {
        try {
            if (selectedAccountId == -1) {
                JOptionPane.showMessageDialog(this, "Select a row first!"); return;
            }
            Connection conn = Databasemanager.connect();

            PreparedStatement pstCust = conn.prepareStatement(
                "UPDATE Customer SET first_name=?, last_name=?, email=?, phone_number=? WHERE customer_id=?");
            pstCust.setString(1, txtFirstName.getText().trim());
            pstCust.setString(2, txtLastName.getText().trim());
            pstCust.setString(3, txtEmail.getText().trim());
            pstCust.setString(4, txtPhone.getText().trim());
            pstCust.setInt(5, selectedCustomerId);
            pstCust.executeUpdate();

            PreparedStatement pstAcc = conn.prepareStatement(
                "UPDATE Account SET account_type=?, balance=? WHERE account_id=?");
            pstAcc.setString(1, txtAccountType.getText().trim());
            pstAcc.setDouble(2, Double.parseDouble(txtBalance.getText().trim()));
            pstAcc.setInt(3, selectedAccountId);
            pstAcc.executeUpdate();

            JOptionPane.showMessageDialog(this, "Updated!");
            clearFields();
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Error: " + e.getMessage());
        }
    }

    private void deleteAccount() {
        try {
            if (selectedAccountId == -1) {
                JOptionPane.showMessageDialog(this, "Select a row first!"); return;
            }
            int c = JOptionPane.showConfirmDialog(this, "Delete this account?", "Confirm", JOptionPane.YES_NO_OPTION);
            if (c == JOptionPane.YES_OPTION) {
                PreparedStatement pst = Databasemanager.connect()
                    .prepareStatement("DELETE FROM Account WHERE account_id=?");
                pst.setInt(1, selectedAccountId);
                pst.executeUpdate();
                JOptionPane.showMessageDialog(this, "Deleted!");
                clearFields();
            }
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Error: " + e.getMessage());
        }
    }

    private void searchAccount() {
        try {
            String k = txtSearch.getText().trim();
            if (k.isEmpty()) { loadTable(); return; }
            PreparedStatement pst = Databasemanager.connect().prepareStatement(
                "SELECT c.customer_id, c.first_name, c.last_name, c.email, c.phone_number, " +
                "a.account_id, a.account_type, a.balance " +
                "FROM Customer c JOIN Account a ON c.customer_id = a.customer_id " +
                "WHERE CONCAT(c.first_name,' ',c.last_name) LIKE ? OR CAST(a.account_id AS CHAR) LIKE ?");
            pst.setString(1, "%" + k + "%");
            pst.setString(2, "%" + k + "%");
            fillTable(pst.executeQuery());
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Error: " + e.getMessage());
        }
    }

    private void selectRow() {
        int row = tblAccounts.getSelectedRow();
        if (row == -1) return;
        selectedAccountId  = Integer.parseInt(tblAccounts.getValueAt(row, 1).toString());
        selectedCustomerId = Integer.parseInt(tblAccounts.getValueAt(row, 8).toString());
        txtFirstName.setText(tblAccounts.getValueAt(row, 4).toString());
        txtLastName.setText(tblAccounts.getValueAt(row, 5).toString());
        txtEmail.setText(tblAccounts.getValueAt(row, 6) != null ? tblAccounts.getValueAt(row, 6).toString() : "");
        txtPhone.setText(tblAccounts.getValueAt(row, 7) != null ? tblAccounts.getValueAt(row, 7).toString() : "");
        txtAccountType.setText(tblAccounts.getValueAt(row, 2).toString());
        txtBalance.setText(tblAccounts.getValueAt(row, 3).toString().replace(",", ""));
    }

    private void clearFields() {
        txtFirstName.setText(""); txtLastName.setText(""); txtEmail.setText("");
        txtPhone.setText(""); txtAccountType.setText(""); txtBalance.setText("");
        txtSearch.setText("");
        selectedAccountId = -1; selectedCustomerId = -1;
        loadTable();
    }

    public void loadTable() {
        try {
            PreparedStatement pst = Databasemanager.connect().prepareStatement(
                "SELECT c.customer_id, c.first_name, c.last_name, c.email, c.phone_number, " +
                "a.account_id, a.account_type, a.balance " +
                "FROM Customer c JOIN Account a ON c.customer_id = a.customer_id");
            fillTable(pst.executeQuery());
        } catch (Exception e) {
            JOptionPane.showMessageDialog(null, "Error: " + e.getMessage());
        }
    }

    private void fillTable(ResultSet rs) throws Exception {
        String[] cols = {"Customer Name","Account ID","Account Type","Balance",
                         "First Name","Last Name","Email","Phone","Customer ID"};
        DefaultTableModel model = new DefaultTableModel(cols, 0) {
            public boolean isCellEditable(int r, int c) { return false; }
        };
        tblAccounts.setModel(model);
        for (int i = 4; i <= 8; i++) {
            tblAccounts.getColumnModel().getColumn(i).setMinWidth(0);
            tblAccounts.getColumnModel().getColumn(i).setMaxWidth(0);
        }
        DecimalFormat df = new DecimalFormat("#,##0.00");
        while (rs.next()) {
            model.addRow(new Object[]{
                rs.getString("first_name") + " " + rs.getString("last_name"),
                rs.getInt("account_id"), rs.getString("account_type"),
                df.format(rs.getDouble("balance")),
                rs.getString("first_name"), rs.getString("last_name"),
                rs.getString("email"), rs.getString("phone_number"),
                rs.getInt("customer_id")
            });
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new CustomerWindow());
    }
}
