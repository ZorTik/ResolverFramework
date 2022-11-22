package me.zort.commons.resolverframework.boot;

import me.zort.commons.resolverframework.ResolvingPriority;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.ANNOTATION_TYPE)
public @interface PackageSpaceAppender {

    String packageSpace();

    ResolvingPriority priority() default ResolvingPriority.MEDIUM;
    /**
     * Determines local class method which returns condition for appending package space.
     * FORMAT: methodName
     *
     * Note: Method must return boolean and must be without parameters.
     *
     * @return Condition method
     */
    String conditionMethod() default "";

}
