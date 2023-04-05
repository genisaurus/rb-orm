package com.rborm.annotations;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

@Retention(RUNTIME)
@Target(FIELD)
public @interface Id {
	public boolean generated() default false;
	/* valid generators: 
	 * 		"db":	ID generation is handled by the database upon insertion 
	 * 		"app":	ID generation has to be handled by the application before insertion
	 */
	public String generator() default "database";
	
	/* valid ID generation strategies:
	 * 		"integer":	ID is an int value that is incremented by 1 from the current maximum value found in the DB
	 * 		"uuid":		ID is a type-4 UUID
	 */
	public String strategy() default "integer";
}
