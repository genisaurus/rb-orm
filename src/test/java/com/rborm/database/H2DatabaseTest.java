package com.rborm.database;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Properties;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.rborm.context.PropertyNames;

public class H2DatabaseTest {
	
	private static Properties h2Props = new Properties();
	
	@BeforeAll
	public static void configureProps() {
		h2Props.put(PropertyNames.ENGINE, "h2");
		h2Props.put(PropertyNames.DB_NAME, "internal");
		h2Props.put(PropertyNames.URL, "mem");
		h2Props.put(PropertyNames.USERNAME, "sa");
		h2Props.put(PropertyNames.PASSWORD, "sa");
	}
	
	@Test
	public void testBuildURL() {
		H2Database db = new H2Database();
		assertEquals("jdbc:h2:mem:internal", db.buildURL(h2Props));
	}

}
