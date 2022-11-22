package me.zort.commons.resolverframework.type.configuration;

import me.zort.commons.resolverframework.lifecycle.ResolverConfigurer;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface Configurer {

    Class<? extends ResolverConfigurer> value();

}
