package me.zort.commons.resolverframework.lifecycle;

import lombok.Getter;
import me.zort.commons.resolverframework.logging.ResolverLogger;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

public class ResolverMethod {

    private final ResolverLogger logger;

    private final ResolverClassImpl clazz;
    @Getter
    private final Method method;
    @Getter
    private final Class<?> type;

    private final ExecutorService service;

    public ResolverMethod(ResolverClassImpl<? extends ResolverClassImpl<?>> clazz, Method method) {
        this.logger = ResolverLogger.createLogger(ResolverMethod.class, clazz.getService());
        this.clazz = clazz;
        this.method = method;
        this.type = method.getReturnType();

        this.service = Executors.newCachedThreadPool();
    }

    public Optional<Object> invoke() {
        return invokeCustom();
    }

    public Optional<Object> invokeCustom(Object... customValues) {
        Object[] parameterValues = new Object[method.getParameterCount()];
        Random random = new Random();
        int i = 0;
        for(Parameter p : method.getParameters()) {
            List<Object> forParameter = Arrays.stream(customValues)
                    .filter(customValue -> customValue.getClass().equals(p.getType()))
                    .collect(Collectors.toList());
            Object autoValue = forParameter.isEmpty() ? clazz.initAutoValue(p, p.getType(), customValues) : forParameter.get(random.nextInt(forParameter.size()));
            logger.debug("invocation-repeated", "Method invocation parameter " + p.getName() + "[" + p.getType() + "] value " + i + ": " + autoValue);
            parameterValues[i] = autoValue;
            i++;
        }
        try {
            Object result = method.invoke(clazz.getInstance(), parameterValues);
            return Optional.ofNullable(result);
        } catch(Exception ex) {
            System.out.println(Arrays.toString(parameterValues));
            String name = clazz.getInstance() != null ? clazz.getInstance().getClass().getName() : "[not instantinated]";
            logger.err("Cannot invoke method {"
                    + method.getName()
                    + "} to " + name
                    + ": " + ex.getMessage());
            ex.printStackTrace();
            return Optional.empty();
        }
    }

}
