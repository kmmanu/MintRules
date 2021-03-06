package org.mintrules.core;

import org.mintrules.annotation.Value;
import org.mintrules.api.Session;

import java.lang.annotation.Annotation;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static java.lang.String.format;

/**
 * Default implementation of a session.
 */
public class DefaultSession implements Session {

    private Map<String, Object> elements = new HashMap<String, Object>();
    private Map<Class, Class> primitiveTypes = new HashMap<Class, Class>();

    {
        primitiveTypes.put(boolean.class, Boolean.class);
        primitiveTypes.put(byte.class, Byte.class);
        primitiveTypes.put(char.class, Character.class);

        primitiveTypes.put(float.class, Float.class);
        primitiveTypes.put(double.class, Double.class);

        primitiveTypes.put(short.class, Short.class);
        primitiveTypes.put(int.class, Integer.class);
        primitiveTypes.put(long.class, Long.class);
    }

    public DefaultSession() {
        put(this);
    }

    @Override
    public void put(Object value) {
        elements.put(value.getClass().getCanonicalName(), value);
    }

    @Override
    public void put(String name, Object value) {
        elements.put(name, value);
    }

    @Override
    public Object getValue(Class<?> parameterType, Annotation[] annotations) {

        for (int i = 0; i < annotations.length; i++) {
            if (annotations[i].annotationType().equals(Value.class)) {
                String key = ((Value) annotations[i]).value();
                if (!elements.containsKey(key)) {
                    throw new RuntimeException("Couldn't find a parameter with the value '" + key + "'");
                }
                Object value = elements.get(key);
                if (!value.getClass().isAssignableFrom(parameterType)) {
                    throw new RuntimeException("Incompatible types. The parameter with key '" + key + "' requires an " +
                            "instance of " + parameterType.getCanonicalName() + ", but got an instance of " +
                            value.getClass().getCanonicalName());
                }

                return value;
            }
        }


        StringBuilder sb = new StringBuilder(0);
        Map.Entry<String, Object> candidate = null;
        Class<?> actualType = getActualType(parameterType);

        for (Map.Entry<String, Object> entry : elements.entrySet()) {
            if (entry.getValue().getClass().isAssignableFrom(actualType)) {
                if (candidate == null) {
                    candidate = entry;
                } else {
                    if (sb.length() == 0) {
                        sb.append('\'').append(candidate.getKey()).append('\'');
                    }
                    sb.append(", '").append(entry.getKey()).append('\'');
                }
            }
        }

        if (candidate == null) {
            throw new RuntimeException("Couldn't find an instance of " + actualType.getCanonicalName());
        } else if (sb.length() > 0) {
            String message = format("More than one instance of %s was found [%s]", actualType.getCanonicalName(), sb);
            throw new RuntimeException(message);
        }

        return candidate.getValue();
    }

    /**
     * This method transforms primitive type classes to their wrapper types.
     *
     * @param parameterType
     * @return wrapper class type or original type if it was not a primitive type.
     */
    private Class<?> getActualType(Class<?> parameterType) {
        Class<?> actualType;
        if (primitiveTypes.containsKey(parameterType)) {
            actualType = primitiveTypes.get(parameterType);
        } else {
            actualType = parameterType;
        }
        return actualType;
    }

    public Map<String,Object> getElements() {
        return Collections.unmodifiableMap(elements);
    }
}
