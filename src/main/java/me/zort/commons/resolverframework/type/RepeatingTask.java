package me.zort.commons.resolverframework.type;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface RepeatingTask {

    boolean queueEnabled() default false;
    long delay() default 0L;
    long period() default 20L;

}
