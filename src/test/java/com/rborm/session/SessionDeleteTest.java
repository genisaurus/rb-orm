package com.rborm.session;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.rborm.test.model.Account;
import com.rborm.test.model.Apartment;
import com.rborm.test.model.Resident;

public class SessionDeleteTest {
	
	private static SessionFactory sf;
	private static Session session;
	private static Connection conn;
	
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
	public void clearTables() throws SQLException {
		conn.createStatement().execute("DELETE FROM accounts");
		conn.createStatement().execute("DELETE FROM residents");
		conn.createStatement().execute("DELETE FROM apartments");
	}
	
	@Test
	public void testDeleteNoTx() throws SQLException {
		conn.createStatement().execute("INSERT INTO accounts VALUES(1, 'test_user')");
		Account test = new Account(1, "test_user");
		session.delete(test);
		ResultSet rs = conn.createStatement().executeQuery("SELECT count(*) FROM accounts");
		rs.next();
		assertEquals(0, rs.getInt(1));
	}
	
	@Test
	public void testDeleteTx() throws SQLException {
		conn.createStatement().execute("INSERT INTO accounts VALUES(1, 'test_user')");
		Account test = new Account(1, "test_user");
		Transaction tx = session.beginTransaction();
		session.delete(test);
		tx.commit();
		ResultSet rs = conn.createStatement().executeQuery("SELECT count(*) FROM accounts");
		rs.next();
		assertEquals(0, rs.getInt(1));
	}
	
	@Test
	public void testDeleteWithForeign() throws SQLException {
		conn.createStatement().execute("INSERT INTO apartments VALUES(101, 'poor')");
		conn.createStatement().execute("INSERT INTO residents VALUES(X'7000c33195884cceaeec56eefc10dca2', 'Jane Doe', 101)");
		Apartment apt = new Apartment(101, "poor");
		UUID uuid = new UUID( 0x7000c33195884ccel, 0xaeec56eefc10dca2l);
		Resident res = new Resident(uuid, "Jane Doe", apt);
		session.delete(res);
		ResultSet rs1 = conn.createStatement().executeQuery("SELECT count(*) FROM apartments");
		rs1.next();
		ResultSet rs2 = conn.createStatement().executeQuery("SELECT count(*) FROM residents");
		rs2.next();
		assertAll(
				() -> assertEquals(1, rs1.getInt(1)), 
				() -> assertEquals(0, rs2.getInt(1)));
	}
	
	@Test
	public void testDeleteCascadeError() throws SQLException {
		conn.createStatement().execute("INSERT INTO apartments VALUES(101, 'poor')");
		conn.createStatement().execute("INSERT INTO residents VALUES(X'7000c33195884cceaeec56eefc10dca2', 'Jane Doe', 101)");
		Apartment apt = new Apartment(101, "poor");
		assertThrows(SQLException.class, () -> session.delete(apt)); 
	}
	
	@Test
	public void testDeleteById() throws SQLException {
		conn.createStatement().execute("INSERT INTO accounts VALUES(1, 'test_user')");
		session.delete(Account.class, 1);
		ResultSet rs = conn.createStatement().executeQuery("SELECT count(*) FROM accounts");
		rs.next();
		assertEquals(0, rs.getInt(1));
	}

}
