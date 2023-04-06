package com.rborm.session;

import java.lang.annotation.Annotation;
import java.lang.invoke.MethodType;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;
import java.util.List;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.HashMap;
import java.util.Iterator;

import com.rborm.annotations.*;
import com.rborm.database.QueryBuilder;
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
	public <T, K> T get(Class<T> clazz, K id) {
		AnnotationUtils.validate(clazz);

		T resultObj = null;
		try {
			resultObj = clazz.getDeclaredConstructor().newInstance();
		} catch (Exception e) {
			e.printStackTrace();
		}

		// get a list of annotated fields, which includes those of all superclasses
		Field idField = AnnotationUtils.findAnnotatedFields(clazz, Id.class).get(0);
		List<Field> colFields = AnnotationUtils.findAnnotatedFields(clazz, Column.class);

		// Ensure given Id type matches @Id-annotated field type. If field type is primitive, wrap it first.
		Class<?> idFieldType = idField.getType();
		if (idFieldType.isPrimitive())
			idFieldType = MethodType.methodType(idFieldType).wrap().returnType();
		if (idFieldType != id.getClass())
			throw new IllegalArgumentException(idField.getName() + " is of type " + idFieldType
					+ ", but the provided ID value is " + id.getClass());

		// Create SQL query
		String query = QueryBuilder.selectOne(clazz);

		try {
			PreparedStatement stmt = conn.prepareStatement(query);
			stmt.setObject(1, id);
			
			// if the ResultSet is empty, return null
			ResultSet rs = stmt.executeQuery();
			if (!rs.first())
				return null;

			// programmatically invoke setters for every field...
			for (Field col : colFields) {
				String fieldName = AnnotationUtils.findColumnName(col);
				Object databaseValue = rs.getObject(fieldName);
				// capitalize the first letter of the field name to append after set___ (foo -> setFoo)
				String setterName = (new StringBuilder("set"))
						.append(col.getName().substring(0, 1).toUpperCase() + col.getName().substring(1))
						.toString();

				// for legibility, get a shorthand to the field type for finding the setter and invoking it with a parameter
				Class<?> paramType = col.getType();
				// if the field is a foreign key, we need to invoke the setter with the full referenced object.
				if (col.isAnnotationPresent(ForeignKey.class) && databaseValue != null) {
					// @Id-annotated field of foreign class
					Field foreignIdField = AnnotationUtils.findAnnotatedFields(paramType, Id.class).get(0); 
					// if the foreign class' @Id field is primitive, we need to wrap it for Session.get(), as it will not autobox
					Class<?> wrapper = foreignIdField.getType().isPrimitive()
							? MethodType.methodType(foreignIdField.getType()).wrap().returnType()
							: foreignIdField.getType();
					// the databaseValue for this field should be the PK value
					var foreignObj = this.get(paramType, wrapper.cast(databaseValue));
					try {
						resultObj.getClass().getMethod(setterName, paramType).invoke(resultObj,
								paramType.cast(foreignObj));
					} catch (Exception e1) {
						e1.printStackTrace();
					}
				} else
					try {
						resultObj.getClass().getMethod(setterName, paramType).invoke(resultObj, databaseValue);
					} catch (Exception e1) {
						e1.printStackTrace();
					}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

		return resultObj;
	}
	
	public <T> List<T> getAll(Class<T> clazz) {
		List<T> results = new ArrayList<>();
		
		AnnotationUtils.validate(clazz);

		// get a list of annotated fields, which includes those of all superclasses
		Field idField = AnnotationUtils.findAnnotatedFields(clazz, Id.class).get(0);
		List<Field> colFields = AnnotationUtils.findAnnotatedFields(clazz, Column.class);

		// Ensure given Id type matches @Id-annotated field type. If field type is primitive, wrap it first.
		Class<?> idFieldType = idField.getType();
		if (idFieldType.isPrimitive())
			idFieldType = MethodType.methodType(idFieldType).wrap().returnType();

		// Create SQL query
		String query = QueryBuilder.selectAll(clazz);

		try {
			PreparedStatement stmt = conn.prepareStatement(query);
			
			// if the ResultSet is empty, return null
			ResultSet rs = stmt.executeQuery();
			while(rs.next()) {
			
				T resultObj = null;
				try {
					resultObj = clazz.getDeclaredConstructor().newInstance();
				} catch (Exception e) {
					e.printStackTrace();
				}
	
				// programmatically invoke setters for every field...
				for (Field col : colFields) {
					String fieldName = AnnotationUtils.findColumnName(col);
					Object databaseValue = rs.getObject(fieldName);
					// capitalize the first letter of the field name to append after set___ (foo -> setFoo)
					String setterName = (new StringBuilder("set"))
							.append(col.getName().substring(0, 1).toUpperCase() + col.getName().substring(1))
							.toString();
	
					// for legibility, get a shorthand to the field type for finding the setter and invoking it with a parameter
					Class<?> paramType = col.getType();
					// if the field is a foreign key, we need to invoke the setter with the full referenced object.
					if (col.isAnnotationPresent(ForeignKey.class) && databaseValue != null) {
						// @Id-annotated field of foreign class
						Field foreignIdField = AnnotationUtils.findAnnotatedFields(paramType, Id.class).get(0); 
						// if the foreign class' @Id field is primitive, we need to wrap it for Session.get(), as it will not autobox
						Class<?> wrapper = foreignIdField.getType().isPrimitive()
								? MethodType.methodType(foreignIdField.getType()).wrap().returnType()
								: foreignIdField.getType();
						// the databaseValue for this field should be the PK value
						var foreignObj = this.get(paramType, wrapper.cast(databaseValue));
						try {
							resultObj.getClass().getMethod(setterName, paramType).invoke(resultObj,
									paramType.cast(foreignObj));
						} catch (Exception e1) {
							e1.printStackTrace();
						}
					} else
						try {
							resultObj.getClass().getMethod(setterName, paramType).invoke(resultObj, databaseValue);
						} catch (Exception e1) {
							e1.printStackTrace();
						}
				}
				results.add(resultObj);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		return results;
	}

	// save object to data store
	public <T> void save(T obj) throws SQLException {
		AnnotationUtils.validate(obj.getClass());

		// recursively save Foreign key objects first to preserve referential integrity
		List<Field> foreignKeys = AnnotationUtils.findAnnotatedFields(obj.getClass(), ForeignKey.class);
		for (Field fk : foreignKeys)
			try {
				save(fk.get(obj));
			} catch (Exception e1) {
				System.out.println("Could not save foreign key " + AnnotationUtils.findColumnName(fk) + " of class " + obj.getClass());
				e1.printStackTrace();
			}

		String[] queryAndFields = QueryBuilder.insert(obj.getClass());

		PreparedStatement stmt = null;
		try {
			stmt = conn.prepareStatement(queryAndFields[0]);
			String[] orderedFields = queryAndFields[1].split(",");
			for (int i = 1; i <= orderedFields.length; ++i) {
				String fieldName = orderedFields[i - 1];
				Field field = obj.getClass().getDeclaredField(fieldName);
				field.setAccessible(true);
				// if the field is a FK / reference to a foreign object, get that objects @Id
				// and save that instead
				if (obj.getClass().getDeclaredField(fieldName).isAnnotationPresent(ForeignKey.class)) {
					Field foreignIdField = AnnotationUtils.findAnnotatedFields(field.getType(), Id.class).get(0);
					foreignIdField.setAccessible(true);
					var foreignObj = field.get(obj);
					stmt.setObject(i, foreignIdField.get(foreignObj));
				} else
					stmt.setObject(i, field.get(obj));
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

		if (this.tx != null)
			tx.addStatement(stmt);
		else
			try {
				if (stmt != null)
					stmt.execute();
			} catch (SQLException e) {
				System.out.println("Could not save " + obj + " (no transaction)");
				throw e;
			}
	}

	// update object in data store
	public <T> void update(T obj) throws SQLException {
		AnnotationUtils.validate(obj.getClass());

		// recursively update Foreign key objects first to preserve referential integrity
		List<Field> foreignKeys = AnnotationUtils.findAnnotatedFields(obj.getClass(), ForeignKey.class);
		for (Field fk : foreignKeys)
			try {
				update(fk.get(obj));
			} catch (Exception e1) {
				System.out.println("Could not save foreign key " + AnnotationUtils.findColumnName(fk) + " of class " + obj.getClass());
				e1.printStackTrace();
			}

		String[] queryAndFields = QueryBuilder.update(obj.getClass());

		PreparedStatement stmt = null;
		try {
			stmt = conn.prepareStatement(queryAndFields[0]);
			String[] orderedFields = queryAndFields[1].split(",");
			for (int i = 1; i <= orderedFields.length; ++i) {
				String fieldName = orderedFields[i - 1];
				Field field = obj.getClass().getDeclaredField(fieldName);
				field.setAccessible(true);
				// if the field is a FK / reference to a foreign object, get that objects @Id
				// and save that instead
				if (obj.getClass().getDeclaredField(fieldName).isAnnotationPresent(ForeignKey.class)) {
					Field foreignIdField = AnnotationUtils.findAnnotatedFields(field.getType(), Id.class).get(0);
					foreignIdField.setAccessible(true);
					var foreignObj = field.get(obj);
					stmt.setObject(i, foreignIdField.get(foreignObj));
				} else
					stmt.setObject(i, field.get(obj));
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

		if (this.tx != null)
			tx.addStatement(stmt);
		else
			try {
				if (stmt != null)
					stmt.execute();
			} catch (SQLException e) {
				System.out.println("Could not update " + obj + " (no transaction)");
				throw e;
			}
	}

	// removed object from persistent data store
	public <T> void delete(T obj) throws SQLException {
		AnnotationUtils.validate(obj.getClass());

		String query = QueryBuilder.delete(obj.getClass());

		PreparedStatement stmt = null;
		try {
			stmt = conn.prepareStatement(query);
			Field id = AnnotationUtils.findAnnotatedFields(obj.getClass(), Id.class).get(0);
			stmt.setObject(1, id.get(obj));
		} catch (Exception e) {
			e.printStackTrace();
		}

		if (this.tx != null)
			tx.addStatement(stmt);
		else
			// preserving SQLException throw in case of cascade error
			stmt.execute();
	}
	
	public <T> void delete(Class<?> clazz, T id) throws SQLException {
		AnnotationUtils.validate(clazz);
		
		String query = QueryBuilder.delete(clazz);
		
		PreparedStatement stmt = null;
		try {
			stmt = conn.prepareStatement(query);
			stmt.setObject(1, id);
		} catch (Exception e) {
			e.printStackTrace();
		}

		if (this.tx != null)
			tx.addStatement(stmt);
		else
			// preserving SQLException throw in case of cascade error
			stmt.execute();
		
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

}
