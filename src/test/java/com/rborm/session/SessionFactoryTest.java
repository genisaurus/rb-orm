package com.rborm.session;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.File;
import java.io.FileNotFoundException;
import java.lang.reflect.Field;
import java.util.Properties;
import java.sql.SQLException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.rborm.context.PropertyNames;
import com.rborm.exceptions.PropertiesException;

public class SessionFactoryTest {

	private static Properties h2Props = new Properties();
	private static String propsPath = "src/test/resources/test.properties";

	@BeforeEach
	public void configureProps() {
		h2Props.put(PropertyNames.ENGINE, "h2");
		h2Props.put(PropertyNames.DB_NAME, "internal");
		h2Props.put(PropertyNames.URL, "mem");
		h2Props.put(PropertyNames.USERNAME, "sa");
		h2Props.put(PropertyNames.PASSWORD, "sa");
	}
	
	@Test
	public void testNoArgsConstructor() {
		SessionFactory sf = new SessionFactory();
		try {
			Field internalProps = sf.getClass().getDeclaredField("props");
			internalProps.setAccessible(true);
			assertEquals((Properties) internalProps.get(sf), h2Props);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Test
	public void testPropertiesConstructor() {
		SessionFactory sf = new SessionFactory(h2Props);
		try {
			Field internalProps = sf.getClass().getDeclaredField("props");
			internalProps.setAccessible(true);
			assertSame((Properties) internalProps.get(sf), h2Props);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	@Test
	public void testStringConstructor() {	
		try {
			SessionFactory sf = new SessionFactory(propsPath);
			Field internalProps = sf.getClass().getDeclaredField("props");
			internalProps.setAccessible(true);
			assertEquals((Properties) internalProps.get(sf), h2Props);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	@Test
	public void testBadPropertiesPath() {
		assertThrows(FileNotFoundException.class, () -> new SessionFactory("doesnotexist.properties"));
	}

	@Test
	public void testFileConstructor() {
		try {
			SessionFactory sf = new SessionFactory(new File(propsPath));
			Field internalProps = sf.getClass().getDeclaredField("props");
			internalProps.setAccessible(true);
			assertEquals((Properties) internalProps.get(sf), h2Props);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Test
	public void testMissingEngine() {
		h2Props.remove(PropertyNames.ENGINE);
		assertThrows(PropertiesException.class, () -> new SessionFactory(h2Props));
	}
	
	@Test public void testUnsupportedEngine() {
		h2Props.put(PropertyNames.ENGINE, "proprietary");
		assertThrows(PropertiesException.class, () -> new SessionFactory(h2Props));
	}

	@Test
	public void testMissingName() {
		h2Props.remove(PropertyNames.DB_NAME);
		assertThrows(PropertiesException.class, () -> new SessionFactory(h2Props));
	}

	@Test
	public void testMissingURL() {
		h2Props.remove(PropertyNames.URL);
		assertThrows(PropertiesException.class, () -> new SessionFactory(h2Props));
	}

	@Test
	public void testMissingUsername() {
		h2Props.remove(PropertyNames.USERNAME);
		assertThrows(PropertiesException.class, () -> new SessionFactory(h2Props));
	}

	@Test
	public void testMissingPassword() {
		h2Props.remove(PropertyNames.PASSWORD);
		assertThrows(PropertiesException.class, () -> new SessionFactory(h2Props));
	}

	@Test
	public void testNewSession() {
		SessionFactory sf = new SessionFactory(h2Props);
		try {
			sf.newSession();
		} catch (SQLException e) {
			System.out.println("Could not create connection to database");
			e.printStackTrace();
		}
	}
	
	@Test
	public void testBadConnection() {
		h2Props.put(PropertyNames.URL, "obviouslywrong.com");
		SessionFactory sf = new SessionFactory(h2Props);
		assertThrows(SQLException.class, () -> sf.newSession());
	}

}
