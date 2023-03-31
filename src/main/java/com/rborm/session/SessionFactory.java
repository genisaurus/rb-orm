package com.rborm.session;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Properties;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

import com.rborm.context.PropertyNames;
import com.rborm.database.*;
import com.rborm.exceptions.PropertiesException;

public class SessionFactory {

	private String[] supportedDatabases = {"h2", "postgresql"};
	private Properties props;
	private URLBuilder urlBuilder;

	public SessionFactory() {
		Properties h2 = new Properties();
		h2.put(PropertyNames.ENGINE, "h2");
		h2.put(PropertyNames.DB_NAME, "internal");
		h2.put(PropertyNames.URL, "mem");
		h2.put(PropertyNames.USERNAME, "sa");
		h2.put(PropertyNames.PASSWORD, "sa");
		this.props = h2;
		this.urlBuilder = new H2Database();
	}

	public SessionFactory(Properties props) throws PropertiesException {
		validateProperties(props);

		this.props = props;
		switch (props.getProperty(PropertyNames.ENGINE)) {
			case "postgresql":
				this.urlBuilder = new PostgresDatabase();
				break;
			case "h2":
			default:
				this.urlBuilder = new H2Database();
				break;
		}
	}

	public SessionFactory(File propsFile) {
		this(convert(propsFile));
	}

	public SessionFactory(String propsPath) {
		this(new File(propsPath));
	}

	private static Properties convert(File propsFile) {
		Properties props = new Properties();
		try {
			props.load(new FileInputStream(propsFile));
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return props;
	}
	
	private void validateProperties(Properties props) throws PropertiesException {
		String engine = props.containsKey(PropertyNames.ENGINE) ? props.getProperty(PropertyNames.ENGINE) : null;
		if (engine == null)
			throw new PropertiesException("Missing property: " + PropertyNames.ENGINE);
		else {
			boolean supported = false;
			for(String db : supportedDatabases)
				if (db.equals(engine))
					supported = true;
			if (!supported)
				throw new PropertiesException("Unsupported Database Engine: " + engine);
		}
		if (!props.containsKey(PropertyNames.DB_NAME) || "".equals(props.getProperty(PropertyNames.DB_NAME)))
			throw new PropertiesException("Missing property: " + PropertyNames.DB_NAME);
		if (!props.containsKey(PropertyNames.URL) || "".equals(props.getProperty(PropertyNames.URL)))
			throw new PropertiesException("Missing property: " + PropertyNames.URL);
		if (!props.containsKey(PropertyNames.USERNAME) || "".equals(props.getProperty(PropertyNames.USERNAME)))
			throw new PropertiesException("Missing property: " + PropertyNames.USERNAME);
		if (!props.containsKey(PropertyNames.PASSWORD) || "".equals(props.getProperty(PropertyNames.PASSWORD)))
			throw new PropertiesException("Missing property: " + PropertyNames.PASSWORD);
	}

	public Session newSession() {
		Connection conn = null;
		String url = urlBuilder.buildURL(props);
		try {
			conn = DriverManager.getConnection(url, props.getProperty(PropertyNames.USERNAME), props.getProperty(PropertyNames.PASSWORD));
		} catch(SQLException e) {
			System.out.println("Could not create connection to database");
			e.printStackTrace();
		}
		return new Session(conn);

	}

}
