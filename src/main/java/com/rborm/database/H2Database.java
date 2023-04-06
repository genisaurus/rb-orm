package com.rborm.database;

import java.util.Properties;

import com.rborm.context.PropertyNames;

public class H2Database implements URLBuilder {
	
	public H2Database() {
		try {
			Class.forName("org.h2.Driver");
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		}
	}

	@Override
	public String buildURL(Properties props) {
		System.out.println(props);
		System.out.println(props.getProperty(PropertyNames.URL));
		return "jdbc:h2:" + props.getProperty(PropertyNames.URL) + ":" + props.getProperty(PropertyNames.DB_NAME);
	}

}
