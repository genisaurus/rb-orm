package com.rborm.session;

import java.sql.Connection;
import java.sql.SQLException;

public class Session {
	
	private Connection conn;
	private Transaction tx;
	
	Session(Connection conn) {
		this.conn = conn;
	}
	
	public Transaction beginTransaction() {
		if (tx == null)
			tx = new Transaction(this);
		return tx;
	}
	
	// get object by ID
	public <T> T get(Class<T> clazz, int id) {
		return null;
	}
	
	// save object to data store
	public void save(Object obj) {
		
	}
	
	// update object in data store
	public void update(Object obj) {
		
	}
	
	// removed object from persistent data store
	public void delete(Object obj) {
		
	}
	
	public void close() {
		try {
			this.conn.close();
		} catch (SQLException e) {
			System.out.println("Failed to close database connection when closing a session");
			e.printStackTrace();
		}
		
	}

}
