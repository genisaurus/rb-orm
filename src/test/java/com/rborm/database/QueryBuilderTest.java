package com.rborm.database;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import com.rborm.test.model.Account;

public class QueryBuilderTest {
	
	@Test
	public void testGetOne() {
		String query = "SELECT account_id, username FROM accounts WHERE account_id=?";
		assertEquals(query, QueryBuilder.selectOne(Account.class));
	}
	
	@Test
	public void testGetAll() {
		String query = "SELECT account_id, username FROM accounts";
		assertEquals(query, QueryBuilder.selectAll(Account.class));
	}
	
	@Test
	public void testInsert() {
		String query = "INSERT INTO accounts(account_id,username) VALUES(?,?)";
		assertEquals(query, QueryBuilder.insert(Account.class)[0]);
	}
	
	@Test
	public void testUpdate() {
		String query = "UPDATE accounts SET account_id=?,username=? WHERE account_id=?";
		assertEquals(query, QueryBuilder.update(Account.class)[0]);
	}
	
	@Test
	public void testDelete() {
		String query = "DELETE FROM accounts WHERE account_id=?";
		assertEquals(query, QueryBuilder.delete(Account.class));
	}
	

}
