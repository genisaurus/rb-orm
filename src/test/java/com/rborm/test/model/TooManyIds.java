package com.rborm.test.model;

import com.rborm.annotations.Column;
import com.rborm.annotations.Id;
import com.rborm.annotations.Mapped;

@Mapped
public class TooManyIds {

	@Id
	@Column
	public int a;
	
	@Id
	@Column
	public int b;
}
