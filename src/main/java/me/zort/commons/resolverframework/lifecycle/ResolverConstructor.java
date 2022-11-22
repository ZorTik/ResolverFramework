package me.zort.commons.resolverframework.lifecycle;

import lombok.Getter;
import me.zort.commons.resolverframework.logging.ResolverLogger;
import me.zort.commons.resolverframework.type.Value;

import java.lang.reflect.Constructor;
import java.lang.reflect.Parameter;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.stream.Collectors;

public class ResolverConstructor {

    private final ResolverLogger logger;
    private final ResolverClassImpl clazz;
    @Getter
    private final Constructor constructor;

    public ResolverConstructor(ResolverClassImpl clazz, Constructor constructor) {
        this.logger = ResolverLogger.createLogger(ResolverMethod.class, clazz.getService());
        this.clazz = clazz;
        this.constructor = constructor;
    }

    public Optional<Object> invoke() {
        return invokeCustom();
    }

    public Optional<Object> invokeCustom(Object... customValues) {
        Object[] parameterValues = new Object[constructor.getParameterCount()];
        int i = 0;
        Random random = new Random();
        for(Parameter p : constructor.getParameters()) {
            Value value = p.getAnnotation(Value.class);
            //Object autoValue = clazz.initAutoValueCustomized(value, p.getType(), customValues);
            List<Object> forParameter = Arrays.stream(customValues)
                    .filter(customValue -> customValue.getClass().equals(p.getType()))
                    .collect(Collectors.toList());
            Object autoValue = forParameter.isEmpty() ? clazz.initAutoValue(p, p.getType(), customValues) : forParameter.get(random.nextInt(forParameter.size()));
            logger.debug("Constructor invocation parameter " + p.getName() + "[" + p.getType() + "] value " + i + ": " + autoValue);
            parameterValues[i] = autoValue;
            i++;
        }
        try {
            /*Object result = customValues.length > 0
                    ? constructor.newInstance(parameterValues)
                    : constructor.newInstance();*/
            Object result = constructor.newInstance(parameterValues);
            return Optional.ofNullable(result);
        } catch(Exception ex) {
            logger.err("Cannot invoke constructor {"
                    + constructor.getName()
                    + "} to " + clazz.getClazz().getName()
                    + ": " + ex.getMessage());
            ex.printStackTrace();
            return Optional.empty();
        }
    }

}
