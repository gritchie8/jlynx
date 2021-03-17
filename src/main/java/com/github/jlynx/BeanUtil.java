package com.github.jlynx;

import java.io.InputStream;
import java.lang.reflect.Field;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.LoggerFactory;

/**
 * Bean utilities used by jLynx.
 */
class BeanUtil {

  private final static Map<Class<?>, Field[]> cache = new HashMap<>();

  static Field[] getFields(final Class<?> targetClass) {

    // cache.computeIfAbsent(targetClass, k -> targetClass.getFields());
    cache.computeIfAbsent(targetClass, k -> {

      List<Field> fields = new ArrayList<>();
      Class<?> class1 = targetClass;
      while (class1 != Object.class) {
        for (Field field : class1.getDeclaredFields()) {

          if (!field.trySetAccessible() || field.isAnnotationPresent(Exclude.class) || field.getType().isArray()
              || java.util.Collection.class.isAssignableFrom(field.getType()) || field.getType().isInterface())
            continue;
          else
            fields.add(field);
        }
        class1 = class1.getSuperclass();
      }
      return fields.toArray(new Field[fields.size()]);

    });

    return cache.get(targetClass);
  }

  private BeanUtil() {
  }

  static Map<String, Object> describe(Object target) {

    Map<String, Object> retMap = new HashMap<>();

    Arrays.stream(getFields(target.getClass())).filter(field -> field.canAccess(target)).forEach(field -> {

      try {
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
  static Object getValue(String property, Object target) throws IllegalAccessException {

    Field fieldToGet = getFieldIgnoreCase(target, property);

    try {
      fieldToGet.setAccessible(true);
      return fieldToGet.get(target);
    } catch (IllegalAccessException e) {
      LoggerFactory.getLogger("jlynx").error(e.getMessage(), e);
      throw e;
    }
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
        LoggerFactory.getLogger("jlynx")
            .warn(target.getClass().getSimpleName() + "#" + propertyOrColumn + " - no property exists");
      else if (fieldToSet.trySetAccessible())
        try {
          if (value instanceof java.sql.Timestamp && fieldToSet.getType().getCanonicalName().equals("java.sql.Date")) {
            long time = ((Timestamp) value).getTime();
            fieldToSet.set(target, new java.sql.Date(time));
          } else if (value == null || fieldToSet.getType().isPrimitive()
              || fieldToSet.getType().isAssignableFrom(value.getClass()))
            fieldToSet.set(target, value);
          else
            LoggerFactory.getLogger("jlynx")
                .warn(target.getClass().getSimpleName() + "#" + fieldToSet.getName() + " not set");

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