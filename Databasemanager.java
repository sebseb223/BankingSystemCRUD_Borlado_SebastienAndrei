package bankingsystemcrud;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class Databasemanager {
    private static Connection connection = null;

    public static Connection connect() {
        try {
            if (connection == null || connection.isClosed()) {
                Class.forName("com.mysql.cj.jdbc.Driver");
                String url = "jdbc:mysql://localhost:3306/BankingSystem";
                String user = "root";
                String password = "snakez223";
                connection = DriverManager.getConnection(url, user, password);
            }
        } catch (ClassNotFoundException | SQLException e) {
            e.printStackTrace();
        }
        return connection;
    }
}
