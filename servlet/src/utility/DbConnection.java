package utility;

import org.apache.tomcat.dbcp.dbcp2.BasicDataSource;

import java.beans.PropertyVetoException;
import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;


public class DbConnection {

	public static String dbDriver = "com.mysql.jdbc.Driver";
    	public static String dbAddress = "jdbc:mysql://localhost:8889/route";
	public static String dbUsername = "root";
	public static String dbPassword = "password";

	private static DbConnection datasource;
	private BasicDataSource ds;

	private DbConnection() throws IOException, SQLException,
			PropertyVetoException {
		ds = new BasicDataSource();
				
		ds.setDriverClassName(dbDriver);
		ds.setUsername(dbUsername);
		ds.setPassword(dbPassword);
		ds.setUrl(dbAddress);

	}

	public static DbConnection getInstance() throws IOException, SQLException,
			PropertyVetoException {
		if (datasource == null) {
			datasource = new DbConnection();
			return datasource;
		} else {
			return datasource;
		}
	}

	public Connection getConnection() throws SQLException {
        try {
            Class.forName("com.mysql.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
        return this.ds.getConnection();
	}

}
