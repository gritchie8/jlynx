package com.googlecode.jlynx;

import java.beans.Introspector;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.sql.Date;
import java.sql.Timestamp;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
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
	private static Logger logger = LoggerFactory.getLogger();
	private static boolean _finestOk = logger.isLoggable(Level.FINEST);

	private static Date convertDate(String dtStr) {

		if (_finestOk)
			logger.finest("Setting date using pattern: " + Config.datePattern);
		SimpleDateFormat format = new SimpleDateFormat(Config.datePattern);
		try {
			return new Date(format.parse(dtStr).getTime());
		} catch (ParseException e) {
			logger.severe("Trouble setting Date! " + dtStr);
			return null;
		}
	}

	private static String convertDate(java.sql.Date dt) {
		SimpleDateFormat format = new SimpleDateFormat(Config.datePattern);
		return format.format(dt);
		// return "";
	}

	private static Timestamp convertTimestamp(String dtStr) {

		String pattern = (dtStr.length() > 10) ? Config.tsPattern
				: Config.datePattern;
		if (_finestOk)
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

	static String decapitalize(String fieldName) {
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
			if (_finestOk)
				logger.finest(method + " ==> " + m.getReturnType() + " ==> "
						+ m.getReturnType().isArray());

			if (m.getReturnType().isArray() || m.getReturnType().isInterface()
					|| m.getReturnType().equals(void.class)
					|| m.getReturnType().equals(Class.class)
					|| m.getParameterTypes().length > 0) {
				if (_finestOk)
					logger.finest("Removing a List or Array field! " + method);
				continue;
			}

			if (method.indexOf("get") == 0) {
				retList.add(method.substring(3, method.length()));
			} else if (method.indexOf("is") == 0) {
				retList.add(method.substring(2, method.length()));
			}

		}

		for (String prop : retList) {
			prop = decapitalize(prop);
			retMap.put(prop, getValue(prop, target));
		}
		retList = null;
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

		for (int i = 0; i < methods.length; i++) {

			if (property.equalsIgnoreCase(methods[i].getName())) {

				Class<?>[] paramClass = methods[i].getParameterTypes();
				if (paramClass.length == 1) {
					return paramClass[0];
				}

			}

		}

		return ret;

	}

	/**
	 * This method performs in a case-insensitive manner
	 */
	static Object getValue(String property, Object target) {

		if (_finestOk)
			logger.finest("Seeking property=" + property + " from " + target);

		String get = "get" + property;
		String is = "is" + property;

		Method[] methods = (Method[]) cache.get(target.getClass());
		if (methods == null) {
			methods = target.getClass().getMethods();
			cache.put(target.getClass(), methods);
		}

		for (int i = 0; i < methods.length; i++) {

			if (get.equalsIgnoreCase(methods[i].getName())
					|| is.equalsIgnoreCase(methods[i].getName())) {

				if (_finestOk)
					logger.finest("methodName=" + methods[i].getName());

				try {
					return methods[i].invoke(target, (Object[]) null);
				} catch (IllegalArgumentException ex) {
					// ex.printStackTrace();
				} catch (IllegalAccessException ex) {
					// ex.printStackTrace();
				} catch (InvocationTargetException ex) {
					// ex.printStackTrace();
				}

			}
		}
		return null;

	}

	/**
	 * This method performs in a case-insensitive manner
	 */
	public static void setValue(String property, Object target, Object value) {

		String test = property.toLowerCase() + "."
				+ target.getClass().getName();
		if (_finestOk)
			logger.finest(test);
		if (Config.COL_MAPPING.containsKey(test)) {
			// logger.debug("\n\n\nCheck for correct column/property!");
			// property = (String) Config.COL_MAPPING.get(test);
			if (_finestOk)
				logger.finest("Using property :: " + property);
		}

		property = "set" + property;

		if (_finestOk)
			logger.finest("setting value=" + value);

		Method[] methods = (Method[]) cache.get(target.getClass());
		if (methods == null) {
			methods = target.getClass().getMethods();
			cache.put(target.getClass(), methods);
		}

		for (int i = 0; i < methods.length; i++) {

			if (property.equalsIgnoreCase(methods[i].getName())) {

				Class<?>[] paramClass = methods[i].getParameterTypes();
				if (paramClass.length == 1) {

					Class<?> cls = paramClass[0];

					if (_finestOk)
						logger.finest("type=" + cls);
					try {

						methods[i].invoke(target, new Object[] { value });

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

									methods[i].invoke(target,
											new Object[] { int2 });

								} else if (value instanceof String[]
										&& clsName
												.equals("[Ljava.lang.Integer;")) {

									Integer[] int2 = new Integer[va.length];
									for (int i2 = 0; i2 < va.length; i2++) {
										int2[i2] = new Integer(va[i2]);
									}

									methods[i].invoke(target,
											new Object[] { int2 });

								}

								return;

							} catch (Exception e3) {
								if (_finestOk)
									logger.finest("Could not set array");
								return;
							}
						}

						Constructor<?> ctor;
						try {

							ctor = cls.getConstructor(new Class[] { value
									.getClass() });

							if (_finestOk)
								logger.finest("ctor = " + ctor);

							Object newVal = ctor
									.newInstance(new Object[] { value });

							methods[i].invoke(target, new Object[] { newVal });

						} catch (Exception e2) {

							if (cls.getName().equals("java.sql.Timestamp")) {
								if (value instanceof String) {
									try {
										methods[i]
												.invoke(target,
														new Object[] { convertTimestamp(value
																.toString()) });

									} catch (Exception e3) {
										e3.printStackTrace();
									}
								}
							} else if (cls.getName().equals("java.sql.Date")) {
								if (value instanceof String) {
									try {
										methods[i]
												.invoke(target,
														new Object[] { convertDate(value
																.toString()) });

									} catch (Exception e3) {
										e3.printStackTrace();
									}
								}
							}
						}
					}

				}

			}
		}

	}

	static String toJSFormBind(String form, Object bean) {
		StringBuffer result = new StringBuffer("if( document." + form + " ){\n");
		Map<?, ?> pairs;
		if (bean instanceof Map)
			pairs = (Map<?, ?>) bean;
		else
			pairs = describe(bean);

		Iterator<?> iter = pairs.keySet().iterator();
		while (iter.hasNext()) {

			Object obj = iter.next();
			Object value = pairs.get(obj);
			if (value != null) {
				result.append("  try { document.").append(form).append(".")
						.append(obj.toString()).append(".value = '")
						.append(value.toString()).append("'; } catch(e){}\n");
			}

		}
		result.append("}");

		return result.toString();
	}

	static String toJSON(Object bean) {

		if (bean == null)
			return null;

		StringBuffer result = new StringBuffer("{\n");

		Map<?, ?> pairs;
		if (bean instanceof Map)
			pairs = (Map<?, ?>) bean;
		else
			pairs = describe(bean);

		Iterator<?> iter = pairs.keySet().iterator();
		while (iter.hasNext()) {

			Object obj = iter.next();

			result.append("\t\"").append(obj).append("\": ");
			Object value = pairs.get(obj);
			if (value != null) {
				// adjusted for v1.6.2
				if (value instanceof String) {
					value = StringUtils.replace(value.toString(), "\\", "\\\\");
					value = StringUtils.replace(value.toString(), "\"", "\\\"");
					value = StringUtils.replace(value.toString(), "\'", "\\\'");
					value = StringUtils.replace(value.toString(), "\n", "\\n");
					value = StringUtils.replace(value.toString(), "\r", "\\r");
					value = StringUtils.replace(value.toString(), "\b", "\\b");
					value = StringUtils.replace(value.toString(), "\t", "\\t");
				} else if (value instanceof Timestamp) {
					long time = ((Timestamp) value).getTime();
					value = "" + time + "";
				} else if (value instanceof Date) {
					value = convertDate((Date) value);
				}

				result.append("\"").append(value).append("\"");
			} else
				result.append(value);

			if (iter.hasNext())
				result.append(",\n");
			else
				result.append("\n}");

		}

		return result.toString();
	}

	static String toXml(List<?> list, String element) {
		StringBuffer xml = new StringBuffer();
		Iterator<?> iter = list.iterator();
		while (iter.hasNext()) {
			xml.append(toXml(iter.next(), element)).append("\n");
		}
		return xml.toString();
	}

	static String toXml(Object bean, String element) {

		StringBuffer sb = new StringBuffer("<?xml version=\"1.0\"?>\n");

		if (bean == null)
			return null;
		else {

			String className = bean.getClass().getName();
			if (element == null)
				element = className.substring(className.lastIndexOf(".") + 1)
						.toLowerCase();
			sb.append("<").append(element).append(" class=\"")
					.append(className).append("\">\n");

			Map<?, ?> props = BeanUtils.describe(bean);
			Iterator<?> keys = props.keySet().iterator();
			while (keys.hasNext()) {
				String key = keys.next().toString();
				Object value = props.get(key);
				if (value == null)
					value = "";
				sb.append(" <").append(key).append(">").append(value)
						.append("</").append(key).append(">\n");
			}

			sb.append("</").append(element).append(">");

		}
		return sb.toString();
	}

	private BeanUtils() {
	}

}
