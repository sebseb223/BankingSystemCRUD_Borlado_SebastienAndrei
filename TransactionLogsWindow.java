package bankingsystemcrud;

import java.awt.*;
import java.awt.event.*;
import java.text.DecimalFormat;
import java.sql.*;
import javax.swing.*;
import javax.swing.table.*;

public class TransactionLogsWindow extends JFrame {

    private JTextField txtFilterAccount;
    private JComboBox<String> cmbFilterType;
    private JButton btnFilter, btnClear;
    private JTable tblLogs;

    public TransactionLogsWindow() {
        setTitle("Transaction Logs / History");
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setSize(750, 500);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());

        // ── Filter ────────────────────────────────────────────────────
        JPanel filterPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 10));

        txtFilterAccount = new JTextField(10);
        cmbFilterType    = new JComboBox<>(new String[]{"All", "Deposit", "Withdraw"});
        btnFilter        = new JButton("Filter");
        btnClear         = new JButton("Clear");

        filterPanel.add(new JLabel("Account ID:"));
        filterPanel.add(txtFilterAccount);
        filterPanel.add(new JLabel("Type:"));
        filterPanel.add(cmbFilterType);
        filterPanel.add(btnFilter);
        filterPanel.add(btnClear);

        // ── Table ─────────────────────────────────────────────────────
        tblLogs = new JTable();
        JScrollPane scroll = new JScrollPane(tblLogs);

        add(filterPanel, BorderLayout.NORTH);
        add(scroll,      BorderLayout.CENTER);

        // ── Events ────────────────────────────────────────────────────
        btnFilter.addActionListener(e -> applyFilter());
        btnClear.addActionListener(e  -> {
            txtFilterAccount.setText("");
            cmbFilterType.setSelectedIndex(0);
            loadAllLogs();
        });

        loadAllLogs();
        setVisible(true);
    }

    private void loadAllLogs() {
        try {
            ResultSet rs = Databasemanager.connect().prepareStatement(
                "SELECT t.transaction_id, t.account_id, " +
                "CONCAT(c.first_name,' ',c.last_name) AS customer_name, " +
                "t.transaction_type, t.amount, t.transaction_date " +
                "FROM Transaction t " +
                "JOIN Account a ON t.account_id = a.account_id " +
                "JOIN Customer c ON a.customer_id = c.customer_id " +
                "ORDER BY t.transaction_date DESC").executeQuery();
            fillTable(rs);
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Error: " + e.getMessage());
        }
    }

    private void applyFilter() {
        try {
            String accountFilter = txtFilterAccount.getText().trim();
            String typeFilter    = cmbFilterType.getSelectedItem().toString();

            StringBuilder sql = new StringBuilder(
                "SELECT t.transaction_id, t.account_id, " +
                "CONCAT(c.first_name,' ',c.last_name) AS customer_name, " +
                "t.transaction_type, t.amount, t.transaction_date " +
                "FROM Transaction t " +
                "JOIN Account a ON t.account_id = a.account_id " +
                "JOIN Customer c ON a.customer_id = c.customer_id WHERE 1=1");

            if (!accountFilter.isEmpty()) sql.append(" AND t.account_id = ?");
            if (!"All".equals(typeFilter))  sql.append(" AND t.transaction_type = ?");
            sql.append(" ORDER BY t.transaction_date DESC");

            PreparedStatement pst = Databasemanager.connect().prepareStatement(sql.toString());
            int i = 1;
            if (!accountFilter.isEmpty()) pst.setInt(i++, Integer.parseInt(accountFilter));
            if (!"All".equals(typeFilter))  pst.setString(i, typeFilter);

            fillTable(pst.executeQuery());
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this, "Account ID must be a number!");
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Error: " + e.getMessage());
        }
    }

    private void fillTable(ResultSet rs) throws Exception {
        DefaultTableModel model = new DefaultTableModel(
            new String[]{"Trans ID","Account ID","Customer","Type","Amount","Date"}, 0) {
            public boolean isCellEditable(int r, int c) { return false; }
        };
        tblLogs.setModel(model);
        DecimalFormat df = new DecimalFormat("#,##0.00");
        while (rs.next()) {
            model.addRow(new Object[]{
                rs.getInt("transaction_id"), rs.getInt("account_id"),
                rs.getString("customer_name"), rs.getString("transaction_type"),
                df.format(rs.getDouble("amount")), rs.getTimestamp("transaction_date")
            });
        }
    }
}
