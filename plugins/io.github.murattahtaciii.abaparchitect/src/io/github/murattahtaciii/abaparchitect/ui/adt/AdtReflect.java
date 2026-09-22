package io.github.murattahtaciii.abaparchitect.ui.adt;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public final class AdtReflect {

    private AdtReflect() {
    }

    public static Class<?> load(String className) throws AdtException {
        try {
            return Class.forName(className);
        } catch (ClassNotFoundException e) {
            throw new AdtException("ADT sınıfı bulunamadı: " + className
                    + " (ADT/ABAP Development Tools kurulu mu?)", e);
        }
    }

    public static boolean exists(String className) {
        try {
            Class.forName(className);
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

    public static Object callStatic(String className, String method, Class<?>[] parameterTypes, Object... args)
            throws AdtException {
        try {
            Class<?> type = load(className);
            if ("<init>".equals(method)) {
                Constructor<?> constructor = type.getConstructor(parameterTypes);
                setAccessible(constructor);
                return constructor.newInstance(args);
            }
            Method m = type.getMethod(method, parameterTypes);
            setAccessible(m);
            return m.invoke(null, args);
        } catch (InvocationTargetException e) {
            throw translate(className + "." + method, e.getCause());
        } catch (AdtException e) {
            throw e;
        } catch (Exception e) {
            throw new AdtException(className + "." + method + " çağrılamadı: " + e, e);
        }
    }

    public static Object call(Object target, String method, Class<?>[] parameterTypes, Object... args)
            throws AdtException {
        if (target == null) {
            throw new AdtException("ADT nesnesi yok (null): " + method);
        }
        try {
            Method m = target.getClass().getMethod(method, parameterTypes);
            setAccessible(m);
            return m.invoke(target, args);
        } catch (InvocationTargetException e) {
            throw translate(target.getClass().getSimpleName() + "." + method, e.getCause());
        } catch (Exception e) {
            throw new AdtException(target.getClass().getSimpleName() + "." + method + " çağrılamadı: " + e, e);
        }
    }

    private static void setAccessible(java.lang.reflect.Executable executable) {
        try {
            executable.setAccessible(true);
        } catch (Exception ignored) {
            // erişim zaten mümkünse sessizce geçilir
        }
    }

    public static Object call(Object target, String method) throws AdtException {
        return call(target, method, new Class<?>[0]);
    }

    public static String stringResult(Object target, String method) throws AdtException {
        Object value = call(target, method);
        return value == null ? null : String.valueOf(value);
    }

    private static AdtException translate(String where, Throwable cause) {
        String message = cause == null ? "bilinmeyen hata" : cause.getClass().getSimpleName() + ": " + cause.getMessage();
        return new AdtException(where + " -> " + message, cause);
    }
}
