package com.rborm.session;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.File;
import java.lang.reflect.Field;
import java.util.Properties;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.rborm.exceptions.PropertiesException;

public class SessionFactoryTest {

	private static Properties h2Props = new Properties();
	private static File propsPath;

	@BeforeEach
	public void configureProps() {
		h2Props.put("rborm.database_engine", "h2");
		h2Props.put("rborm.database_name", "internal");
		h2Props.put("rborm.connection_url", "mem");
		h2Props.put("rborm.username", "sa");
		h2Props.put("rborm.password", "sa");

		propsPath = new File("src/test/resources/test.properties");
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
	public void testFileConstructor() {
		SessionFactory sf = new SessionFactory(propsPath);
		try {
			Field internalProps = sf.getClass().getDeclaredField("props");
			internalProps.setAccessible(true);
			assertEquals((Properties) internalProps.get(sf), h2Props);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Test
	public void testMissingEngine() {
		h2Props.remove("rborm.database_engine");
		assertThrows(PropertiesException.class, () -> new SessionFactory(h2Props));
	}

	@Test
	public void testMissingName() {
		h2Props.remove("rborm.database_name");
		assertThrows(PropertiesException.class, () -> new SessionFactory(h2Props));
	}

	@Test
	public void testMissingURL() {
		h2Props.remove("rborm.connection_url");
		assertThrows(PropertiesException.class, () -> new SessionFactory(h2Props));
	}

	@Test
	public void testMissingUsername() {
		h2Props.remove("rborm.username");
		assertThrows(PropertiesException.class, () -> new SessionFactory(h2Props));
	}

	@Test
	public void testMissingPassword() {
		h2Props.remove("rborm.password");
		assertThrows(PropertiesException.class, () -> new SessionFactory(h2Props));
	}

	@Test
	public void testNewSession() {
		SessionFactory sf = new SessionFactory(h2Props);
		Session session = sf.newSession();
	}

}
