package me.zort.commons.resolverframework.lifecycle.bukkit;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.primitives.Primitives;
import com.google.common.reflect.TypeToken;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import ez.Row;
import lombok.Getter;
import lombok.Setter;
import me.zort.commons.common.Nullables;
import me.zort.commons.data.sql.Id;
import me.zort.commons.resolverframework.ResolverService;
import me.zort.commons.resolverframework.exceptions.ServiceException;
import me.zort.commons.resolverframework.lifecycle.ResolverClassImpl;
import me.zort.commons.resolverframework.lifecycle.ResolverConstructor;
import me.zort.commons.resolverframework.lifecycle.ResolverField;
import me.zort.commons.resolverframework.lifecycle.ResolverMethod;
import me.zort.commons.resolverframework.logging.ResolverLogger;
import me.zort.commons.resolverframework.type.*;
import me.zort.commons.resolverframework.type.database.*;
import me.zort.commons.resolverframework.type.minecraft.CommandBukkit;
import me.zort.commons.resolverframework.type.minecraft.EventBukkit;
import me.zort.commons.runtime.data.GlobalOrganizedDataStorage;
import me.zort.commons.spigot.task.AbstractTimer;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandMap;
import org.bukkit.command.CommandSender;
import org.bukkit.event.Event;
import org.bukkit.event.EventPriority;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.Nullable;
import ox.x.XList;

import javax.xml.crypto.Data;
import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collectors;

@Getter
public class ResolverClassBukkit extends AbstractTimer implements ResolverClassImpl<ResolverClassBukkit> {

    public static Class<? extends Annotation>[] BUKKIT_ANNOT = new Class[] {
            CommandBukkit.class,
            EventBukkit.class
    };

    private final ResolverLogger logger;
    private final ResolverClassBukkit resolverClass;

    private final ResolverService service;
    private final Class<?> clazz;
    private boolean instantinated;
    @Getter(onMethod_ = {@Nullable})
    private Object instance;
    private final Service annot;

    private final Map<Class<? extends Annotation>, List<ResolverMethod>> resolverMethods;
    private final Map<Class<? extends Annotation>, List<ResolverField>> resolverFields;
    private final Map<Method, Object> cachedMethodResults;
    private final List<Method> inProgressMethods;
    private final Map<String, Command> registeredCommands;

    @Setter
    private GlobalOrganizedDataStorage dataStorage;
    private final SqlHandler sqlHandlerAnnot;

    @Setter
    private boolean paused;
    private boolean done;

    private final List<Class<? extends Annotation>> addonCheckedAnnotations;

    public ResolverClassBukkit(String bukkitPluginName, ResolverService service, Class<?> clazz) throws ServiceException {
        this((JavaPlugin) Optional.ofNullable(Bukkit.getPluginManager().getPlugin(bukkitPluginName))
                .orElseThrow(() -> new ServiceException("I cannot start class targetting to bukkit plugin which does not exist.")),
                service, clazz);
    }

    public ResolverClassBukkit(JavaPlugin plugin, ResolverService service, Class<?> clazz) throws ServiceException {
        this(plugin, service, clazz, Arrays.asList(BUKKIT_ANNOT));
    }

    public ResolverClassBukkit(JavaPlugin plugin, ResolverService service, Class<?> clazz, List<Class<? extends Annotation>> addonCheckedAnnotations) throws ServiceException {
        this(plugin, service, clazz, addonCheckedAnnotations, false);
    }

    public ResolverClassBukkit(JavaPlugin plugin, ResolverService service, Class<?> clazz, List<Class<? extends Annotation>> addonCheckedAnnotations, boolean ignoreNotService) throws ServiceException {
        super(timer(), plugin);
        if(!clazz.isAnnotationPresent(Service.class) && !ignoreNotService) {
            throw new ServiceException("Class " + clazz.getName() + " is not a service.");
        }
        this.logger = service.getLogger();
        this.resolverClass = this;
        this.service = service;
        this.clazz = clazz;
        this.instantinated = false;
        this.instance = null;
        this.annot = getClazz().getDeclaredAnnotation(Service.class);
        this.cachedMethodResults = Maps.newHashMap();
        this.inProgressMethods = Lists.newArrayList();
        this.registeredCommands = Maps.newConcurrentMap();
        this.resolverMethods = Maps.newHashMap();
        this.resolverFields = Maps.newHashMap();
        this.sqlHandlerAnnot = clazz.isAnnotationPresent(SqlHandler.class)
                ? clazz.getDeclaredAnnotation(SqlHandler.class)
                : null;
        this.addonCheckedAnnotations = addonCheckedAnnotations;
        Optional<Service> serviceOptional = Optional.ofNullable(getServiceAnnot());
        this.paused = serviceOptional.isPresent()
                && serviceOptional.get().paused();
        this.done = false;
    }

    @Override
    public ResolverClassBukkit instantinate() {
        try {
            Constructor<?>[] constructors = clazz.getDeclaredConstructors();
            Optional<Constructor<?>> emptyConstructorOptional = Arrays.stream(constructors)
                    .filter(c -> c.getParameterCount() == 0)
                    .findFirst();
            Object obj;
            if(emptyConstructorOptional.isPresent()) {
                Constructor<?> constructor = emptyConstructorOptional.get();
                obj = constructor.newInstance();
            } else if(constructors.length > 0) {
                Constructor<?> constructor = constructors[0];
                ResolverConstructor resolverConstructor = new ResolverConstructor(this, constructor);
                Optional<Object> optionalObj = resolverConstructor.invoke();
                obj = optionalObj.orElseThrow((Supplier<Exception>) () -> new ServiceException("Constructor result is not present."));
            } else {
                Constructor<?> constructor = clazz.getDeclaredConstructor();
                obj = constructor.newInstance();
            }
            instance = obj;
        } catch (Exception ex) {
            logger.err("Cannot instantinate " + clazz.getName() + " by constructor: " + ex.getMessage());
            ex.printStackTrace();
            return this;
        }
        if(sqlHandlerAnnot != null && service.getMySQL() != null) {
            Map<String, Class<?>> cols = Maps.newHashMap();
            cols.put("id", Id.class);
            cols.put(sqlHandlerAnnot.colKey(), String.class);
            cols.put(sqlHandlerAnnot.colValue(), Data.class);
            if(sqlHandlerAnnot.includeTimestamp()) cols.put("time", Long.class);
            this.dataStorage = GlobalOrganizedDataStorage
                    .generate(
                            service.getMySQL(),
                            sqlHandlerAnnot.table(),
                            cols
                    );
            logger.debug("SQL table creation invoked in " + getClazz() + ".");
        }
        this.instantinated = true;
        return this;
    }

    @Override
    public ResolverClassBukkit initialize() {
        registerCommands();
        registerListener();
        return this;
    }

    @Override
    public void unresolve() {
        unregisterCommands();
    }

    @Override
    public void invokeFields(Class<? extends Annotation> annotationType) {
        List<ResolverField> allAnnotated = getAllFieldsByAnnotation(annotationType);
        logger.debug("All annotated fields " + annotationType.getName() + ": " + annotationType);
        allAnnotated.forEach(resolverField -> invokeField(resolverField));
    }

    @Override
    public void invokeField(ResolverField field) {
        field.set();
    }

    @Override
    public boolean existMethodByCondition(Class<? extends Annotation> annotationType, Predicate<ResolverMethod> condition) {
        List<ResolverMethod> allAnnotated = getAllByAnnotation(annotationType).stream()
                .filter(condition)
                .collect(Collectors.toList());
        return !allAnnotated.isEmpty();
    }

    @Override
    public void invokeMethodsCustom(Class<? extends Annotation> annotationType, Predicate<ResolverMethod> condition, Object... customValues) {
        List<ResolverMethod> allAnnotated = getAllByAnnotation(annotationType);
        logger.debug("All annotated " + annotationType.getName() + ": " + allAnnotated);
        allAnnotated.forEach(resolverMethod -> {
            if(condition.test(resolverMethod)) {
                invokeMethodCustom(resolverMethod, condition, customValues);
            }
        });
    }

    @Override
    public void invokeMethods(Class<? extends Annotation> annotationType, Predicate<ResolverMethod> condition) {
        List<ResolverMethod> allAnnotated = getAllByAnnotation(annotationType);
        logger.debug("All annotated " + annotationType.getName() + ": " + allAnnotated);
        allAnnotated.forEach(resolverMethod -> {
            if(condition.test(resolverMethod)) {
                invokeMethod(resolverMethod);
            }
        });
    }

    @Override
    public void invokeMethods(Class<? extends Annotation> annotationType) {
        getAllByAnnotation(annotationType).forEach(this::invokeMethod);
    }

    @Override
    public void invokeMethodCustom(ResolverMethod method, Predicate<ResolverMethod> condition, Object... customValues) {
        Optional<Object> resultOptional = method.invokeCustom(customValues);
        if(!method.getType().equals(Void.TYPE)) {
            Object result = resultOptional.orElse(null);
            getCachedMethodResults()
                    .put(method.getMethod(), result);
        }
    }

    public void invokeMethodCustom(ResolverMethod method, Object... addonObjects) {
        Optional<Object> resultOptional = method.invokeCustom(addonObjects);
        if(!method.getType().equals(Void.TYPE)) {
            Object result = resultOptional.orElse(null);
            getCachedMethodResults()
                    .put(method.getMethod(), result);
        }
    }

    @Override
    public void invokeMethod(ResolverMethod method, Predicate<ResolverMethod> condition) {
        if(condition.test(method)) invokeMethod(method);
    }

    @Override
    public void invokeMethod(ResolverMethod method) {
        Optional<Object> resultOptional = method.invoke();
        if(!method.getType().equals(Void.TYPE)) {
            Object result = resultOptional.orElse(null);
            getCachedMethodResults()
                    .put(method.getMethod(), result);
        }
    }

    @Override
    public <T> T initAutoValue(Value value, Class<T> type) {
        return initAutoValueCustomized(value, type);
    }

    @Override
    public List<ResolverField> getAllFieldsByAnnotation(Class<? extends Annotation> clazz) {
        return resolverFields.getOrDefault(clazz, new ArrayList<>());
    }

    @Override
    public List<ResolverMethod> getAllByAnnotation(Class<? extends Annotation> clazz) {
        return resolverMethods.getOrDefault(clazz, new ArrayList<>());
    }

    public void registerCommands() {
        if(!resolverMethods.containsKey(CommandBukkit.class)) return;
        List<ResolverMethod> resolverMethods = this.resolverMethods.get(CommandBukkit.class);
        resolverMethods.forEach(resolverMethod -> registerCommand(resolverMethod));
    }

    public void unregisterCommands() {
        CommandMap commandMap = getCommandMap();
        if(commandMap != null) {
            registeredCommands.values().forEach(cmd -> {
                if(!cmd.unregister(commandMap)) {
                    logger.warn("Failed to unregister command " + cmd.getName() + "!");
                }
            });
        }
    }

    public void registerCommand(ResolverMethod method) {
        Method m = method.getMethod();
        if(m.isAnnotationPresent(CommandBukkit.class)) {
            CommandBukkit commandBukkit = m.getDeclaredAnnotation(CommandBukkit.class);
            String name = commandBukkit.value();
            String description = commandBukkit.description();
            String usageMessage = commandBukkit.usageMessage();
            String[] aliases = commandBukkit.aliases();
            registerCommand(method, name, description, usageMessage, aliases);
        }
    }

    public void registerListener() {
        if(annot.bukkit() && annot.bukkitListener()) {
            ResolverBukkitListener_v18 listener = new ResolverBukkitListener_v18(this);
            PluginManager pluginManager = getPlugin().getServer().getPluginManager();
            List<Class<? extends Event>> eventsToRegister = Lists.newArrayList(ResolverBukkitListener_v18.BASE_EVENTS);
            ResolverService.getCustomBukkitEvents().forEach(customEvent -> {
                try {
                    eventsToRegister.add((Class<? extends Event>) customEvent);
                } catch(Exception ex) {
                    logger.err("Cannot load custom event "
                            + customEvent.getName()
                            + " to "
                            + clazz.getName()
                            + ": "
                            + ex.getMessage());
                }
            });
            EventPriority[] eventPriorities = EventPriority.values();
            int eventsPriorityMaxIndex = eventPriorities.length - 1;
            eventsToRegister.forEach(eventClass -> pluginManager.registerEvent(eventClass, listener, getServiceAnnot() != null
                    && getServiceAnnot().eventsPriorityIndex() <= eventsPriorityMaxIndex
                    ? eventPriorities[getServiceAnnot().eventsPriorityIndex()]
                    : EventPriority.NORMAL, ((listener1, event) -> {
                listener.invokeEvent(event);
            }), getPlugin(), false));
        }
    }

    public void invokeListener(Event event) {
        if(!annot.bukkit() || !annot.bukkitListener()) return;
        List<ResolverMethod> methods = getAllByAnnotation(EventBukkit.class)/*.stream()
                .filter(resolverMethod -> resolverMethod.getMethod().getParameters().length > 0)
                .collect(Collectors.toList())*/;
        methods.forEach(resolverMethod -> {
            EventBukkit eventBukkit = resolverMethod.getMethod().getDeclaredAnnotation(EventBukkit.class);
            if(eventBukkit.value().equals(event.getClass()) || eventBukkit.value().equals(Event.class)) {
                Optional<Object> playerOptional = ResolverBukkitValueInitializer.EVENT_PLAYER
                        .initialize(service, event);
                if(playerOptional.isPresent()) {
                    invokeMethodCustom(resolverMethod, event, playerOptional.get());
                } else {
                    invokeMethodCustom(resolverMethod, event);
                }
            }
        });
    }

    private void registerCommand(ResolverMethod method, String name, String description, String usageMessage, String[] aliases) {
        String errMessage = "Cannot implement bukkit command " + name + " with " + method.getMethod() + "[class " + getInstance() + "]: ";
        ResolverService service = getService();
        try {
            final Field commandMapField = Bukkit.getServer().getClass().getDeclaredField("commandMap");
            commandMapField.setAccessible(true);
            CommandMap commandMap = (CommandMap) commandMapField.get(Bukkit.getServer());
            Command command = new Command(name, description, usageMessage, Arrays.stream(aliases).collect(Collectors.toList())) {
                @Override
                public boolean execute(CommandSender commandSender, String label, String[] args) {
                    Optional<Object> objOptional = method.invokeCustom(commandSender, args);
                    Object obj = objOptional.orElse(null);
                    return !Primitives.wrap(method.getMethod().getReturnType()).equals(Boolean.class) || (boolean) obj;
                }
            };
            if(commandMap.register(name, command)) {
                registeredCommands.put(name, command);
            } else service.getLogger().err(errMessage + "An unexpected error occured.");
        } catch (Exception ex) {
            service.getLogger().err(errMessage + ex.getMessage());
        }
    }

    private CommandMap getCommandMap() {
        final Field commandMapField;
        try {
            commandMapField = Bukkit.getServer().getClass().getDeclaredField("commandMap");
            commandMapField.setAccessible(true);
            return (CommandMap) commandMapField.get(Bukkit.getServer());
        } catch (NoSuchFieldException | IllegalAccessException e) {
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    private static Function<AbstractTimer, Boolean> timer() {
        return timer -> {
            ResolverClassBukkit resolverClass = (ResolverClassBukkit) timer;
            if(resolverClass.isPaused()) return true;
            ResolverService service = resolverClass.getService();
            Class<RepeatingTaskShutdown> repeatingTaskShutdownClass = RepeatingTaskShutdown.class;
            Class<RepeatingTask> repeatingTaskClass = RepeatingTask.class;
            Class<Task> taskClass = Task.class;
            Class<Service> serviceClass = Service.class;
            List<ResolverMethod> repeating = resolverClass.getAllByAnnotation(repeatingTaskClass);
            long time = resolverClass.getTime();
            repeating.forEach(m -> {
                Method rootMethod = m.getMethod();
                RepeatingTask taskAnnot = rootMethod.getDeclaredAnnotation(repeatingTaskClass);
                if(time >= taskAnnot.delay() && time % taskAnnot.period() == 0) {
                    Runnable runnable = () -> resolverClass.invokeMethodCustom(m, m1 -> true, Long.valueOf((long) (((double) time) / 50.0)));
                    if(rootMethod.isAnnotationPresent(Async.class)) {
                        resolverClass.getService().getExecutorService().submit(runnable);
                    } else {
                        if(taskAnnot.queueEnabled() && !resolverClass.getInProgressMethods().contains(rootMethod)) {
                            resolverClass.getInProgressMethods().add(rootMethod);
                            runnable.run();
                            resolverClass.getInProgressMethods().remove(rootMethod);
                        } else {
                            runnable.run();
                        }
                    }
                }
            });
            List<ResolverMethod> delayed = resolverClass.getAllByAnnotation(taskClass);
            delayed.forEach(m -> {
                Method rootMethod = m.getMethod();
                Task taskAnnot = rootMethod.getDeclaredAnnotation(taskClass);
                if(time == taskAnnot.delay()) {
                    Runnable runnable = () -> resolverClass.invokeMethod(m);
                    if(rootMethod.isAnnotationPresent(Async.class)) {
                        resolverClass.getService().getExecutorService().submit(runnable);
                    } else {
                        runnable.run();
                    }
                }
            });
            Service service1 = resolverClass.getClazz().getDeclaredAnnotation(serviceClass);
            if(resolverClass.getSqlHandlerAnnot() != null && resolverClass.getInstance() instanceof SqlKeysPresent) {
                GlobalOrganizedDataStorage dataStorage = resolverClass.getDataStorage();
                SqlHandler sqlHandler = resolverClass.getSqlHandlerAnnot();
                Class<? extends SqlDownload> sqlDownloadclass = SqlDownload.class;
                Class<? extends SqlUpload> sqlUploadclass = SqlUpload.class;
                Predicate<ResolverMethod> downloadPred = resolverMethod -> {
                    SqlDownload sqlDownload = resolverMethod.getMethod()
                            .getDeclaredAnnotation(SqlDownload.class);
                    long interval = sqlDownload.interval();
                    return time % interval == 0;
                };
                Predicate<ResolverMethod> uploadPred = resolverMethod -> {
                    SqlUpload sqlUpload = resolverMethod.getMethod()
                            .getDeclaredAnnotation(SqlUpload.class);
                    long interval = sqlUpload.interval();
                    return time % interval == 0;
                };
                if(resolverClass.existMethodByCondition(sqlDownloadclass, downloadPred) || resolverClass.existMethodByCondition(sqlUploadclass, uploadPred)) {
                    List<ResolverMethod> keysMethods = resolverClass.getAllByAnnotation(SqlLookupKeys.class).stream()
                            .filter(resolverMethod -> {
                                Type type = new TypeToken<List<String>>(){}.getType();
                                return resolverMethod.getMethod()
                                        .getGenericReturnType().equals(type);
                            })
                            .collect(Collectors.toList());
                    List<ResolverMethod> downloadMethods = resolverClass.getAllByAnnotation(SqlDownload.class).stream()
                            .filter(downloadPred)
                            .collect(Collectors.toList());
                    List<ResolverMethod> uploadMethods = resolverClass.getAllByAnnotation(SqlUpload.class).stream()
                            .filter(uploadPred)
                            .collect(Collectors.toList());
                    if(keysMethods.size() > 0) {
                        ResolverMethod keysMethod = keysMethods.get(0);
                        Runnable downloadRunnable = () -> {
                            List<String> keys = (List<String>) keysMethod.invoke().orElse(null);
                            if(keys == null) {
                                XList<Row> rows = dataStorage.select("*", !sqlHandler.condition().equals("") ? sqlHandler.condition() : null);
                                Map<String, String> rowsMap = Maps.newHashMap();
                                Map<String, LocalDateTime> timestampsMap = Maps.newHashMap();
                                rows.forEach(row -> {
                                    rowsMap.put(row.get(sqlHandler.colKey()), row.get(sqlHandler.colValue()));
                                    Nullables.simpleCatch(() -> {
                                        if(sqlHandler.includeTimestamp()) {
                                            long timestamp = row.getLong("time");
                                            LocalDateTime date = LocalDateTime.ofInstant(Instant.ofEpochMilli(timestamp), ZoneId.systemDefault());
                                            timestampsMap.put(row.get(sqlHandler.colKey()), date);
                                        }
                                    });
                                });
                                downloadMethods.forEach(resolverMethod -> {
                                    rowsMap.forEach((k, v) -> {
                                        SqlDownload sqlDownload = resolverMethod.getMethod()
                                                .getDeclaredAnnotation(SqlDownload.class);
                                        Class<?> parserValueType = sqlDownload.deserializeType();
                                        Object pv = service.getGson().fromJson(v, parserValueType);
                                        JsonElement jsonElement = JsonParser.parseString(v);
                                        List<Object> paramObjects = Lists.newArrayList(k, jsonElement, pv, rowsMap);
                                        if(timestampsMap.containsKey(k)) paramObjects.add(timestampsMap.get(k));
                                        resolverMethod.invokeCustom(paramObjects.toArray());
                                    });
                                });
                            } else {
                                keys.forEach(key -> {
                                    XList<Row> rows = dataStorage.select("*", GlobalOrganizedDataStorage.createCondition(sqlHandler.colKey(), key));
                                    Map<String, String> rowsMap = Maps.newHashMap();
                                    Map<String, LocalDateTime> timestampsMap = Maps.newHashMap();
                                    rows.forEach(row -> {
                                        rowsMap.put(row.get(sqlHandler.colKey()), row.get(sqlHandler.colValue()));
                                        Nullables.simpleCatch(() -> {
                                            if(sqlHandler.includeTimestamp()) {
                                                long timestamp = row.getLong("time");
                                                LocalDateTime date = LocalDateTime.ofInstant(Instant.ofEpochMilli(timestamp), ZoneId.systemDefault());
                                                timestampsMap.put(row.get(sqlHandler.colKey()), date);
                                            }
                                        });
                                    });
                                    rowsMap.forEach((k, v) -> downloadMethods.forEach(resolverMethod -> {
                                        SqlDownload sqlDownload = resolverMethod.getMethod()
                                                .getDeclaredAnnotation(SqlDownload.class);
                                        Class<?> parserValueType = sqlDownload.deserializeType();
                                        Object pv = service.getGson().fromJson(v, parserValueType);
                                        JsonElement jsonElement = JsonParser.parseString(v);
                                        List<Object> paramObjects = Lists.newArrayList(k, jsonElement, pv, rowsMap);
                                        if(timestampsMap.containsKey(k)) paramObjects.add(timestampsMap.get(k));
                                        resolverMethod.invokeCustom(paramObjects.toArray());
                                    }));
                                });
                            }
                        };
                        Runnable uploadRunnable = () -> {
                            Type mapType = new TypeToken<Map<String, Object>>(){}.getType();
                            uploadMethods.forEach(uploadMethod -> {
                                if(uploadMethod.getMethod().getGenericReturnType().equals(mapType)) {
                                    Optional<Object> resultOptional = uploadMethod.invoke();
                                    if(resultOptional.isPresent()) {
                                        Map<String, Object> map = (Map<String, Object>) resultOptional.get();
                                        map.forEach((key, value) -> {
                                            String condition = GlobalOrganizedDataStorage.createCondition(sqlHandler.colKey(), key);
                                            if(dataStorage.contains(condition)) {
                                                Map<String, String> data = Maps.newHashMap();
                                                data.put(sqlHandler.colKey(), key);
                                                data.put(sqlHandler.colValue(), service.getGson().toJson(value));
                                                if(sqlHandler.includeTimestamp()) {
                                                    data.put("time", String.valueOf(System.currentTimeMillis()));
                                                }
                                                dataStorage.update(data, condition);
                                            } else {
                                                Row row = new Row()
                                                        .with(sqlHandler.colKey(), key)
                                                        .with(sqlHandler.colValue(), service.getGson().toJson(value));
                                                if(sqlHandler.includeTimestamp()) {
                                                    row = row.with("time", String.valueOf(System.currentTimeMillis()));
                                                }
                                                dataStorage.insert(row);
                                            }
                                        });
                                    }
                                }
                            });
                        };
                        resolverClass.getService().getExecutorService()
                                .submit(downloadRunnable);
                        resolverClass.getService().getExecutorService()
                                .submit(uploadRunnable);
                    }
                }
            }
            boolean canContinue = service1.lim() < 0 || time < service1.lim();
            if(!canContinue) {
                List<ResolverMethod> repeatingShutdown = resolverClass.getAllByAnnotation(repeatingTaskShutdownClass);
                repeatingShutdown.forEach(m -> resolverClass.invokeMethod(m));
            }
            return canContinue;
        };
    }

    @Override
    public void complete() {
        this.done = true;
    }

}
