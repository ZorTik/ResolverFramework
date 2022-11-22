package me.zort.commons.resolverframework;

import com.google.common.annotations.Beta;
import com.google.common.collect.Maps;
import com.google.common.primitives.Primitives;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import me.zort.commons.common.Strings;
import me.zort.commons.resolverframework.boot.AppendPackageSpaces;
import me.zort.commons.resolverframework.boot.EnableDebug;
import me.zort.commons.resolverframework.boot.PackageSpaceAppender;
import me.zort.commons.resolverframework.boot.ResolverApplication;
import me.zort.commons.resolverframework.util.ResolverUtil;
import org.jetbrains.annotations.Nullable;
import org.reflections.Reflections;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.*;
import java.util.stream.Collectors;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class Resolver {

    private static final Logger resolverLogger = LoggerFactory.getLogger(Resolver.class);

    public static Random RANDOM = new Random();

    public static ResolverBuilder builder(String packageSpace) {
        return new ResolverBuilder(packageSpace);
    }

    public static ResolverBuilder builder(Class<?> clazz) {
        return builder(clazz.getPackage());
    }

    public static ResolverBuilder builder(Package p) {
        return builder(p.getName());
    }

    public static Map<Class<?>, me.zort.commons.resolverframework_dev.ResolverService> jar(File jarFile, String rootPackageSpace) {
        try {
            URLClassLoader classLoader = URLClassLoader.newInstance(new URL[] {new URL("jar:file:" + jarFile.getAbsolutePath() + "!/")}, Thread.currentThread().getContextClassLoader());
            //URLClassLoader classLoader = new URLClassLoader(new URL[] {jarFile.toURI().toURL()}, Resolver.class.getClassLoader());
            Reflections reflections = ResolverUtil.newReflections(rootPackageSpace, classLoader);
            Set<Class<?>> applications = reflections.getTypesAnnotatedWith(ResolverApplication.class);
            Map<Class<?>, me.zort.commons.resolverframework_dev.ResolverService> result = Maps.newHashMap();
            applications.forEach(applicationClass -> {
                me.zort.commons.resolverframework_dev.ResolverService resolverService = application(classLoader, applicationClass);
                if(resolverService != null) {
                    result.put(applicationClass, resolverService);
                }
            });
            return result;
        } catch (MalformedURLException e) {
            resolverLogger.error(e.getMessage());
            return null;
        }
    }

    @Nullable
    @Beta
    public static me.zort.commons.resolverframework_dev.ResolverService application(ClassLoader classLoader, Class<?> applicationClass) {
        try {
            Object application = applicationClass.getDeclaredConstructor().newInstance();
            ResolverBuilder builder = builder(applicationClass);
            if(applicationClass.isAnnotationPresent(EnableDebug.class)) {
                builder.withDebug();
            }
            //TODO: MySQL
            ResolverService resolverService = builder.link();
            if(applicationClass.isAnnotationPresent(AppendPackageSpaces.class)) {
                AppendPackageSpaces appendPackageSpaces = applicationClass.getDeclaredAnnotation(AppendPackageSpaces.class);
                PackageSpaceAppender[] value = appendPackageSpaces.value();
                List<PackageSpaceAppender> valueList = Arrays.stream(value).collect(Collectors.toList());
                valueList.forEach(appender -> {
                    String conditionMethod = appender.conditionMethod();
                    ResolvingPriority priority = appender.priority();
                    String packageSpace = appender.packageSpace();
                    if(!packageSpace.equalsIgnoreCase(resolverService.getPackageSpace())) {
                        boolean b = true;
                        if(!conditionMethod.equalsIgnoreCase(Strings.EMPTY)) {
                            try {
                                Method method = applicationClass.getDeclaredMethod(conditionMethod);
                                Object result = method.invoke(application);
                                if(Primitives.isWrapperType(Boolean.class)) {
                                    b = (boolean) result;
                                } else {
                                    resolverLogger.error("Application " + applicationClass.getName() + " condition method " + conditionMethod + " does not return boolean.");
                                }
                            } catch (NoSuchMethodException e) {
                                resolverLogger.error("Application " + applicationClass.getName() + " does not have condition method " + conditionMethod + " or is invalid.");
                            } catch (InvocationTargetException | IllegalAccessException e) {
                                resolverLogger.error("Application " + applicationClass.getName() + " condition method " + conditionMethod + " has invalid access modifier or there was unexpected error.");
                            }
                        }
                        if(b) resolverService.appendPackageSpace(packageSpace, classLoader, priority);
                    }
                });
            }
            return resolverService;
        } catch (NoSuchMethodException | InvocationTargetException | IllegalAccessException | InstantiationException e) {
            resolverLogger.error(e.getMessage());
            return null;
        }
    }

}
