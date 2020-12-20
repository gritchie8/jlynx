package com.github.jlynx;

import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.lang.reflect.Field;
import java.sql.Timestamp;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * Bean utilities used by jLynx.
 */
class BeanUtil {

    private final static Map<Class<?>, Field[]> cache = new HashMap<>();

    static Field[] getFields(Class<?> targetClass) {
        cache.computeIfAbsent(targetClass, Class::getDeclaredFields);
        return cache.get(targetClass);
    }

    private BeanUtil() {
    }

    static Map<String, Object> describe(Object target) {

        Map<String, Object> retMap = new HashMap<>();

        Arrays.stream(getFields(target.getClass())).filter(field -> field.trySetAccessible()).forEach(field -> {

            try {
                if (!field.isAnnotationPresent(Column.class) || field.getAnnotation(Column.class).include())
                    retMap.put(field.getName(), field.get(target));
            } catch (IllegalAccessException e) {
                LoggerFactory.getLogger("jlynx").error(e.getMessage(), e);
            }

        });

        return retMap;
    }

    static Class<?> getType(String property, Object target) {

        Field field = getFieldIgnoreCase(target, property);
        return field.getType();
    }

    /**
     * This method performs in a case-insensitive manner
     */
    static Object getValue(String property, Object target) {

        Field fieldToGet = getFieldIgnoreCase(target, property);

        try {
            return fieldToGet.get(target);
        } catch (NullPointerException | IllegalAccessException e) {
            e.printStackTrace();
        }

        return null;

    }

    static void setValueFromString(Object bean, String property, String value) {

        for (Field field : getFields(bean.getClass())) {
            if (field.getName().equalsIgnoreCase(property))
                try {

                    Class<?> typeClass = field.getType();

                    if (typeClass == String.class)
                        field.set(bean, value);
                    else if (typeClass == Integer.class)
                        field.set(bean, Integer.parseInt(value));
                    else if (typeClass == Long.class)
                        field.set(bean, Long.parseLong(value));
                    else if (typeClass == java.sql.Date.class || typeClass == java.sql.Timestamp.class) {
                        long time = Long.parseLong(value);
                        Object dateVal = typeClass.getDeclaredConstructor(long.class).newInstance(time);
                        field.set(bean, dateVal);
                    } else
                        field.set(bean, typeClass.getDeclaredConstructor(String.class).newInstance(value));

                    return;
                } catch (Throwable e) {
                    LoggerFactory.getLogger("jlynx").error(e.getMessage());
                }
        }
    }

    /**
     * This method performs in a case-insensitive manner
     */
    static void setValue(String propertyOrColumn, Object target, Object value) {

        if (value == null || value.getClass().getPackage().getName().startsWith("java") || value instanceof InputStream) {

            Field fieldToSet = getFieldIgnoreCase(target, propertyOrColumn);

            if (fieldToSet == null)
                LoggerFactory.getLogger("jlynx").warn(target.getClass().getName()
                        + " - no property found for database column: " + propertyOrColumn);
            else if (fieldToSet.trySetAccessible())
                try {
                    if (value instanceof java.sql.Timestamp &&
                            fieldToSet.getType().getCanonicalName().equalsIgnoreCase("java.sql.Date")) {
                        long time = ((Timestamp) value).getTime();
                        fieldToSet.set(target, new java.sql.Date(time));
                    } else if (value == null ||
                            fieldToSet.getType().isPrimitive() ||
                            fieldToSet.getType().isAssignableFrom(value.getClass()))
                        fieldToSet.set(target, value);


                } catch (IllegalAccessException | IllegalArgumentException e) {
                    LoggerFactory.getLogger("jlynx").error(e.getMessage(), e);
                }

        } else
            LoggerFactory.getLogger("jlynx").warn(propertyOrColumn + " could not be set, value not an expected type");

    }

    private static Field getFieldIgnoreCase(Object bean, String fieldOrColumn) {
        Field field1 = null;
        for (Field fieldToCheck : BeanUtil.getFields(bean.getClass()))
            if (fieldToCheck.getName().equalsIgnoreCase(fieldOrColumn)) {
                field1 = fieldToCheck;
                break;
            } else if (fieldToCheck.isAnnotationPresent(Column.class)
                    && fieldOrColumn.equalsIgnoreCase(fieldToCheck.getAnnotation(Column.class).value())) {
                field1 = fieldToCheck;
                break;
            }
        return field1;
    }

}