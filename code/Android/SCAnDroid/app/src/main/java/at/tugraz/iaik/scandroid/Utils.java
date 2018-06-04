package at.tugraz.iaik.scandroid;

import android.support.annotation.Nullable;
import android.util.Log;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * Created by Gerald Palfinger on 27.10.17.
 */

public class Utils {
    private static final String TAG = Utils.class.getName();

    public static boolean isWrapperType(Class<?> clazz) {
        boolean isWrapper = clazz.equals(Boolean.class) ||
                clazz.equals(Integer.class) ||
                clazz.equals(Character.class) ||
                clazz.equals(Byte.class) ||
                clazz.equals(Short.class) ||
                clazz.equals(Double.class) ||
                clazz.equals(Long.class) ||
                clazz.equals(Float.class) ||
                clazz.equals(Void.class);

        if (!isWrapper && clazz.getCanonicalName() != null) {
            isWrapper = clazz.getCanonicalName().toLowerCase().contains("java.lang.string") ||
                    clazz.getCanonicalName().toLowerCase().contains("int") ||
                    clazz.getCanonicalName().toLowerCase().contains("float") ||
                    clazz.getCanonicalName().toLowerCase().contains("boolean") ||
                    clazz.getCanonicalName().toLowerCase().contains("byte") ||
                    clazz.getCanonicalName().toLowerCase().contains("short") ||
                    clazz.getCanonicalName().toLowerCase().contains("double") ||
                    clazz.getCanonicalName().toLowerCase().contains("char") ||
                    clazz.getCanonicalName().toLowerCase().contains("long");
        }
        return isWrapper;
    }

    /**
     * Taken from Android Java Source code API v28. Can be replaced with Class.getTypeName() if targeting Android 8 and higher
     * <p>
     * Return an informative string for the name of this type.
     *
     * @return an informative string for the name of this type
     * @since 1.8
     */
    public static String getTypeName(Class<?> cl) {
        if (cl.isArray()) {
            try {
                int dimensions = 0;
                while (cl.isArray()) {
                    dimensions++;
                    cl = cl.getComponentType();
                }
                StringBuilder sb = new StringBuilder();
                sb.append(cl.getName());
                for (int i = 0; i < dimensions; i++) {
                    sb.append("[]");
                }
                return sb.toString();
            } catch (Throwable e) { /*FALLTHRU*/ }
        }
        return cl.getName();
    }

    //Source: https://stackoverflow.com/questions/16427319/cast-object-to-array
    public static Object[] convertToObjectArray(Object array) {
        Class arrayType = array.getClass().getComponentType();
        if (arrayType.isPrimitive()) {
            List arrayList = new ArrayList();
            int length = Array.getLength(array);
            for (int i = 0; i < length; ++i) {
                arrayList.add(Array.get(array, i));
            }
            return arrayList.toArray();
        } else {
            return (Object[]) array;
        }
    }

    @Nullable
    static Object getDefaultParameter(Class<?> aClass) {
        String className = aClass.getName();
        Random random = new Random();
        if (className.equals(int.class.getName())) {
            return random.nextInt(100000);
        } else if (className.equals(int[].class.getName())) {
            return new int[]{random.nextInt(100000), random.nextInt(100000)};
        } else if (className.equals(long.class.getName())) {
            return (long)random.nextInt(100000);
        } else if (className.equals(long[].class.getName())) {
            return new long[]{random.nextInt(100000), random.nextInt(100000)};
        } else if (className.equals(String.class.getName()) || className.equals(CharSequence.class.getName())) {
            return "default";
        } else if (className.equals(String[].class.getName())) {
            return new String[]{"default1", "default2"};
        } else if (className.equals(CharSequence.class.getName())) {
            return new CharSequence[]{"default1", "default2"};
        } else if (className.equals(float.class.getName())) {
            return random.nextFloat();
        } else if (className.equals(float[].class.getName())) {
            return new float[]{1.02f, 13.34f};
        } else if (className.equals(double.class.getName())) {
            return random.nextDouble();
        } else if (className.equals(double[].class.getName())) {
            return new double[]{1.02, 13.34};
        } else if (className.equals(boolean.class.getName())) {
            return true;
        } else if (className.equals(boolean[].class.getName())) {
            return new boolean[]{true, false};
        } else if (className.equals(char.class.getName())) {
            return 'd';
        } else if (className.equals(char[].class.getName())) {
            return new char[]{'d', 'e'};
        } else if (className.equals(short.class.getName())) {
            return (short) 1337;
        } else if (className.equals(short[].class.getName())) {
            return new short[]{(short) 1337, (short) 1335};
        } else if (className.equals(byte.class.getName())) {
            return (byte) 1337;
        } else if (className.equals(byte[].class.getName())) {
            return new byte[]{(byte) 1337, (byte) 1335};
        } else {
            Log.w(TAG, "No default found for className = " + className);
            return null;
        }
    }

    // Source: Apache Commons
    /**
     * Maps primitive <code>Class</code>es to their corresponding wrapper <code>Class</code>.
     */
    private static final Map primitiveWrapperMap = new HashMap();

    static {
        primitiveWrapperMap.put(Boolean.TYPE, Boolean.class);
        primitiveWrapperMap.put(Byte.TYPE, Byte.class);
        primitiveWrapperMap.put(Character.TYPE, Character.class);
        primitiveWrapperMap.put(Short.TYPE, Short.class);
        primitiveWrapperMap.put(Integer.TYPE, Integer.class);
        primitiveWrapperMap.put(Long.TYPE, Long.class);
        primitiveWrapperMap.put(Double.TYPE, Double.class);
        primitiveWrapperMap.put(Float.TYPE, Float.class);
        primitiveWrapperMap.put(Void.TYPE, Void.TYPE);
    }

    /**
     * Maps wrapper <code>Class</code>es to their corresponding primitive types.
     */
    private static final Map wrapperPrimitiveMap = new HashMap();

    static {
        for (Iterator it = primitiveWrapperMap.keySet().iterator(); it.hasNext(); ) {
            Class primitiveClass = (Class) it.next();
            Class wrapperClass = (Class) primitiveWrapperMap.get(primitiveClass);
            if (!primitiveClass.equals(wrapperClass)) {
                wrapperPrimitiveMap.put(wrapperClass, primitiveClass);
            }
        }
    }

    /**
     * <p>Checks if one <code>Class</code> can be assigned to a variable of
     * another <code>Class</code>.</p>
     * <p>
     * <p>Unlike the {@link Class#isAssignableFrom(java.lang.Class)} method,
     * this method takes into account widenings of primitive classes and
     * <code>null</code>s.</p>
     * <p>
     * <p>Primitive widenings allow an int to be assigned to a long, float or
     * double. This method returns the correct result for these cases.</p>
     * <p>
     * <p><code>Null</code> may be assigned to any reference type. This method
     * will return <code>true</code> if <code>null</code> is passed in and the
     * toClass is non-primitive.</p>
     * <p>
     * <p>Specifically, this method tests whether the type represented by the
     * specified <code>Class</code> parameter can be converted to the type
     * represented by this <code>Class</code> object via an identity conversion
     * widening primitive or widening reference conversion. See
     * <em><a href="http://java.sun.com/docs/books/jls/">The Java Language Specification</a></em>,
     * sections 5.1.1, 5.1.2 and 5.1.4 for details.</p>
     *
     * @param cls        the Class to check, may be null
     * @param toClass    the Class to try to assign into, returns false if null
     * @param autoboxing whether to use implicit autoboxing/unboxing between primitives and wrappers
     * @return <code>true</code> if assignment possible
     * @since 2.5
     */
    static boolean isAssignable(Class cls, Class toClass, boolean autoboxing) {
        if (toClass == null) {
            return false;
        }
        // have to check for null, as isAssignableFrom doesn't
        if (cls == null) {
            return !(toClass.isPrimitive());
        }
        //autoboxing:
        if (autoboxing) {
            if (cls.isPrimitive() && !toClass.isPrimitive()) {
                cls = primitiveToWrapper(cls);
                if (cls == null) {
                    return false;
                }
            }
            if (toClass.isPrimitive() && !cls.isPrimitive()) {
                cls = wrapperToPrimitive(cls);
                if (cls == null) {
                    return false;
                }
            }
        }
        if (cls.equals(toClass)) {
            return true;
        }
        if (cls.isPrimitive()) {
            if (!toClass.isPrimitive()) {
                return false;
            }
            if (Integer.TYPE.equals(cls)) {
                return Long.TYPE.equals(toClass)
                        || Float.TYPE.equals(toClass)
                        || Double.TYPE.equals(toClass);
            }
            if (Long.TYPE.equals(cls)) {
                return Float.TYPE.equals(toClass)
                        || Double.TYPE.equals(toClass);
            }
            if (Boolean.TYPE.equals(cls)) {
                return false;
            }
            if (Double.TYPE.equals(cls)) {
                return false;
            }
            if (Float.TYPE.equals(cls)) {
                return Double.TYPE.equals(toClass);
            }
            if (Character.TYPE.equals(cls)) {
                return Integer.TYPE.equals(toClass)
                        || Long.TYPE.equals(toClass)
                        || Float.TYPE.equals(toClass)
                        || Double.TYPE.equals(toClass);
            }
            if (Short.TYPE.equals(cls)) {
                return Integer.TYPE.equals(toClass)
                        || Long.TYPE.equals(toClass)
                        || Float.TYPE.equals(toClass)
                        || Double.TYPE.equals(toClass);
            }
            if (Byte.TYPE.equals(cls)) {
                return Short.TYPE.equals(toClass)
                        || Integer.TYPE.equals(toClass)
                        || Long.TYPE.equals(toClass)
                        || Float.TYPE.equals(toClass)
                        || Double.TYPE.equals(toClass);
            }
            // should never get here
            return false;
        }
        return toClass.isAssignableFrom(cls);
    }

    /**
     * <p>Converts the specified primitive Class object to its corresponding
     * wrapper Class object.</p>
     * <p>
     * <p>NOTE: From v2.2, this method handles <code>Void.TYPE</code>,
     * returning <code>Void.TYPE</code>.</p>
     *
     * @param cls the class to convert, may be null
     * @return the wrapper class for <code>cls</code> or <code>cls</code> if
     * <code>cls</code> is not a primitive. <code>null</code> if null input.
     * @since 2.1
     */
    private static Class primitiveToWrapper(Class cls) {
        Class convertedClass = cls;
        if (cls != null && cls.isPrimitive()) {
            convertedClass = (Class) primitiveWrapperMap.get(cls);
        }
        return convertedClass;
    }

    /**
     * <p>Converts the specified wrapper class to its corresponding primitive
     * class.</p>
     * <p>
     * <p>This method is the counter part of <code>primitiveToWrapper()</code>.
     * If the passed in class is a wrapper class for a primitive type, this
     * primitive type will be returned (e.g. <code>Integer.TYPE</code> for
     * <code>Integer.class</code>). For other classes, or if the parameter is
     * <b>null</b>, the return value is <b>null</b>.</p>
     *
     * @param cls the class to convert, may be <b>null</b>
     * @return the corresponding primitive type if <code>cls</code> is a
     * wrapper class, <b>null</b> otherwise
     * @see #primitiveToWrapper(Class)
     * @since 2.4
     */
    private static Class wrapperToPrimitive(Class cls) {
        return (Class) wrapperPrimitiveMap.get(cls);
    }
    //End of Apache Commons

    static boolean isCollectionType(Object returnValue) {
        if (returnValue.getClass().isArray()) {
            return true;
        } else if (returnValue instanceof Collection<?>) {
            return true;
        } else if (returnValue instanceof Map<?, ?>) {
            return true;
        }
        return false;
    }
}