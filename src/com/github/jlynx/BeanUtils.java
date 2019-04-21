package com.github.jlynx;

import java.beans.Introspector;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.sql.Date;
import java.sql.Timestamp;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Bean utils used by jLynx.
 */
class BeanUtils {

    private static Map<Class<?>, Object[]> cache = new HashMap<Class<?>, Object[]>();
    private static Level finest = Level.FINEST;
    private static Logger logger = Logger.getAnonymousLogger();

    private BeanUtils() {
    }

    private static Date convertDate(String dtStr) {

        SimpleDateFormat format = new SimpleDateFormat("MM/dd/yyyy");

        try {
            return new Date(format.parse(dtStr).getTime());
        } catch (ParseException e) {
            logger.severe("Trouble setting Date! " + dtStr);
            return null;
        }
    }

    private static Timestamp convertTimestamp(String dtStr) {

        String pattern = (dtStr.length() > 10) ? "MM/dd/yyyy hh:mm:ss.sss" : "MM/dd/yyyy";
        if (logger.isLoggable(Level.FINEST))
            logger.finest("Setting timestamp using pattern: " + pattern);

        SimpleDateFormat format = new SimpleDateFormat(pattern);

        try {
            long time = format.parse(dtStr).getTime();
            return new Timestamp(time);
        } catch (ParseException e) {
            logger.severe("Touble setting Timestamp! " + dtStr);
            return null;
        }
    }

    private static String decapitalize(String fieldName) {
        return Introspector.decapitalize(fieldName);
    }

    /**
     * This method performs in a case-sensitive manner
     *
     * @param target
     * @return Map
     */
    static Map<String, Object> describe(Object target) {

        Map<String, Object> retMap = new HashMap<String, Object>();
        Set<String> retList = new TreeSet<String>();
        Method[] methods = (Method[]) cache.get(target.getClass());

        if (methods == null) {
            methods = target.getClass().getMethods();
            cache.put(target.getClass(), methods);
        }

        for (Method m : methods) {

            String method = m.getName();
            if (logger.isLoggable(finest))
                logger.finest(method + " ==> " + m.getReturnType() + " ==> "
                        + m.getReturnType().isArray());

            if (m.getReturnType().isArray() || m.getReturnType().isInterface()
                    || m.getReturnType().equals(void.class)
                    || m.getReturnType().equals(Class.class)
                    || m.getParameterTypes().length > 0) {
                if (logger.isLoggable(finest))
                    logger.finest("Removing a List or Array field! " + method);
                continue;
            }

            if (method.indexOf("get") == 0) {
                retList.add(method.substring(3));
            } else if (method.indexOf("is") == 0) {
                retList.add(method.substring(2));
            }

        }

        for (String prop : retList) {
            prop = decapitalize(prop);
            retMap.put(prop, getValue(prop, target));
        }

        return retMap;
    }

    static Class<?> getType(String property, Object target) {

        Class<?> ret = Object.class;
        property = "set" + property;

        Method[] methods = (Method[]) cache.get(target.getClass());
        if (methods == null) {
            methods = target.getClass().getMethods();
            cache.put(target.getClass(), methods);
        }

        for (Method method : methods)
            if (property.equalsIgnoreCase(method.getName())) {
                Class<?>[] paramClass = method.getParameterTypes();
                if (paramClass.length == 1)
                    return paramClass[0];
            }

        return ret;

    }

    /**
     * This method performs in a case-insensitive manner
     */
    static Object getValue(String property, Object target) {

        if (logger.isLoggable(finest))
            logger.finest("Seeking property=" + property + " from " + target);

        // checking @Column
        for (Field field : target.getClass().getDeclaredFields())
            if (field.isAnnotationPresent(Column.class) && property.equalsIgnoreCase(field.getAnnotation(Column.class).value())) {
                property = field.getName();
                break;
            }

        String get = "get" + property;
        String is = "is" + property;

        Method[] methods = (Method[]) cache.get(target.getClass());
        if (methods == null) {
            methods = target.getClass().getMethods();
            cache.put(target.getClass(), methods);
        }

        for (Method method : methods) {

            if (get.equalsIgnoreCase(method.getName()) || is.equalsIgnoreCase(method.getName())) {

                if (logger.isLoggable(finest))
                    logger.finest("methodName=" + method.getName());

                try {
                    return method.invoke(target, (Object[]) null);
                } catch (IllegalArgumentException ex) {
                    logger.warning(ex.getMessage());
                } catch (IllegalAccessException ex) {
                    logger.warning(ex.getMessage());
                } catch (InvocationTargetException ex) {
                    logger.warning(ex.getMessage());
                }

            }
        }
        return null;

    }

    /**
     * This method performs in a case-insensitive manner
     */
    static void setValue(String property, Object target, Object value) {

        String test = property.toLowerCase() + "." + target.getClass().getName();
        if (logger.isLoggable(finest))
            logger.finest(test);

        property = "set" + property;

        if (logger.isLoggable(finest))
            logger.finest("setting value=" + value);

        Method[] methods = cache.containsKey(target.getClass()) ?
                (Method[]) cache.get(target.getClass()) : target.getClass().getDeclaredMethods();

        for (Method method : methods) {

            if (property.equalsIgnoreCase(method.getName())) {

                Class<?>[] paramClass = method.getParameterTypes();
                if (paramClass.length == 1) {

                    Class<?> cls = paramClass[0];

                    if (logger.isLoggable(finest))
                        logger.finest("type=" + cls);
                    try {

                        method.invoke(target, value);

                    } catch (Exception ex) {

                        // handle Integer and int arrays
                        if (value != null && value.getClass().isArray()) {
                            try {
                                String clsName = cls.getName();
                                String[] va = (String[]) value;
                                if (value instanceof String[]
                                        && clsName.equals("[I")) {

                                    int[] int2 = new int[va.length];
                                    for (int i2 = 0; i2 < va.length; i2++) {
                                        int2[i2] = Integer.parseInt(va[i2]);
                                    }

                                    method.invoke(target, new Object[]{int2});

                                } else if (value instanceof String[]
                                        && clsName
                                        .equals("[Ljava.lang.Integer;")) {

                                    Integer[] int2 = new Integer[va.length];
                                    for (int i2 = 0; i2 < va.length; i2++) {
                                        int2[i2] = new Integer(va[i2]);
                                    }

                                    method.invoke(target, new Object[]{int2});

                                }

                                return;

                            } catch (Exception e3) {
                                if (logger.isLoggable(finest))
                                    logger.finest("Could not set array");
                                return;
                            }
                        }

                        Constructor<?> ctor;
                        try {

                            ctor = cls.getConstructor(value.getClass());

                            if (logger.isLoggable(finest))
                                logger.finest("ctor = " + ctor);

                            Object newVal = ctor.newInstance(value);

                            method.invoke(target, newVal);

                        } catch (Exception e2) {

                            if (cls.getName().equals("java.sql.Timestamp")) {
                                if (value instanceof String) {
                                    try {
                                        method.invoke(target, convertTimestamp(value.toString()));

                                    } catch (Exception e3) {
                                        e3.printStackTrace();
                                    }
                                }
                            } else if (cls.getName().equals("java.sql.Date")) {
                                if (value instanceof String) {
                                    try {
                                        method.invoke(target, convertDate(value.toString()));

                                    } catch (Exception e3) {
                                        e3.printStackTrace();
                                    }
                                }
                            }
                        }
                        break;
                    }
                }
                break;
            }
        }
    }

}
