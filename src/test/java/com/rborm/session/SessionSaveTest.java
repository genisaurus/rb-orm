package com.rborm.session;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
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

public class SessionSaveTest extends SessionTest {
	
	@AfterEach
	public void clearTables() throws SQLException {
		conn.createStatement().execute("DELETE FROM accounts");
		conn.createStatement().execute("DELETE FROM residents");
		conn.createStatement().execute("DELETE FROM apartments");
	}
	
	@Test
	public void testSaveSingleObjectNoTx() throws SQLException {
		Account test = new Account(1, "test_user");
		session.save(test);
		ResultSet rs = conn.createStatement().executeQuery("SELECT * FROM accounts WHERE account_id=1");
		rs.next();
		assertEquals("test_user", rs.getString(2));
	}
	
	@Test 
	public void testSaveSingleObjectTx() throws SQLException {
		Account test = new Account(1, "test_user");
		Transaction tx = session.beginTransaction();
		session.save(test);
		tx.commit();
		ResultSet rs = conn.createStatement().executeQuery("SELECT * FROM accounts WHERE account_id=1");
		rs.next();
		assertEquals("test_user", rs.getString(2));
	}
	
	@Test
	public void testSaveCompositeObjectNoTx() throws SQLException {
		Apartment apt = new Apartment(101, "poor");
		UUID uuid = new UUID( 0x7000c33195884ccel, 0xaeec56eefc10dca2l);
		Resident res = new Resident(uuid, "Jane Doe", apt);
		session.save(res);
		ResultSet rs1 = conn.createStatement().executeQuery("SELECT * FROM apartments WHERE room=101");
		rs1.next();
		ResultSet rs2 = conn.createStatement().executeQuery("SELECT * FROM residents WHERE name='Jane Doe'");
		rs2.next();
		assertAll(
				() -> assertEquals("poor", rs1.getString(2)), 
				() -> assertEquals(101, rs2.getInt(3)));
	}
	
	@Test
	public void testSaveCompositeObjectTx() throws SQLException {
		Apartment apt = new Apartment(101, "poor");
		UUID uuid = new UUID( 0x7000c33195884ccel, 0xaeec56eefc10dca2l);
		Resident res = new Resident(uuid, "Jane Doe", apt);
		Transaction tx = session.beginTransaction();
		session.save(res);
		tx.commit();
		ResultSet rs1 = conn.createStatement().executeQuery("SELECT * FROM apartments WHERE room=101");
		rs1.next();
		ResultSet rs2 = conn.createStatement().executeQuery("SELECT * FROM residents WHERE name='Jane Doe'");
		rs2.next();
		assertAll(
				() -> assertEquals("poor", rs1.getString(2)), 
				() -> assertEquals(101, rs2.getInt(3)));
	}
	
	@Test
	public void testCache() throws NoSuchFieldException, SecurityException, NoSuchMethodException, IllegalArgumentException, IllegalAccessException, InvocationTargetException, SQLException {
		Account acct = new Account(1, "test_user");
		session.useCache(true);
		session.save(acct);
		Field cache = session.getClass().getDeclaredField("cache");
		cache.setAccessible(true);
		Method cacheContains = cache.get(session).getClass().getDeclaredMethod("contains", Object.class);
		cacheContains.setAccessible(true);
		assertTrue((Boolean)cacheContains.invoke(cache.get(session), acct));
	}

}
