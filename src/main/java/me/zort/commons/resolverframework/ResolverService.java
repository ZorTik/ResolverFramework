package me.zort.commons.resolverframework;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.gson.Gson;
import ez.DB;
import lombok.Getter;
import lombok.Setter;
import me.zort.commons.common.Pair;
import me.zort.commons.common.Strings;
import me.zort.commons.resolverframework.exceptions.ConfigurerException;
import me.zort.commons.resolverframework.exceptions.ServiceException;
import me.zort.commons.resolverframework.lifecycle.ResolverClass;
import me.zort.commons.resolverframework.lifecycle.ResolverClassImpl;
import me.zort.commons.resolverframework.lifecycle.ResolverConfigurer;
import me.zort.commons.resolverframework.lifecycle.bukkit.ResolverClassBukkit;
import me.zort.commons.resolverframework.logging.ResolverLogger;
import me.zort.commons.resolverframework.logging.ResolverMessage;
import me.zort.commons.resolverframework.type.*;
import me.zort.commons.resolverframework.type.configuration.Configurer;
import me.zort.commons.resolverframework.type.configuration.Data;
import me.zort.commons.resolverframework.type.database.SqlDownload;
import me.zort.commons.resolverframework.type.database.SqlLookupKeys;
import me.zort.commons.resolverframework.type.database.SqlUpload;
import me.zort.commons.resolverframework.util.ResolverUtil;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.reflections.Reflections;

import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Collectors;

@SuppressWarnings("unused")
public class ResolverService implements me.zort.commons.resolverframework_dev.ResolverService {

    protected static ResolverService link(String packageSpace) {
        return link(packageSpace, null);
    }

    protected static ResolverService link(String packageSpace, ClassLoader classLoader) {
        ResolverService service = new ResolverService(packageSpace, classLoader);
        ResolverRepository.add(service);
        return service;
    }

    public static final String DEBUG_MAIN_CODE = "main";

    @Getter
    private final ResolverLogger logger;

    @Getter
    private final String packageSpace;
    @Getter
    private final Map<Reflections, Pair<String, ResolvingPriority>> reflections;
    @Nullable
    @Setter
    @Getter
    private ClassLoader mainClassLoader;

    private final Map<Class<?>, List<ResolverClassImpl<? extends ResolverClassImpl<?>>>> cachedBasic;
    @Getter
    private final Map<Class<?>, ResolverConfigurer> configurers;
    @Getter
    private final Map<Class<?>, Object> specialObjects;
    @Getter
    private final Map<String, Supplier<Boolean>> conditions;
    private final List<Consumer<ResolverClassImpl<? extends ResolverClassImpl<?>>>> paramActions;
    @Getter
    private final List<Class<? extends Annotation>> checked;
    private final List<Class<?>> externalConfigurers;
    private final List<Class<?>> externalServices;

    private final List<String> debugExclusions;
    @Setter
    @Getter
    private boolean debug;

    @Getter
    private static final List<Class<?>> customBukkitEvents = Lists.newArrayList();
    @Nullable
    @Getter
    private String bukkitPluginLinked;

    @Getter
    private final ExecutorService executorService;
    @Getter
    private final Gson gson;
    @Getter
    private DB mySQL;

    private ResolverService(String packageSpace) {
        this(packageSpace, null);
    }

    private ResolverService(String packageSpace, @Nullable ClassLoader classLoader) {
        this(packageSpace, classLoader, Lists.newArrayList(
                        Construct.class,
                        RepeatingTaskShutdown.class,
                        RepeatingTask.class,
                        Task.class,
                        Value.class,
                        SqlLookupKeys.class,
                        SqlDownload.class,
                        SqlUpload.class,
                        Data.class,
                        Unresolve.class,
                        ResolvingCompleted.class,
                        Condition.class
                )
        , resolverClass -> {
            resolverClass.invokeFields(Value.class);
            resolverClass.invokeFields(Data.class);
            resolverClass.invokeMethods(Construct.class);
        });
    }

    @SafeVarargs
    private ResolverService(String packageSpace,
                            @Nullable ClassLoader classLoader,
                            List<Class<? extends Annotation>> checked,
                            Consumer<ResolverClassImpl<? extends ResolverClassImpl<?>>>... paramActions) {
        this.logger = ResolverLogger.createLogger(ResolverService.class, this);
        this.packageSpace = packageSpace;
        this.reflections = Maps.newHashMap();
        this.mainClassLoader = classLoader;
        this.cachedBasic = Maps.newConcurrentMap();
        this.configurers = Maps.newHashMap();
        this.specialObjects = Maps.newHashMap();
        this.conditions = Maps.newConcurrentMap();
        this.checked = checked;
        this.paramActions = Arrays.stream(paramActions).collect(Collectors.toList());
        this.externalConfigurers = Lists.newArrayList();
        this.externalServices = Lists.newArrayList();
        this.debugExclusions = Lists.newArrayList();
        this.debug = false;
        this.bukkitPluginLinked = null;
        this.executorService = Executors.newCachedThreadPool();
        this.gson = new Gson();
        this.mySQL = null;
    }

    @SuppressWarnings("UnusedReturnValue")
    public ResolverService appendPackageSpace(String packageSpace, ResolvingPriority priority) {
        return appendPackageSpace(packageSpace, null, priority);
    }

    public ResolverService appendPackageSpace(String packageSpace, @Nullable ClassLoader classLoader, ResolvingPriority priority) {
        Reflections reflections = classLoader != null ? ResolverUtil.newReflections(packageSpace, classLoader) : ResolverUtil.newReflections(packageSpace);
        this.reflections.put(reflections, new Pair<>(packageSpace, priority));
        return this;
    }

    public void registerExternalConfigurers(Class<?>... externalConfigurerClasses) {
        for(Class<?> clazz : externalConfigurerClasses) {
            registerExternalConfigurer(clazz);
        }
    }

    public void registerExternalConfigurer(Class<?> externalConfigurerClass) {
        externalConfigurers.add(externalConfigurerClass);
    }

    public void registerExternalServices(Class<?>... externalServiceClasses) {
        for(Class<?> clazz : externalServiceClasses) {
            registerExternalService(clazz);
        }
    }

    public void registerExternalService(Class<?> externalServiceClass) {
        externalServices.add(externalServiceClass);
    }

    public void registerCondition(String key, Supplier<Boolean> condition) {
        conditions.put(key, condition);
    }

    public void enableMysql(DB mySQL) {
        this.mySQL = mySQL;
    }

    public void enableMysql(Class<?> clazz) throws ServiceException {
        enableMysql(clazz, null);
    }

    /**
     *  Třída musí obsahovat prázdný konstruktor.
     */
    public <T> void enableMysql(Class<T> clazz, @Nullable T o) throws ServiceException {
        try {
            logger.info("Researching SQL Configuration " + clazz.getName());
            Object classObject = o == null
                    ? clazz.getDeclaredConstructor().newInstance()
                    : o;
            boolean b = false;
            for(Method method : clazz.getDeclaredMethods()) {
                if(method.getReturnType().equals(DB.class) && method.getParameterCount() == 0) {
                    this.mySQL = (DB) method.invoke(classObject);
                    b = true;
                }
            }
            if(b) {
                logger.info("Enabled MySQL for package space " + packageSpace);
            } else {
                logger.err("Cannot load MySQL configuration class " + clazz.getName() + ". Contains MySQL method?");
            }
        } catch(Exception ex) {
            throw new ServiceException("Cannot load SQL configuration class: " + ex.getMessage());
        }
    }

    public void registerCustomBukkitEvents(Class<?>... eventClasses) {
        Arrays.stream(eventClasses).forEach(this::registerCustomBukkitEvent);
    }

    public void registerCustomBukkitEvents(Collection<Class<?>> eventClasses) {
        eventClasses.forEach(this::registerCustomBukkitEvent);
    }

    public void registerCustomBukkitEvent(Class<?> eventClass) {
        customBukkitEvents.add(eventClass);
    }

    public void linkBukkitPlugin(JavaPlugin plugin) {
        if(plugin == null) return;
        this.bukkitPluginLinked = plugin.getName();
    }

    public void insertDebugExclusions(Collection<String> codes) {
        codes.forEach(this::insertDebugExclusion);
    }

    public void insertDebugExclusion(String code) {
        if(this.debugExclusions.contains(code)) return;
        this.debugExclusions.add(code);
    }

    public <T> void setSpecialObject(T o, Class<T> type) {
        specialObjects.put(type, o);
    }

    public void callEvent(Object event) {
        cachedBasic.forEach((clazz, resolverClasses) -> resolverClasses.forEach(resolverClass -> resolverClass.invokeMethodsCustom(Event.class, resolverMethod -> {
            Event eventAnnot = resolverMethod.getMethod().getDeclaredAnnotation(Event.class);
            return eventAnnot != null && eventAnnot.value().equals(event.getClass());
        }, event)));
    }

    @SuppressWarnings("unchecked")
    @NotNull
    public <T> Optional<T> getObjectByClass(Class<T> clazz) {
        T res = null;
        if(cachedBasic.containsKey(clazz) && !cachedBasic.get(clazz).isEmpty()) {
            ResolverClassImpl<? extends ResolverClassImpl<?>> resolverClass = cachedBasic.get(clazz).get(0);
            if(resolverClass.isInstantinated()) {
                res = (T) resolverClass.getInstance();
            }
        } else if(configurers.containsKey(clazz)) {
            ResolverConfigurer resolverConfigurer = configurers.get(clazz);
            ResolverClass resolverClass = resolverConfigurer.getResolverClass();
            if(resolverClass.isInstantinated()) {
                res = (T) resolverClass.getInstance();
            }
        }
        return Optional.ofNullable(res);
    }

    public List<ResolverClassImpl<? extends ResolverClassImpl<?>>> getResolverClassesByClass(Class<?> clazz) {
        return cachedBasic.getOrDefault(clazz, Lists.newArrayList());
    }

    @NotNull
    public List<ResolverClassImpl<? extends ResolverClassImpl<?>>> getAllCachedbyType(Class<?> type) {
        cachedBasic.putIfAbsent(type, new ArrayList<>());
        return cachedBasic.get(type);
    }

    public boolean resolveSingle(Class<?> serviceClass, boolean ignorePaused) {
        if(!serviceClass.isAnnotationPresent(Service.class)) {
            logger.err("Class " + serviceClass.getName() + " is not service.");
            return false;
        }
        Service service = serviceClass.getDeclaredAnnotation(Service.class);
        if(service.paused() && !ignorePaused) {
            logger.err("Class " + serviceClass.getName() + " is paused.");
            return false;
        }
        @SuppressWarnings("all")
        List<ResolverClassImpl<? extends ResolverClassImpl<?>>> temp = Lists.newArrayList();
        if(service.constructionCount() >= 1) {
            String condition = service.condition();
            try {
                if(condition.equals(Strings.EMPTY) || (conditions.containsKey(condition) && conditions.get(condition).get())) {
                    if(service.bukkit() && bukkitPluginLinked == null) {
                        logger.err("Cannot load bukkit resolver class " + serviceClass.getName() + ". We don't have bukkit plugin linked.");
                    } else {
                        for(int i = 0; i < service.constructionCount(); i++) {
                            try {
                                ResolverClassImpl<? extends ResolverClassImpl<?>> resolverClass = service.bukkit()
                                        ? new ResolverClassBukkit(bukkitPluginLinked, this, serviceClass)
                                        .instantinate()
                                        .prepare()
                                        : new ResolverClass(this, serviceClass)
                                        .instantinate()
                                        .prepare();
                                getAllCachedbyType(serviceClass).add(resolverClass);
                                if(getAllCachedbyType(serviceClass).isEmpty()) {
                                    logger.err("Storage of type " + serviceClass.getSimpleName() + " was empty.");
                                }
                            } catch (Exception ex) {
                                logger.err(ex.getMessage());
                            }
                        }
                    }
                }
            } catch(Exception ex) {
                logger.err("Cannot resolve class " + serviceClass.getName() + ":");
                ex.printStackTrace();
            }
        }
        temp.forEach(resolverClass -> {
            if(resolverClass.isInstantinated()) {
                debug("Invoking constructs " + resolverClass.getClazz().getName());
                paramActions.forEach(c -> c.accept(resolverClass));
                debug("Invoker constructs completed " + resolverClass.getClazz().getName());
            }
            debug("Service annotation " + service);
            if(service.scheduled()) {
                debug("Scheduling " + resolverClass.getClazz().getName());
                debug("Delay: " + service.delay());
                resolverClass.start(service.delay() + 1L, 1L);
                debug("Scheduler completed " + resolverClass.getClazz().getName());
            }
            resolverClass.initialize();
        });
        getAllCachedbyType(serviceClass).addAll(temp);
        return true;
    }

    public boolean resolve() {
        logger.bc(ResolverMessage.STARTING, packageSpace);
        Optional<Reflections> reflectionsOptional = ResolverRepository.getReflectionsByPackageSpace(packageSpace);
        if(!reflectionsOptional.isPresent()) {
            logger.err(ResolverMessage.ERR_REFLECTIONS_NOT_PRESENT_STARTUP);
            return false;
        }
        Reflections reflectionsObj = reflectionsOptional.get();
        if(!this.reflections.containsKey(reflectionsObj)) this.reflections.put(reflectionsObj, new Pair<>(packageSpace, ResolvingPriority.ROOT_WORK));
        AtomicBoolean atomicBoolean = new AtomicBoolean(true);
        List<Reflections> reflectionsList = this.reflections.keySet().stream()
                .sorted(Collections.reverseOrder(Comparator.comparingInt(o -> reflections.get(o).getValue().getCode())))
                .collect(Collectors.toList());
        reflectionsList.forEach(reflections1 -> {
            if(!resolve(reflections1)) atomicBoolean.set(false);
        });
        ResolverUtil.cloneList(cachedBasic.values()).forEach(resolverClasses -> resolverClasses.forEach(resolverClass -> {
            resolverClass.invokeMethods(ResolvingCompleted.class);
        }));
        return atomicBoolean.get();
    }

    public boolean resolve(Reflections reflections) {
        if(!this.reflections.containsKey(reflections)) {
            logger.err("Cannot resolve reflections " + reflections + ". Package space is not appended.");
            return false;
        }
        String packageSpaceTemp = this.reflections.get(reflections).getKey();
        Set<Class<?>> configs = reflections.getTypesAnnotatedWith(Configurer.class);
        configs.addAll(externalConfigurers/*.stream().filter(clazz -> clazz.isAnnotationPresent(Configurer.class)).collect(Collectors.toList())*/);
        configs.forEach(configClass -> {
            if(ResolverUtil.isInPackage(configClass, packageSpaceTemp)) {
                Configurer configurer = configClass.getDeclaredAnnotation(Configurer.class);
                Class<? extends ResolverConfigurer> configurerCandidate = configurer.value();
                try {
                    Constructor<? extends ResolverConfigurer> constructor = configurerCandidate.getDeclaredConstructor(ResolverService.class, Class.class);
                    ResolverConfigurer object = constructor.newInstance(this, configClass);
                    if(object.configure()) {
                        debug("Successfully configured class " + configClass.getName() + " with candidate " + configurerCandidate.getName());
                        configurers.put(configClass, object);
                    } else {
                        logger.err("Cannot configure candidate " + configurerCandidate.getName());
                    }
                } catch (NoSuchMethodException ex) {
                    logger.err("Selected configurer candidate " + configurerCandidate.getName() + " does not have required constructor.");
                } catch(InvocationTargetException ex) {
                    if(ex.getCause() instanceof ConfigurerException) {
                        ConfigurerException configurerException = (ConfigurerException) ex.getCause();
                        logger.err("Configurer " + configClass.getName() + " threw an exception: " + configurerException.getMessage());
                    }
                } catch (InstantiationException | IllegalAccessException ex) {
                    logger.err("Unexpected error occured while instantinating configurer candidate " + configurerCandidate.getName());
                }
            }
        });
        Set<Class<?>> services = reflections.getTypesAnnotatedWith(Service.class);
        services.addAll(externalServices/*.stream().filter(clazz -> clazz.isAnnotationPresent(Service.class)).collect(Collectors.toList())*/);
        services.forEach(serviceClass -> {
            if(ResolverUtil.isInPackage(serviceClass, packageSpaceTemp)) {
                Service service = serviceClass.getDeclaredAnnotation(Service.class);
                if(service != null) {
                    if(!service.paused() && service.constructionCount() >= 1) {
                        String condition = service.condition();
                        try {
                            if(condition.equals(Strings.EMPTY) || (conditions.containsKey(condition) && conditions.get(condition).get())) {
                                if(service.bukkit() && bukkitPluginLinked == null) {
                                    logger.err("Cannot load bukkit resolver class " + serviceClass.getName() + ". We don't have bukkit plugin linked.");
                                } else {
                                    for(int i = 0; i < service.constructionCount(); i++) {
                                        try {
                                            ResolverClassImpl<? extends ResolverClassImpl<?>> resolverClass = service.bukkit()
                                                    ? new ResolverClassBukkit(bukkitPluginLinked, this, serviceClass)
                                                    .instantinate()
                                                    .prepare()
                                                    : new ResolverClass(this, serviceClass)
                                                    .instantinate()
                                                    .prepare();
                                            getAllCachedbyType(serviceClass).add(resolverClass);
                                            if(getAllCachedbyType(serviceClass).isEmpty()) {
                                                logger.err("Storage of type " + serviceClass.getSimpleName() + " was empty.");
                                            }
                                        } catch (Exception ex) {
                                            logger.err(ex.getMessage());
                                        }
                                    }
                                }
                            }
                        } catch(Exception ex) {
                            logger.err("Cannot resolve class " + serviceClass.getName() + ":");
                            ex.printStackTrace();
                        }
                    }
                } else {
                    logger.err("Class " + serviceClass.getName() + " was registered, but is not service!");
                }
            }
        });
        configurers.forEach((clazz, configurer) -> configurer.postConfigure());
        Collection<List<ResolverClassImpl<? extends ResolverClassImpl<?>>>> cachedBasicValues = ResolverUtil.cloneList(cachedBasic.values());
        cachedBasicValues.forEach(resolverClasses -> resolverClasses.forEach(resolverClass -> {
            if(ResolverUtil.isInPackage(resolverClass.getClazz(), packageSpaceTemp)) {
                debug("Service " + resolverClass.getClazz().getName());
                Service service = resolverClass.getClazz().getDeclaredAnnotation(Service.class);
                if(resolverClass.isInstantinated()) {
                    debug("Invoking constructs " + resolverClass.getClazz().getName());
                    paramActions.forEach(c -> c.accept(resolverClass));
                    debug("Invoker constructs completed " + resolverClass.getClazz().getName());
                }
                debug("Service annot " + service.toString());
                if(service.scheduled()) {
                    debug("Scheduling " + resolverClass.getClazz().getName());
                    debug("Delay: " + service.delay());
                    resolverClass.start(service.delay() + 1L, 1L);
                    debug("Scheduler completed " + resolverClass.getClazz().getName());
                }
                resolverClass.initialize();
            }
        }));
        List<Class<?>> scheduledTypes = cachedBasic.keySet().stream()
                .filter(key -> !getScheduledResolverClasses(key).isEmpty())
                .collect(Collectors.toList());
        if(scheduledTypes.isEmpty()) {
            cachedBasic.forEach((type, resolverClasses) -> resolverClasses.forEach(resolverClass -> {
                debug(ResolverMessage.DEBUG_NO_SCHEDULED_SERVICE_LOADED,
                        resolverClass.getClazz().toString());
                resolverClass.stop();
            }));
        }
        return true;
    }

    public void unresolve() {
        cachedBasic.forEach((typeClass, resolverClasses) -> resolverClasses.forEach(resolverClass -> resolverClass.invokeMethods(Unresolve.class)));
        configurers.values().forEach(resolverConfigurer -> resolverConfigurer.getResolverClass().invokeMethods(Unresolve.class));
        cachedBasic.forEach((typeClass, resolverClasses) -> resolverClasses.forEach(resolverClass -> {
            resolverClass.stop();
            resolverClass.getCachedMethodResults().clear();
            resolverClass.getResolverMethods().clear();
            try {
                resolverClass.unresolve();
            } catch(Exception ex) {
                ex.printStackTrace();
            }
        }));
        executorService.shutdownNow();
        configurers.values().forEach(ResolverConfigurer::deconfigure);
        cachedBasic.clear();
        configurers.clear();
    }

    private List<ResolverClassImpl<? extends ResolverClassImpl<?>>> getScheduledResolverClasses(Class<?> key) {
        return getAllCachedbyType(key).stream()
                .filter(resolverClass -> {
                    if(!resolverClass.isInstantinated()) return false;
                    Service service = resolverClass.getInstance().getClass()
                            .getDeclaredAnnotation(Service.class);
                    return service.scheduled();
                })
                .collect(Collectors.toList());
    }

    public void debug(String message) {
        debug(DEBUG_MAIN_CODE, message);
    }

    public void debug(ResolverMessage message, String... replacements) {
        debug(DEBUG_MAIN_CODE, message, replacements);
    }

    public void debug(String code, ResolverMessage message, String... replacements) {
        if(debugExclusions.contains(code)) return;
        logger.debug(message, replacements);
    }

    public void debug(String code, String message) {
        if(debugExclusions.contains(code)) return;
        logger.debug(message);
    }

}
