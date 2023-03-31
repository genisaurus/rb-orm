package com.rborm.session;

import java.sql.PreparedStatement;

class Transaction {
	
	private Session session;
	private PreparedStatement stmt;
	
	public Transaction(Session session) {
		this.session = session;
	}
	
	public void commit() {
		
	}
	
	public void rollback() {
		
	}

}
