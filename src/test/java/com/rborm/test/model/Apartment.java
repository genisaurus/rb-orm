package com.rborm.test.model;

import java.util.Objects;

import com.rborm.annotations.Column;
import com.rborm.annotations.Id;
import com.rborm.annotations.Mapped;

@Mapped(table="apartments")
public class Apartment {
	@Id
	@Column
	private int room;
	@Column
	private String condition;
	
	public Apartment() {
		super();
	}
	
	public Apartment(int room, String condition) {
		super();
		this.room = room;
		this.condition = condition;
	}

	public int getRoom() {
		return room;
	}
	public void setRoom(int room) {
		this.room = room;
	}
	public String getCondition() {
		return condition;
	}
	public void setCondition(String condition) {
		this.condition = condition;
	}
	@Override
	public String toString() {
		return "Apartment [room=" + room + ", condition=" + condition + "]";
	}
	@Override
	public int hashCode() {
		return Objects.hash(condition, room);
	}
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (!(obj instanceof Apartment))
			return false;
		Apartment other = (Apartment) obj;
		return Objects.equals(condition, other.condition) && room == other.room;
	}

	
	
}
