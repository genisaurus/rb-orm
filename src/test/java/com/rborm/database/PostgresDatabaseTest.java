package com.rborm.database;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Properties;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.rborm.context.PropertyNames;

public class PostgresDatabaseTest {
	
private static Properties postgresProps = new Properties();
	
	@BeforeAll
	public static void configureProps() {
		postgresProps.put(PropertyNames.ENGINE, "postgresql");
		postgresProps.put(PropertyNames.DB_NAME, "postgres");
		postgresProps.put(PropertyNames.URL, "localhost");
		postgresProps.put(PropertyNames.PORT, "5432");
		postgresProps.put(PropertyNames.USERNAME, "postgres");
		postgresProps.put(PropertyNames.PASSWORD, "password");
	}
	
	@Test
	public void testBuildURL() {
		PostgresDatabase db = new PostgresDatabase();
		assertEquals(db.buildURL(postgresProps), "jdbc:postgresql://localhost:5432/postgres");
	}

}
