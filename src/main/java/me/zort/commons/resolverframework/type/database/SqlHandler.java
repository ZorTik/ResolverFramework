package me.zort.commons.resolverframework.type.database;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface SqlHandler {

    boolean includeTimestamp() default true;
    String condition() default "";
    String colKey();
    String colValue();
    String table();

}
