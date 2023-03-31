package com.rborm.database;

import java.util.Properties;

import com.rborm.context.PropertyNames;

public class PostgresDatabase implements URLBuilder {
	
	public PostgresDatabase() {
		try {
			Class.forName("org.postgresql.Driver");
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		}
	}

	@Override
	public String buildURL(Properties props) {
		String port = props.containsKey(PropertyNames.PORT) ? props.getProperty(PropertyNames.PORT) : "5432";
		return "jdbc:postgresql://" + 
				props.getProperty(PropertyNames.URL) + ":" + port + "/" + 
				props.getProperty(PropertyNames.DB_NAME);
	}

}
