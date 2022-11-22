package me.zort.commons.resolverframework.lifecycle;

import lombok.Getter;
import me.zort.commons.resolverframework.logging.ResolverLogger;

import java.lang.reflect.Field;

public class ResolverField {

    private final ResolverLogger logger;
    private final ResolverClassImpl clazz;
    @Getter
    private final Field field;
    @Getter
    private final Class<?> type;

    public ResolverField(ResolverClassImpl clazz, Field field) {
        this.logger = ResolverLogger.createLogger(ResolverField.class, clazz.getService());
        this.clazz = clazz;
        this.field = field;
        this.type = field.getType();
    }

    public void set() {
        field.setAccessible(true);
        Object value = clazz.initAutoValue(field, type);
        logger.debug("Auto field type " + type + " value " + value);
        try {
            field.set(clazz.getInstance(), value);
        } catch (IllegalAccessException ex) {
            logger.err("Cannot set field {"
                    + field.getName()
                    + "} to " + clazz.getInstance().getClass().getName()
                    + ": " + ex.getMessage());
            ex.printStackTrace();
        }
    }

}
