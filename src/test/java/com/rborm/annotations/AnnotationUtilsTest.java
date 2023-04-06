package com.rborm.annotations;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.lang.reflect.Field;

import org.junit.jupiter.api.Test;

import com.rborm.exceptions.ClassNotMappedException;
import com.rborm.exceptions.MappingException;
import com.rborm.test.model.Account;
import com.rborm.test.model.TooManyIds;
import com.rborm.test.model.NoId;

public class AnnotationUtilsTest {
	
	@Test
	public void testPass() {
		AnnotationUtils.validate(Account.class);
	}
	
	@Test
	public void testFailNotMapped() {
		assertThrows(ClassNotMappedException.class, () -> AnnotationUtils.validate(String.class));
	}
	
	@Test
	public void testFailNoId() {
		assertThrows(MappingException.class, () -> AnnotationUtils.validate(NoId.class));
	}
	
	@Test
	public void testFailTooManyIds() {
		assertThrows(MappingException.class, () -> AnnotationUtils.validate(TooManyIds.class));
	}
	
	@Test
	public void testFindAnnotatedFields() throws NoSuchFieldException, SecurityException {
		Field accountId = Account.class.getDeclaredField("accountId");
		assertEquals(accountId, AnnotationUtils.findAnnotatedFields(Account.class, Id.class).get(0));
	}
	
	@Test
	public void testFindColumnName() throws NoSuchFieldException, SecurityException {
		Field accountId = Account.class.getDeclaredField("accountId");
		assertEquals("account_id", AnnotationUtils.findColumnName(accountId));
	}

}
