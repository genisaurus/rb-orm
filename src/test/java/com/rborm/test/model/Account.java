package com.rborm.test.model;

import java.util.Objects;

import com.rborm.annotations.Column;
import com.rborm.annotations.Id;
import com.rborm.annotations.Mapped;

@Mapped(table="accounts")
public class Account {
	
	@Id
	@Column(name="account_id")
	private int accountId;
	@Column
	private String username;
	
	
	
	public Account() {
		super();
	}
	public Account(int accountId, String username) {
		super();
		this.accountId = accountId;
		this.username = username;
	}
	public int getAccountId() {
		return accountId;
	}
	public void setAccountId(int accountId) {
		this.accountId = accountId;
	}
	public String getUsername() {
		return username;
	}
	public void setUsername(String username) {
		this.username = username;
	}
	@Override
	public String toString() {
		return "Account [accountId=" + accountId + ", username=" + username + "]";
	}
	@Override
	public int hashCode() {
		return Objects.hash(accountId, username);
	}
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (!(obj instanceof Account))
			return false;
		Account other = (Account) obj;
		return accountId == other.accountId && Objects.equals(username, other.username);
	}
	
	
}
