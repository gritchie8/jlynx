package com.github.jlynx;

import org.apache.commons.beanutils.PropertyUtils;
import org.slf4j.LoggerFactory;

import java.beans.PropertyDescriptor;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

/**
 * Bean utils used by jLynx.
 */
class BeanUtil {

    private final static Map<Class<?>, Object[]> cache = new HashMap<>();

    private BeanUtil() {
    }

    /*private static String decapitalize(String fieldName) {
        return Introspector.decapitalize(fieldName);
    }*/

    static Map<String, Object> describe(Object target) {

        PropertyDescriptor[] propertyDescriptors = PropertyUtils.getPropertyDescriptors(target);

        Map<String, Object> retMap = new HashMap<>();

        for (PropertyDescriptor propDescriptor : propertyDescriptors) {
            try {
                String propName = propDescriptor.getName();
                if (PropertyUtils.isReadable(target, propName)
                        && PropertyUtils.isWriteable(target, propName)) {

                    boolean annotationValid = true;
                    Method readMethod = PropertyUtils.getReadMethod(propDescriptor);
                    if (readMethod.isAnnotationPresent(Column.class)) {
                        Column column = readMethod.getAnnotation(Column.class);
                        annotationValid = column.include();
                    }

                    if (annotationValid)
                        retMap.put(propName, PropertyUtils.getSimpleProperty(target, propDescriptor.getName()));
                }
            } catch (Throwable t) {
                t.printStackTrace();
            }
        }

        return retMap;
    }

    static Class<?> getType(String property, Object target) {

        if (PropertyUtils.isWriteable(target, property) && PropertyUtils.isReadable(target, property)) {
            try {
                return PropertyUtils.getPropertyType(target, property);
            } catch (Exception e) {
                e.printStackTrace();
                throw new RuntimeException("Invalid bean property");
            }
        } else
            throw new RuntimeException("Invalid bean property");

    }

    /**
     * This method performs in a case-insensitive manner
     */
    static Object getValue(String property, Object target) {

        PropertyDescriptor[] propDescriptors = PropertyUtils.getPropertyDescriptors(target);
        // needed as databases often return uppercase fields
        for (PropertyDescriptor propDescriptor : propDescriptors) {
            if (propDescriptor.getName().equalsIgnoreCase(property)) {
                property = propDescriptor.getName();
                break;
            } else if (propDescriptor.getReadMethod() != null) {
                Column column = propDescriptor.getReadMethod().getAnnotation(Column.class);
                if (column != null && column.value().equalsIgnoreCase(property))
                    property = propDescriptor.getName();

            }
        }

        try {
            // apache util replaces custom method in jlynx v1.9
            return PropertyUtils.getSimpleProperty(target, property);
        } catch (Exception e) {
            LoggerFactory.getLogger(BeanUtil.class).warn(e.getMessage());
            return null;
        }

    }

    static void setValueFromString(Object bean, String property, String value) {
        try {
            if (PropertyUtils.isWriteable(bean, property)) {
                Class<?> propType = PropertyUtils.getPropertyType(bean, property);
                if (propType.isAssignableFrom(String.class))
                    PropertyUtils.setSimpleProperty(bean, property, value);
                else if (propType.isAssignableFrom(java.sql.Date.class) || propType.isAssignableFrom(java.sql.Timestamp.class)) {
                    Object value2 = propType.getDeclaredConstructor(long.class).newInstance(Long.parseLong(value));
                    PropertyUtils.setSimpleProperty(bean, property, value2);
                } else {
                    Object value2 = propType.getDeclaredConstructor(String.class).newInstance(value);
                    PropertyUtils.setSimpleProperty(bean, property, value2);
                }
            }
        } catch (Exception throwable) {
            LoggerFactory.getLogger(BeanUtil.class).error("#setBean - could not set simple property " +
                    bean.getClass().getSimpleName() +
                    "#" + property + " - " + throwable.getClass().getSimpleName()
                    + " - " + throwable.getMessage());
        }
    }

    private static void setPropertyIgnoreCase(Object bean, String property, Object value) {
        String beanProperty = null;
        // needed as databases often return uppercase fields
        for (PropertyDescriptor propDescriptor : PropertyUtils.getPropertyDescriptors(bean))
            if (propDescriptor.getName().equalsIgnoreCase(property)) {
                beanProperty = propDescriptor.getName();
                break;
            }

        //todo handle Column annotation
        if (beanProperty == null) {
            for (PropertyDescriptor propDescriptor : PropertyUtils.getPropertyDescriptors(bean)) {
                Method readMethod = propDescriptor.getReadMethod();
                if (readMethod.isAnnotationPresent(Column.class)
                        && readMethod.getAnnotation(Column.class).value().equalsIgnoreCase(property)) {
                    beanProperty = propDescriptor.getName();
                    break;
                }
            }

        }


        try {
            // apache util replaces custom method in jlynx v1.9
            PropertyUtils.setSimpleProperty(bean, beanProperty, value);
        } catch (Exception e) {
            LoggerFactory.getLogger(BeanUtil.class).error(e.getMessage(), e);
        }
    }

    /**
     * This method performs in a case-insensitive manner
     */
    static void setValue(String property, Object target, Object value) {
        setPropertyIgnoreCase(target, property, value);
    }

}
