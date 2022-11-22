package me.zort.commons.resolverframework.type;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface Service {

    int constructionCount() default 1;
    String serviceName() default "none";
    boolean scheduled() default false;
    long delay() default 0L;
    long lim() default -1L;
    boolean paused() default false;
    String condition() default "";

    boolean bukkitListener() default false;
    int eventsPriorityIndex() default 2;
    boolean bukkit() default false;

}
