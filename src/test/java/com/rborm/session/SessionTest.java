package com.rborm.session;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;

public class SessionTest {
	
	protected static SessionFactory sf;
	protected static Session session;
	protected static Connection conn;
	
	@BeforeAll
	public static void configure() throws SQLException, NoSuchFieldException, SecurityException, IllegalArgumentException, IllegalAccessException {
		try {
			Class.forName("org.h2.Driver");
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		}
		conn = DriverManager.getConnection("jdbc:h2:mem:internal", "sa", "sa");
		sf = new SessionFactory();
		session = sf.newSession();

		String createAccounts =
				"CREATE TEMP TABLE IF NOT EXISTS accounts ("
				+ "account_id INT NOT NULL, "
				+ "username VARCHAR(50) NOT NULL,"
				+ "PRIMARY KEY(account_id) ); ";
		String createApartments = 
				"CREATE TEMP TABLE IF NOT EXISTS apartments ("
				+ "room INT NOT NULL,"
				+ "condition VARCHAR(50) NOT NULL,"
				+ "PRIMARY KEY(room) );";
		String createResidents =
				"CREATE TEMP TABLE IF NOT EXISTS residents ("
				+ "uuid UUID NOT NULL,"
				+ "name VARCHAR(50) NOT NULL,"
				+ "apt INT,"
				+ "FOREIGN KEY(apt) REFERENCES apartments(room),"
				+ "PRIMARY KEY(uuid) );";
		
		conn.createStatement().execute(createAccounts);
		conn.createStatement().execute(createApartments);
		conn.createStatement().execute(createResidents);				
	}
	
	@AfterEach
	public void disableCache() throws SQLException {
		session = sf.newSession();
	}

}
