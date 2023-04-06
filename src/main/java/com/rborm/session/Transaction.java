package com.rborm.session;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;
import java.util.ArrayList;

class Transaction {
	
	private Session session;
	private List<PreparedStatement> stmts;
	private PreparedStatement txOpenStmt;
	private PreparedStatement txRollbackStmt;
	private PreparedStatement txCommitStmt;
	private boolean wasCommitted = false;
	
	public Transaction(Session session) {
		this.session = session;
		stmts = new ArrayList<>();
		
		try {
			txOpenStmt = session.conn.prepareStatement("BEGIN;");
			txRollbackStmt = session.conn.prepareStatement("ROLLBACK;");
			txCommitStmt = session.conn.prepareStatement("COMMIT;");
		} catch (SQLException e) {
			System.out.println("Connection could not prepare transaction statements");
			e.printStackTrace();
		}
		try {
			txOpenStmt.execute();
		} catch (SQLException e) {
			System.out.println("Could not begin transaction");
			e.printStackTrace();
		}
	}
	
	void addStatement(PreparedStatement stmt) {
		this.stmts.add(stmt);
	}
	
	public void commit() throws SQLException {
		for(PreparedStatement stmt : stmts)
			try {
				stmt.execute();
			} catch (SQLException e) {
				System.out.println("Statement \"" + stmt.toString() + "\" could not be executed, rolling transaction back");
				rollback();
				throw e;
			}
		try {
			txCommitStmt.execute();
		} catch (SQLException e) {
			System.out.println("Could not commit transaction, rolling transaction back");
			e.printStackTrace();
			rollback();
			throw e;
		}
		this.wasCommitted = true;
		close();
	}
	
	public void rollback() {
		this.stmts = new ArrayList<>();
		this.wasCommitted = false;
		try {
			txRollbackStmt.execute();
		} catch(SQLException e) {
			System.out.println("Transaction could not be rolled back. This transaction has been emptied of pending queries, and should be closed.");
		}
	}
	
	public void close() {
		if(!wasCommitted)
			rollback();
		session.txNotifyClosed();
	}

}
