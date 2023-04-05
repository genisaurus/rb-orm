package com.rborm.test.model;

import java.util.Objects;
import java.util.UUID;

import com.rborm.annotations.Column;
import com.rborm.annotations.ForeignKey;
import com.rborm.annotations.Id;
import com.rborm.annotations.Mapped;

@Mapped(table="residents")
public class Resident {
	@Id
	@Column
	private UUID uuid;
	@Column
	private String name;
	@ForeignKey
	@Column
	private Apartment apt;
	
	public Resident() {
		super();
	}
	public Resident(UUID uuid, String name, Apartment apt) {
		super();
		this.uuid = uuid;
		this.name = name;
		this.apt = apt;
	}
	public UUID getUuid() {
		return uuid;
	}
	public void setUuid(UUID uuid) {
		this.uuid = uuid;
	}
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public Apartment getApt() {
		return apt;
	}
	public void setApt(Apartment apt) {
		this.apt = apt;
	}
	@Override
	public String toString() {
		return "Resident [uuid=" + uuid + ", name=" + name + ", apt=" + apt + "]";
	}
	@Override
	public int hashCode() {
		return Objects.hash(apt, name, uuid);
	}
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (!(obj instanceof Resident))
			return false;
		Resident other = (Resident) obj;
		return Objects.equals(apt, other.apt) && Objects.equals(name, other.name) && Objects.equals(uuid, other.uuid);
	}
	
	
}
