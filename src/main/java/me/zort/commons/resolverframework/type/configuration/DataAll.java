package me.zort.commons.resolverframework.type.configuration;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.PARAMETER, ElementType.FIELD})
public @interface DataAll {

    /**
     *  Data Source
     */
    Class<?> source();
    MapColumn col() default MapColumn.KEYS;

}
