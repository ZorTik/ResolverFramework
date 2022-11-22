package me.zort.commons.resolverframework.util;

import com.google.common.reflect.TypeToken;
import org.jetbrains.annotations.Nullable;
import org.reflections.Reflections;
import org.reflections.scanners.Scanner;
import org.reflections.scanners.SubTypesScanner;
import org.reflections.scanners.TypeAnnotationsScanner;

import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collection;

public class ResolverUtil {

    public static <T> Collection<T> cloneList(Collection<T> collection) {
        return new ArrayList(collection);
    }

    public static <T> T blankMethodResult(Object classObject, String methodName, Class<T> resultType) {
        Class<?> clazz = classObject.getClass();
        try {
            Method m = clazz.getMethod(methodName);
            if(!m.getReturnType().equals(resultType)) {
                return null;
            }
            Object o = m.invoke(classObject);
            return (T) o;
        } catch (Exception ex) {
            return null;
        }
    }

    public static Object blankMethodResult(Object classObject, String methodName) {
        Class<?> clazz = classObject.getClass();
        try {
            Method m = clazz.getMethod(methodName);
            return m.invoke(classObject);
        } catch (Exception ex) {
            return null;
        }
    }

    public static <T> Type genericType() {
        return new TypeToken<T>(){}.getType();
    }

    public static Reflections newReflections(String path) {
        return newReflections(path, null);
    }

    public static Reflections newReflections(String path, @Nullable ClassLoader classLoader) {
        Scanner[] scanners = {new TypeAnnotationsScanner(), new SubTypesScanner()};
        return classLoader != null
                ? new Reflections(path, classLoader, scanners)
                : new Reflections(path, scanners);
        /*return classLoader != null
                ? new Reflections(new ConfigurationBuilder()
                .filterInputsBy(new FilterBuilder().includePackage(path))
                .addClassLoaders(classLoader, classLoader.getParent())
                .setScanners(scanners))
                : new Reflections(path, scanners);*/
    }

    public static boolean isInPackage(Class<?> clazz, String packagePath) {
        return isInPackage(clazz.getPackage().getName(), packagePath);
    }

    public static boolean isInPackage(String path, String packagePath) {
        return path.contains(packagePath);
    }

}
