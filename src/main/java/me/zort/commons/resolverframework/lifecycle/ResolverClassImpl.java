package me.zort.commons.resolverframework.lifecycle;

import com.google.common.base.Defaults;
import com.google.common.collect.ImmutableMap;
import com.google.common.primitives.Primitives;
import me.zort.commons.resolverframework.ResolverService;
import me.zort.commons.resolverframework.exceptions.ServiceException;
import me.zort.commons.resolverframework.logging.ResolverLogger;
import me.zort.commons.resolverframework.type.Condition;
import me.zort.commons.resolverframework.type.Service;
import me.zort.commons.resolverframework.type.Value;
import me.zort.commons.resolverframework.type.configuration.Configurer;
import me.zort.commons.resolverframework.type.configuration.Data;
import me.zort.commons.resolverframework.type.configuration.DataAll;
import me.zort.commons.resolverframework.type.configuration.MapColumn;
import org.jetbrains.annotations.Nullable;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.*;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public interface ResolverClassImpl<C extends ResolverClassImpl<?>> extends ResolverCandidate {

    default <T> T initAutoValue(AnnotatedElement annotatedElement, Class<T> type, T... customValues) {
        if (annotatedElement.getDeclaredAnnotation(Value.class) != null) {
            Value value = annotatedElement.getDeclaredAnnotation(Value.class);
            return initAutoValueCustomized(value, type, customValues);
        } else if (annotatedElement.getDeclaredAnnotation(Data.class) != null) {
            Data data = annotatedElement.getDeclaredAnnotation(Data.class);
            return initDataValue(data, type).orElse(Defaults.defaultValue(type));
        } else if (annotatedElement.getDeclaredAnnotation(DataAll.class) != null) {
            DataAll dataAll = annotatedElement.getDeclaredAnnotation(DataAll.class);
            Optional<T> dataAllOptional = initDataValue(dataAll, type);
            T dataAllObject = dataAllOptional.orElse(null);
            return dataAllObject;
        } else if(annotatedElement.getDeclaredAnnotation(Condition.class) != null && type.getSimpleName().equalsIgnoreCase("boolean")) {
            Condition condition = annotatedElement.getDeclaredAnnotation(Condition.class);
            return initConditionValue(condition, type);
        } else if(customValues.length > 0) {
            T o = initAutoValueCustomized(null, type, customValues);
            return o;
        }
        return Defaults.defaultValue(type);
    }

    <T> T initAutoValue(Value value, Class<T> type);
    default <T> T initAutoValueCustomized(Value value, Class<T> type, T... customValues) {
        List<ResolverClassImpl<? extends ResolverClassImpl<?>>> allByType = getService().getAllCachedbyType(type);
        allByType.forEach(clazz -> {
            if(!clazz.isInstantinated()) {
                clazz.instantinate();
            }
        });
        List<Object> objs = allByType.stream()
                .filter(ResolverClassImpl::isInstantinated)
                .map(ResolverClassImpl::getInstance)
                .collect(Collectors.toList());
        if(customValues.length > 0) {
            objs.addAll(Arrays.stream(customValues)
                    .filter(customValue -> {
                        if(customValue.getClass().equals(type) || type.isAssignableFrom(customValue.getClass())) {
                            return true;
                        } else {
                            try {
                                type.cast(customValue);
                                return true;
                            } catch(ClassCastException ex) {
                                return false;
                            }
                        }
                    })
                    .collect(Collectors.toList()));
        }
        if(!getService().getSpecialObjects().isEmpty()) {
            Map<Class<?>, Object> specialObjects = getService().getSpecialObjects();
            specialObjects.forEach((typeClass, specialObject) -> {
                if(typeClass.equals(type) || (specialObject != null && specialObject.getClass().equals(type))) {
                    objs.add(specialObject);
                }
            });
            /*objs.addAll(specialObjects.values().stream()
                    .filter(specialValue -> specialValue.getClass().equals(type))
                    .collect(Collectors.toList()));*/
        }
        Map<Class<?>, ResolverConfigurer> configurers = getService().getConfigurers();
        if(!configurers.isEmpty() && configurers.containsKey(type)) {
            ResolverConfigurer configurer = configurers.get(type);
            ResolverClass resolverClass = configurer.getResolverClass();
            objs.add(resolverClass.getInstance());
        }
        Optional<Object> objByName = objs.stream()
                .filter(obj -> {
                    Service service = obj.getClass().getDeclaredAnnotation(Service.class);
                    if(service == null) return true;
                    String name = service.serviceName();
                    return name.equals(value.name());
                })
                .findFirst();
        getLogger().debug("Auto value init Value: " + value);
        getLogger().debug("Auto value init type: " + type);
        if(value != null && value.generate()) {
            try {
                ResolverClass resolverClass = new ResolverClass(getService(), type)
                        .prepare()
                        .instantinate();
                return (T) resolverClass.getInstance();
            } catch (ServiceException e) {
                getLogger().err("Cannot resolve generated value " + type + ": " + e.getMessage());
            }
        }
        if(objs.isEmpty() || (value != null && (!value.name().equalsIgnoreCase("none")) && !objByName.isPresent())) {
            if(type.equals(Short.class) || type.equals(Long.class) || type.equals(Float.class) || type.equals(short.class) || type.equals(long.class) || type.equals(float.class)) {
                return (T) new Double(0.0);
            } else if(type.equals(Integer.class) || type.equals(int.class)) {
                return (T) new Integer(0);
            } else if(type.equals(Boolean.class) || type.equals(boolean.class)) {
                return (T) Boolean.FALSE;
            } else {
                return null;
            }
        } else {
            if(value == null) {
                return (T) objs.get(0);
            }
            return value.name().equalsIgnoreCase("none")
                    ? (T) objs.get(value.position().get(objs.size()))
                    : (T) objByName.orElse(null);
        }
    }

    /**
     *  Potlačuji varování unchecked, jelikož se ověřuje shoda typu.
     */
    @SuppressWarnings("unchecked")
    default <T, D extends Annotation> Optional<T> initDataValue(D value, Class<T> type) {
        ResolverLogger logger = getService().getLogger();
        T result = null;
        if(value instanceof DataAll) {
            DataAll val = (DataAll) value;
            Class<?> dataSource = val.source();
            ResolverService service = getService();
            if(dataSource.isAnnotationPresent(Configurer.class)) {
                Configurer configurer = dataSource.getDeclaredAnnotation(Configurer.class);
                Class<? extends ResolverConfigurer> configurerCandidate = configurer.value();
                Map<Class<?>, ResolverConfigurer> configurers = service.getConfigurers();
                if(configurerCandidate.equals(ResolverConfigurer_Data.class) && configurers.containsKey(dataSource)) {
                    ResolverConfigurer_Data dataConfigurer = (ResolverConfigurer_Data) configurers.get(dataSource);
                    Map<String, Object> data = dataConfigurer.getData();
                    if(type.equals(Map.class)) {
                        Map<String, Object> dataCloned = ImmutableMap.copyOf(data);
                        result = (T) dataCloned;
                    } else if(type.equals(Collection.class)) {
                        MapColumn col = val.col();
                        Collection<?> collection = col.equals(MapColumn.KEYS) ? data.keySet() : data.values();
                        result = (T) collection;
                    }
                }
            }
        } else if(value instanceof Data) {
            Data val = (Data) value;
            Class<?> dataSource = val.source();
            ResolverService service = getService();
            if(dataSource.isAnnotationPresent(Configurer.class)) {
                Configurer configurer = dataSource.getDeclaredAnnotation(Configurer.class);
                Class<? extends ResolverConfigurer> configurerCandidate = configurer.value();
                Map<Class<?>, ResolverConfigurer> configurers = service.getConfigurers();
                if(configurerCandidate.equals(ResolverConfigurer_Data.class) && configurers.containsKey(dataSource)) {
                    ResolverConfigurer_Data dataConfigurer = (ResolverConfigurer_Data) configurers.get(dataSource);
                    Map<String, Object> data = dataConfigurer.getData();
                    if(data.containsKey(val.key())) {
                        Object dataObject;
                        Class<?> dataObjectTypeWrapped = Primitives.wrap((dataObject = data.get(val.key())).getClass());
                        Class<?> typeWrapped = Primitives.wrap(type);
                        if(dataObjectTypeWrapped.equals(typeWrapped)) {
                            result = (T) dataObject;
                        }
                    }
                }
            }
        }
        return Optional.ofNullable(result);
    }

    default <T> T initConditionValue(Condition condition, Class<T> type) {
        ResolverService resolverService = getService();
        Map<String, Supplier<Boolean>> conditions = resolverService.getConditions();
        String val = condition.value();
        if(conditions.containsKey(val)) {
            return (T) conditions.get(val).get();
        }
        return (T) Boolean.FALSE;
    }

    default void unresolve() {}

    @Nullable
    default Service getServiceAnnot() {
        if(!getClazz().isAnnotationPresent(Service.class)) {
            return null;
        }
        return getClazz().getDeclaredAnnotation(Service.class);
    }
    ResolverService getService();
    ResolverLogger getLogger();
    Class<?> getClazz();
    boolean isInstantinated();
    void setPaused(boolean paused);
    boolean isPaused();
    Object getInstance();
    Map<Class<? extends Annotation>, List<ResolverMethod>> getResolverMethods();
    Map<Class<? extends Annotation>, List<ResolverField>> getResolverFields();
    Map<Method, Object> getCachedMethodResults();
    List<ResolverField> getAllFieldsByAnnotation(Class<? extends Annotation> clazz);
    List<ResolverMethod> getAllByAnnotation(Class<? extends Annotation> clazz);
    void invokeFields(Class<? extends Annotation> annotationType);
    void invokeField(ResolverField field);
    boolean existMethodByCondition(Class<? extends Annotation> annotationType, Predicate<ResolverMethod> condition);
    void invokeMethodsCustom(Class<? extends Annotation> annotationType, Predicate<ResolverMethod> condition, Object... customValues);
    void invokeMethods(Class<? extends Annotation> annotationType, Predicate<ResolverMethod> condition);
    void invokeMethods(Class<? extends Annotation> annotationType);
    void invokeMethodCustom(ResolverMethod method, Predicate<ResolverMethod> condition, Object... customValues);
    void invokeMethod(ResolverMethod method, Predicate<ResolverMethod> condition);
    void invokeMethod(ResolverMethod method);
    List<Class<? extends Annotation>> getAddonCheckedAnnotations();

    C getResolverClass();

    default C prepare() {
        List<Method> methods = Arrays.stream(getClazz().getDeclaredMethods())
                .filter(method -> method.getAnnotations().length > 0)
                .collect(Collectors.toList());
        List<Class<? extends Annotation>> checked = new ArrayList<>();
        checked.addAll(getService().getChecked());
        checked.addAll(getAddonCheckedAnnotations());
        methods.forEach(method -> checked.forEach(a -> {
            if(method.isAnnotationPresent(a)) {
                getResolverMethods().putIfAbsent(a, new ArrayList<>());
                getResolverMethods().get(a)
                        .add(new ResolverMethod(getResolverClass(), method));
            }
        }));
        List<Field> fields = Arrays.stream(getClazz().getDeclaredFields())
                .filter(field -> field.getAnnotations().length > 0)
                .collect(Collectors.toList());
        fields.forEach(field -> getService().getChecked().forEach(a -> {
            if(field.isAnnotationPresent(a)) {
                getResolverFields().putIfAbsent(a, new ArrayList<>());
                getResolverFields().get(a)
                        .add(new ResolverField(getResolverClass(), field));
            }
        }));
        return getResolverClass();
    }
    C instantinate();
    C initialize();

    void start(long delay, long period);
    void stop();

}
