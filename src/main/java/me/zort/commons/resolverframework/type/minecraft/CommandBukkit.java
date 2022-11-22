package me.zort.commons.resolverframework.type.minecraft;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface CommandBukkit {

    String value();
    String description() default "Basic command description";
    String usageMessage() default "Basic command usage";
    String[] aliases() default {};

}
