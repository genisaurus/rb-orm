package com.rborm.session;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.rborm.test.model.*;


public class SessionGetTest extends SessionTest {
	
	@BeforeAll
	public static void insertValues() throws SQLException, NoSuchFieldException, SecurityException, IllegalArgumentException, IllegalAccessException {
		
		conn.createStatement().execute("INSERT INTO accounts VALUES(1, 'test_user')");
		conn.createStatement().execute("INSERT INTO accounts VALUES(2, 'other_test_user')");
		conn.createStatement().execute("INSERT INTO apartments VALUES(101, 'poor')");
		conn.createStatement().execute("INSERT INTO residents VALUES(X'7000c33195884cceaeec56eefc10dca2', 'Jane Doe', 101)");
		conn.createStatement().execute("INSERT INTO residents VALUES(X'86bc3db7cff04445902d5f23eb50626e', 'John Smith', null)");
				
	}
	
	@AfterAll
	public static void cleanup() throws SQLException {
		conn.createStatement().execute("DELETE FROM accounts");
		conn.createStatement().execute("DELETE FROM residents");
		conn.createStatement().execute("DELETE FROM apartments");
	}
	
	@Test
	public void testGetByIntId() {
		Account ref = new Account(1, "test_user");
		Account test = session.get(Account.class, 1);
		assertEquals(ref, test);
	}
	
	@Test
	public void testGetByUUID() {
		UUID uuid = new UUID( 0x86bc3db7cff04445l, 0x902d5f23eb50626el);
		Resident ref = new Resident(uuid, "John Smith", null);
		Resident test = session.get(Resident.class, uuid);
		assertEquals(ref, test);
	}
	
	@Test
	public void testGetWithForeignKey() {
		Apartment apt = new Apartment(101, "poor");
		UUID uuid = new UUID( 0x7000c33195884ccel, 0xaeec56eefc10dca2l);
		Resident ref = new Resident(uuid, "Jane Doe", apt);
		Resident test = session.get(Resident.class, uuid);
		assertEquals(ref, test);
	}
	
	@Test
	public void testGetNullResult() {
		assertNull(session.get(Account.class, 0));
	}
	
	@Test
	public void testGetAll() {
		Account acct1 = new Account(1, "test_user");
		Account acct2 = new Account(2, "other_test_user");
		List<Account> accts = session.getAll(Account.class);
		assertAll(
				() -> assertTrue(accts.contains(acct1)),
				() -> assertTrue(accts.contains(acct2)));
		
	}
	
	@Test
	public void testCache() throws NoSuchFieldException, SecurityException, NoSuchMethodException, IllegalArgumentException, IllegalAccessException, InvocationTargetException {
		Account acct = new Account(1, "test_user");
		session.useCache(true);
		session.get(Account.class, 1);
		Field cache = session.getClass().getDeclaredField("cache");
		cache.setAccessible(true);
		Method cacheContains = cache.get(session).getClass().getDeclaredMethod("contains", Object.class);
		cacheContains.setAccessible(true);
		assertTrue((Boolean)cacheContains.invoke(cache.get(session), acct));
	}

}
