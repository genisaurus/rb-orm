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
		validate(clazz);

		T resultObj = null;
		try {
			resultObj = clazz.getDeclaredConstructor().newInstance();
		} catch (Exception e) {
			e.printStackTrace();
		}

		// get a list of annotated fields, which includes those of all superclasses
		Field idField = findAnnotatedFields(clazz, Id.class).values().toArray(new Field[10])[0];
		Map<String, Field> fields = findAnnotatedFields(clazz, Column.class);

		// Ensure given Id type matches @Id-annotated field type. If field type is primitive, wrap it first.
		Class<?> idFieldType = idField.getType();
		if (idFieldType.isPrimitive())
			idFieldType = MethodType.methodType(idFieldType).wrap().returnType();
		if (idFieldType != id.getClass())
			throw new IllegalArgumentException(idField.getName() + " is of type " + idFieldType
					+ ", but the provided ID value is " + id.getClass());

		// Create SQL query
		Mapped mappedAnnotation = clazz.getAnnotation(Mapped.class);
		String tableName = mappedAnnotation.table().equals("") ? clazz.getSimpleName().toLowerCase()
				: mappedAnnotation.table();
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
					// @Id-annotated field of foreign class
					Field foreignIdField = findAnnotatedFields(paramType, Id.class).values().toArray(new Field[10])[0]; 
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

	// save object to data store
	public <T> void save(T obj) {
		validate(obj.getClass());

		// recursively save Foreign key objects first to preserve referential integrity
		Map<String, Field> foreignKeys = findAnnotatedFields(obj.getClass(), ForeignKey.class);
		for (Entry<String, Field> e : foreignKeys.entrySet())
			try {
				save(e.getValue().get(obj));
			} catch (Exception e1) {
				System.out.println("Could not save foreign key " + e.getKey() + " of class " + obj.getClass());
				e1.printStackTrace();
			}

		Mapped mappedAnnotation = obj.getClass().getAnnotation(Mapped.class);
		String tableName = mappedAnnotation.table().equals("") ? obj.getClass().getSimpleName().toLowerCase()
				: mappedAnnotation.table();

		String[] queryAndFields = insertQueryBuilder(tableName, obj);

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
					Field foreignIdField = findAnnotatedFields(field.getType(), Id.class).values()
							.toArray(new Field[10])[0]; // @Id-annotated field of foreign class
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
				e.printStackTrace();
			}
	}

	// update object in data store
	public <T> void update(T obj) {
		validate(obj.getClass());

		// recursively update Foreign key objects first to preserve referential integrity
		Map<String, Field> foreignKeys = findAnnotatedFields(obj.getClass(), ForeignKey.class);
		for (Entry<String, Field> e : foreignKeys.entrySet())
			try {
				update(e.getValue().get(obj));
			} catch (Exception e1) {
				System.out.println("Could not save foreign key " + e.getKey() + " of class " + obj.getClass());
				e1.printStackTrace();
			}

		Mapped mappedAnnotation = obj.getClass().getAnnotation(Mapped.class);
		String tableName = mappedAnnotation.table().equals("") ? obj.getClass().getSimpleName().toLowerCase()
				: mappedAnnotation.table();

		String[] queryAndFields = updateQueryBuilder(tableName, obj);

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
					Field foreignIdField = findAnnotatedFields(field.getType(), Id.class).values()
							.toArray(new Field[10])[0]; // @Id-annotated field of foreign class
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
				e.printStackTrace();
			}
	}

	// removed object from persistent data store
	public <T> void delete(T obj) {

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

	private <T, K> boolean validate(Class<T> clazz) {
		// Ensure class is flagged to be mapped to the database in the first place.
		if (clazz.getAnnotation(Mapped.class) == null)
			throw new ClassNotMappedException(
					"Class " + clazz.getSimpleName() + " is not annotated with the @Mapped annotation");

		// Ensure there is only one @Id annotated field.
		Map<String, Field> idFields = findAnnotatedFields(clazz, Id.class);
		if (idFields.size() == 0)
			throw new MappingException("Class " + clazz.getSimpleName() + " has no @Id annotation.");
		if (idFields.size() > 1)
			throw new MappingException("Class " + clazz.getSimpleName()
					+ " has too many @Id annotations. Composite keys are not supported at this time.");

		return true;
	}

	private Map<String, Field> findAnnotatedFields(Class<?> clazz, Class<? extends Annotation> annotation) {
		Map<String, Field> fields = new HashMap<>();
		Class<?> c = clazz;
		while (c != null && c != Object.class) {
			for (Field f : c.getDeclaredFields()) {
				f.setAccessible(true);
				if (f.isAnnotationPresent(annotation))
					if (f.isAnnotationPresent(Column.class)) {
						String fieldName = f.getAnnotation(Column.class).name().equals("") ? f.getName().toLowerCase()
								: f.getAnnotation(Column.class).name();
						fields.put(fieldName, f);
					} else
						fields.put(f.getName(), f);
			}
			c = c.getSuperclass();
		}

		return fields;
	}

	private <T> String selectQueryBuilder(String tableName, Map<String, Field> fields, Field idField, T id) {
		StringBuilder query = new StringBuilder("SELECT ");
		int fieldCount = 0;
		for (String fieldName : fields.keySet()) {
			query.append(fieldName);
			if (fieldCount < fields.size() - 1)
				query.append(", ");
			++fieldCount;
		}
		query.append(" FROM ");
		query.append(tableName);

		query.append(" WHERE ");
		String name = idField.getAnnotation(Column.class).name().equals("") ? idField.getName().toLowerCase()
				: idField.getAnnotation(Column.class).name();
		query.append(name + "=?");

		query.append(";");

		return query.toString();
	}

	// Builds a SQL INSERT query for all @Column-annotated fields of an object.
	// Returns a 2-element array,
	// where the first element is the query and the second element is a
	// comma-delimited list of field names
	// in the order that they are listed in the query.
	private <T> String[] insertQueryBuilder(String tableName, T obj) {
		StringBuilder query = new StringBuilder("INSERT INTO ");
		StringBuilder colComponent = new StringBuilder(tableName + "(");
		StringBuilder valComponent = new StringBuilder("VALUES(");
		StringBuilder orderedFieldList = new StringBuilder();

		Set<Entry<String, Field>> entries = findAnnotatedFields(obj.getClass(), Column.class).entrySet();
		Iterator<Entry<String, Field>> iter = entries.iterator();
		for (int i = 0; i < entries.size(); i++) {
			Entry<String, Field> e = iter.next();
			colComponent.append(e.getKey());
			valComponent.append("?");
			orderedFieldList.append(e.getValue().getName());

			if (i != entries.size() - 1) {
				colComponent.append(",");
				valComponent.append(",");
				orderedFieldList.append(",");
			}
		}
		colComponent.append(")");
		valComponent.append(")");
		query.append(colComponent);
		query.append(valComponent);

		String[] out = { query.toString(), orderedFieldList.toString() };
		return out;
	}

	// Builds a SQL UPDATE query for all @Column-annotated fields of an object.
	// Returns a 2-element array, where the first element is the query and the
	// second element is a comma-delimited list of field names in the order
	// that they are listed in the query.
	private <T> String[] updateQueryBuilder(String tableName, T obj) {
		StringBuilder query = new StringBuilder("UPDATE " + tableName + " SET ");
		StringBuilder orderedFieldList = new StringBuilder();

		Set<Entry<String, Field>> entries = findAnnotatedFields(obj.getClass(), Column.class).entrySet();
		Iterator<Entry<String, Field>> iter = entries.iterator();
		for (int i = 0; i < entries.size(); i++) {
			Entry<String, Field> e = iter.next();
			query.append(e.getKey() + "=?");
			orderedFieldList.append(e.getValue().getName());

			if (i != entries.size() - 1) {
				query.append(",");
				orderedFieldList.append(",");
			}
		}
		Entry<String, Field> id = findAnnotatedFields(obj.getClass(), Id.class).entrySet().iterator().next();
		query.append(" WHERE " + id.getKey() + "=?;");
		orderedFieldList.append(","+id.getValue().getName());
		
		String[] out = { query.toString(), orderedFieldList.toString() };
		return out;
	}

}
