package com.rborm.database;

import java.lang.reflect.Field;
import java.util.List;

import com.rborm.annotations.AnnotationUtils;
import com.rborm.annotations.Column;
import com.rborm.annotations.Id;
import com.rborm.exceptions.MappingException;

// TODO: Consider making this an interface with DB-specific implementations
public final class QueryBuilder {

	public static String selectOne(Class<?> clazz) {
		StringBuilder query = new StringBuilder("SELECT ");
		
		List<Field> fields = AnnotationUtils.findAnnotatedFields(clazz, Column.class);
		for (int i = 0; i < fields.size(); ++i) {
			query.append(AnnotationUtils.findColumnName(fields.get(i)));
			if (i < fields.size() - 1)
				query.append(", ");
		}
		query.append(" FROM " + AnnotationUtils.findTableName(clazz));

		query.append(" WHERE ");
		Field id = AnnotationUtils.findAnnotatedFields(clazz, Id.class).get(0);
		query.append(AnnotationUtils.findColumnName(id) + "=?");

		return query.toString();
	}
	
	public static String selectAll(Class<?> clazz) {
		StringBuilder query = new StringBuilder("SELECT ");
		
		List<Field> fields = AnnotationUtils.findAnnotatedFields(clazz, Column.class);
		for (int i = 0; i < fields.size(); ++i) {
			query.append(AnnotationUtils.findColumnName(fields.get(i)));
			if (i < fields.size() - 1)
				query.append(", ");
		}
		query.append(" FROM " + AnnotationUtils.findTableName(clazz));

		return query.toString();
	}

	// Builds a SQL INSERT query for all @Column-annotated fields of an object.
	// Returns a 2-element array,
	// where the first element is the query and the second element is a
	// comma-delimited list of field names
	// in the order that they are listed in the query.
	public static <T> String[] insert(Class<?> clazz) {
		StringBuilder query = new StringBuilder("INSERT INTO ");
		StringBuilder colComponent = new StringBuilder(AnnotationUtils.findTableName(clazz) + "(");
		StringBuilder valComponent = new StringBuilder("VALUES(");
		StringBuilder orderedFieldList = new StringBuilder();

		List<Field> fields = AnnotationUtils.findAnnotatedFields(clazz, Column.class);
		for (int i = 0; i < fields.size(); ++i) {
			Field f = fields.get(i);
			colComponent.append(AnnotationUtils.findColumnName(f));
			valComponent.append("?");
			orderedFieldList.append(f.getName());

			if (i != fields.size() - 1) {
				colComponent.append(",");
				valComponent.append(",");
				orderedFieldList.append(",");
			}
		}
		colComponent.append(")");
		valComponent.append(")");
		query.append(colComponent + " ");
		query.append(valComponent);

		String[] out = { query.toString(), orderedFieldList.toString() };
		return out;
	}

	// Builds a SQL UPDATE query for all @Column-annotated fields of an object.
	// Returns a 2-element array, where the first element is the query and the
	// second element is a comma-delimited list of field names in the order
	// that they are listed in the query.
	public static String[] update(Class<?> clazz) {
		StringBuilder query = new StringBuilder("UPDATE " + AnnotationUtils.findTableName(clazz) + " SET ");
		StringBuilder orderedFieldList = new StringBuilder();

		List<Field> fields = AnnotationUtils.findAnnotatedFields(clazz, Column.class);
		for (int i = 0; i < fields.size(); ++i) {
			Field f = fields.get(i);
			query.append(AnnotationUtils.findColumnName(f) + "=?");
			orderedFieldList.append(f.getName());

			if (i != fields.size() - 1) {
				query.append(",");
				orderedFieldList.append(",");
			}
		}
		Field id = AnnotationUtils.findAnnotatedFields(clazz, Id.class).get(0);
		query.append(" WHERE " + AnnotationUtils.findColumnName(id) + "=?");
		orderedFieldList.append(","+id.getName());
		
		String[] out = { query.toString(), orderedFieldList.toString() };
		return out;
	}
	
	public static String delete(Class<?> clazz) {
		Field id = AnnotationUtils.findAnnotatedFields(clazz, Id.class).get(0);
		
		StringBuilder query = new StringBuilder("DELETE FROM " + AnnotationUtils.findTableName(clazz) + " WHERE ");
		query.append(AnnotationUtils.findColumnName(id) + "=?");

		return query.toString();
	}
	
	
	
	
	
	
}
