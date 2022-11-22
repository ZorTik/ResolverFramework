package me.zort.commons.resolverframework.type.configuration;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface Path {

    SourceAccessMoment moment() default SourceAccessMoment.EVERY;
    SourceType type() default SourceType.FILE;
    String value();
    boolean bukkit() default false;

}
