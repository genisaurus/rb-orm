package com.rborm.session;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;

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

public class SessionUpdateTest extends SessionTest {
	
	
	@AfterEach
	public void clearTables() throws SQLException {
		conn.createStatement().execute("DELETE FROM accounts");
		conn.createStatement().execute("DELETE FROM residents");
		conn.createStatement().execute("DELETE FROM apartments");
	}
	
	@Test
	public void testSaveSingleObjectNoTx() throws SQLException {
		conn.createStatement().execute("INSERT INTO accounts VALUES(1, 'test_user')");
		Account test = new Account(1, "updated_test_user");
		session.update(test);
		ResultSet rs = conn.createStatement().executeQuery("SELECT * FROM accounts WHERE account_id=1");
		rs.next();
		assertEquals(rs.getString(2), "updated_test_user");
	}
	
	@Test 
	public void testSaveSingleObjectTx() throws SQLException {
		conn.createStatement().execute("INSERT INTO accounts VALUES(1, 'test_user')");
		Account test = new Account(1, "updated_test_user");
		Transaction tx = session.beginTransaction();
		session.update(test);
		tx.commit();
		ResultSet rs = conn.createStatement().executeQuery("SELECT * FROM accounts WHERE account_id=1");
		rs.next();
		assertEquals(rs.getString(2), "updated_test_user");
	}
	
	@Test
	public void testSaveCompositeObjectNoTx() throws SQLException {
		conn.createStatement().execute("INSERT INTO apartments VALUES(101, 'poor')");
		conn.createStatement().execute("INSERT INTO residents VALUES(X'7000c33195884cceaeec56eefc10dca2', 'Jane Doe', 101)");
		Apartment apt = new Apartment(101, "actually pretty nice");
		UUID uuid = new UUID( 0x7000c33195884ccel, 0xaeec56eefc10dca2l);
		Resident res = new Resident(uuid, "Jane Doe", apt);
		session.update(res);
		ResultSet rs1 = conn.createStatement().executeQuery("SELECT * FROM apartments WHERE room=101");
		rs1.next();
		assertEquals(rs1.getString(2), "actually pretty nice");
	}
	
	@Test
	public void testSaveCompositeObjectTx() throws SQLException {
		conn.createStatement().execute("INSERT INTO apartments VALUES(101, 'poor')");
		conn.createStatement().execute("INSERT INTO residents VALUES(X'7000c33195884cceaeec56eefc10dca2', 'Jane Doe', 101)");
		Apartment apt = new Apartment(101, "actually pretty nice");
		UUID uuid = new UUID( 0x7000c33195884ccel, 0xaeec56eefc10dca2l);
		Resident res = new Resident(uuid, "Jane Doe", apt);
		Transaction tx = session.beginTransaction();
		session.update(res);
		tx.commit();
		ResultSet rs1 = conn.createStatement().executeQuery("SELECT * FROM apartments WHERE room=101");
		rs1.next();
		assertEquals(rs1.getString(2), "actually pretty nice");
	}

}
