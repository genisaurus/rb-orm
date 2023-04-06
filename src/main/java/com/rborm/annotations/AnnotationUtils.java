package com.rborm.annotations;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.rborm.exceptions.ClassNotMappedException;
import com.rborm.exceptions.MappingException;

public final class AnnotationUtils {
	
	public static <T, K> boolean validate(Class<T> clazz) {
		// Ensure class is flagged to be mapped to the database in the first place.
		if (clazz.getAnnotation(Mapped.class) == null)
			throw new ClassNotMappedException(
					"Class " + clazz.getSimpleName() + " is not annotated with the @Mapped annotation");

		// Ensure there is only one @Id annotated field.
		List<Field> idFields = findAnnotatedFields(clazz, Id.class);
		if (idFields.isEmpty())
			throw new MappingException("Class " + clazz.getName() + " has no @Id annotation.");
		if (idFields.size() > 1)
			throw new MappingException("Class " + clazz.getName()
					+ " has too many @Id annotations. Composite keys are not supported at this time.");

		return true;
	}
	
	public static List<Field> findAnnotatedFields(Class<?> clazz, Class<? extends Annotation> annotation) {
		List<Field> fields = new ArrayList<>();
		Class<?> c = clazz;
		while (c != null && c != Object.class) {
			for (Field f : c.getDeclaredFields()) {
				f.setAccessible(true);
				if (f.isAnnotationPresent(annotation))
					fields.add(f);
			}
			c = c.getSuperclass();
		}

		return fields;
	}
	
	public static String findTableName(Class<?> clazz) {
		if (!clazz.isAnnotationPresent(Mapped.class))
			throw new MappingException("Class " + clazz.getName() + " is not annotated with @Mapped");
		Mapped mappedAnnotation = clazz.getAnnotation(Mapped.class);
		String tableName = "".equals(mappedAnnotation.table())
				? clazz.getSimpleName().toLowerCase()
				: mappedAnnotation.table();
		return tableName;
	}
	
	public static String findColumnName(Field field) {
		return field.getAnnotation(Column.class).name().equals("") 
				? field.getName().toLowerCase()
				: field.getAnnotation(Column.class).name();
	}
}
