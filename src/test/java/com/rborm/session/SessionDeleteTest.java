package com.rborm.session;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
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

public class SessionDeleteTest extends SessionTest {
	
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
	
	@Test
	public void testCache() throws NoSuchFieldException, SecurityException, NoSuchMethodException, IllegalArgumentException, IllegalAccessException, InvocationTargetException, SQLException {
		Account acct = new Account(1, "test_user");
		session.useCache(true);
		session.get(Account.class, 1);
		session.delete(acct);
		Field cache = session.getClass().getDeclaredField("cache");
		cache.setAccessible(true);
		Method cacheContains = cache.get(session).getClass().getDeclaredMethod("contains", Object.class);
		cacheContains.setAccessible(true);
		assertFalse((Boolean)cacheContains.invoke(cache.get(session), acct));
	}

}
