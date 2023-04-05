package com.rborm.session;

import java.lang.annotation.Annotation;
import java.lang.invoke.MethodType;
import java.lang.reflect.Field;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;
import java.util.Map;
import java.util.HashMap;

import com.rborm.annotations.*;
import com.rborm.exceptions.ClassNotMappedException;
import com.rborm.exceptions.MappingException;

public class Session {
	
	Connection conn;
	private Transaction tx;
	
	Session(Connection conn) {
		this.conn = conn;
	}
	
	public Transaction beginTransaction() {
		if (tx == null)
			tx = new Transaction(this);
		return tx;
	}
	
	// get object by ID
	public <T, K> T get(Class<T> clazz,  K id) {
		validate(clazz, id);
		
		T resultObj = null;
		try {
			resultObj = clazz.getDeclaredConstructor().newInstance();
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		// get a list of annotated fields, which includes those of all superclasses
		Field idField = findAnnotatedFields(clazz, Id.class).values().toArray(new Field[10])[0];
		Map<String, Field> fields = findAnnotatedFields(clazz, Column.class);
		
		// Create SQL query
		Mapped mappedAnnotation = clazz.getAnnotation(Mapped.class);
		String tableName = mappedAnnotation.table().equals("") ? clazz.getSimpleName().toLowerCase() : mappedAnnotation.table();
		String query = selectQueryBuilder(tableName, fields, idField, id);
		
		try {
			PreparedStatement stmt = conn.prepareStatement(query);
			if (id instanceof UUID)
				stmt.setObject(1, (UUID) id);
			else if (id instanceof Integer)
				stmt.setInt(1, ((Integer) id).intValue());
			
			
			// if the ResultSet is empty, return null
			ResultSet rs = stmt.executeQuery();
			if (!rs.first())
				return null;
			
			// programmatically invoke setters for every field...
			for (Map.Entry<String, Field> e : fields.entrySet()) {
				String fieldName = e.getKey();
				Field field = e.getValue();
				Object databaseValue = rs.getObject(fieldName);
				// capitalize the first letter of the field name to append after set___ (foo -> setFoo)
				String setterName = (new StringBuilder("set"))
							.append(field.getName().substring(0, 1).toUpperCase() + field.getName().substring(1))
							.toString();
				
				// for legibility, get a shorthand to the field type for finding the setter and invoking it with a parameter
				Class<?> paramType = field.getType();
				// if the field is a foreign key, we need to invoke the setter with the full referenced object.
				if (field.isAnnotationPresent(ForeignKey.class) && databaseValue != null) {
					Field foreignIdField = findAnnotatedFields(paramType, Id.class).values().toArray(new Field[10])[0]; // @Id-annotated field of foreign class
					// if the foreign class' @Id field is primitive, we need to wrap it for Session.get(), as it will not autobox
					Class<?> wrapper = foreignIdField.getType().isPrimitive() ? MethodType.methodType(foreignIdField.getType()).wrap().returnType() : foreignIdField.getType();
					// the databaseValue for this field should be the PK value
					var foreignObj = this.get(paramType, wrapper.cast(databaseValue));
					try {
						resultObj.getClass()
							.getMethod(setterName, paramType)
							.invoke( resultObj, paramType.cast(foreignObj) );
					} catch (Exception e1) {
						e1.printStackTrace();
					}
				} else
					try {
						resultObj.getClass().getMethod(setterName, paramType).invoke( resultObj, databaseValue );
					} catch (Exception e1) {
						e1.printStackTrace();
					}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		return resultObj;
	}
	
	// save object to data store
	public void save(Object obj) {
		
	}
	
	// update object in data store
	public void update(Object obj) {
		
	}
	
	// removed object from persistent data store
	public void delete(Object obj) {
		
	}
	
	public void close() {
		try {
			if (tx != null)
				tx.close();
			this.conn.close();
		} catch (SQLException e) {
			System.out.println("Failed to close database connection when closing a session");
			e.printStackTrace();
		}
		
	}
	
	void txNotifyClosed() {
		this.tx = null;
	}
	
	private <T, K> boolean validate(Class<T> clazz, K id) {
		// Ensure class is flagged to be mapped to the database in the first place.
		if (clazz.getAnnotation(Mapped.class) == null)
			throw new ClassNotMappedException("Class " + clazz.getSimpleName() + " is not annotated with the @Mapped annotation");
		
		// Ensure there is only one @Id annotated field.
		Map<String, Field> idFields = findAnnotatedFields(clazz, Id.class);
		if (idFields.size() == 0)
			throw new MappingException("Class " + clazz.getSimpleName() + " has no @Id annotation.");
		if (idFields.size() > 1)
			throw new MappingException("Class " + clazz.getSimpleName() + " has too many @Id annotations. Composite keys are not supported at this time.");
		Field idField = idFields.values().toArray(new Field[10])[0];
		
		// Ensure given Id type matches @Id-annotated field type. If field type is primitive, wrap it.
		Class<?> idFieldType = idField.getType();
		if (idFieldType.isPrimitive())
			idFieldType = MethodType.methodType(idFieldType).wrap().returnType();
		if (idFieldType != id.getClass())
			throw new IllegalArgumentException(idField.getName() + " is of type " + idFieldType + ", but the provided ID value is " + id.getClass());
		
		return true;
	}
	
	private <T> String selectQueryBuilder(String tableName, Map<String, Field> fields, Field idField, T id) {
		StringBuilder query = new StringBuilder("SELECT ");
		int fieldCount = 0;
		for (String fieldName : fields.keySet()) {
			query.append(fieldName);
			if (fieldCount < fields.size()-1)
				query.append(", ");
			++fieldCount;
		}
		query.append(" FROM ");
		query.append(tableName);
		
		query.append(" WHERE ");
		String name = idField.getAnnotation(Column.class).name().equals("") ? idField.getName().toLowerCase() : idField.getAnnotation(Column.class).name();
		query.append(name + "=?");

		query.append(";");
		
		return query.toString();
	}
	
	private Map<String,Field> findAnnotatedFields(Class<?> clazz, Class<? extends Annotation> annotation) {
		Map<String,Field> fields = new HashMap<>();
		Class<?> c = clazz;
		while(c != null && c != Object.class) {
			for (Field f : c.getDeclaredFields()) {
				f.setAccessible(true);
				if (f.isAnnotationPresent(annotation))
					if (f.isAnnotationPresent(Column.class)) {
						String fieldName = f.getAnnotation(Column.class).name().equals("") ? f.getName().toLowerCase() : f.getAnnotation(Column.class).name();
						fields.put(fieldName, f);
					} else
						fields.put(f.getName(), f);
			}
			c = c.getSuperclass();
		}
		
		return fields;
	}

}
