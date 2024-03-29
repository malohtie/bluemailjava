package smartmail.platform.orm;

import com.mchange.v2.c3p0.ComboPooledDataSource;

import java.beans.PropertyVetoException;
import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;

public class DataSource {
    private ComboPooledDataSource cpds;

    public DataSource(String driver, String url, String username, String password) throws IOException, SQLException, PropertyVetoException {
        this.cpds = new ComboPooledDataSource();
        this.cpds.setDriverClass(driver);
        this.cpds.setJdbcUrl(url);
        this.cpds.setUser(username);
        this.cpds.setPassword(password);
        this.cpds.setMinPoolSize(1);
        this.cpds.setAcquireIncrement(1);
        this.cpds.setMaxPoolSize(300);
        this.cpds.setMaxStatements(100);
    }

    public Connection getConnection() throws SQLException {
        return this.cpds.getConnection();
    }
}
