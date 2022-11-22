package me.zort.commons.resolverframework.lifecycle;

import lombok.Getter;
import me.zort.commons.resolverframework.ResolverService;
import me.zort.commons.resolverframework.exceptions.ConfigurerException;
import me.zort.commons.resolverframework.type.configuration.Configurer;

public abstract class ResolverConfigurer {

    @Getter
    private final ResolverService service;
    @Getter
    private final Class<?> clazz;
    @Getter
    private final Configurer configurerAnnot;

    public ResolverConfigurer(ResolverService service, Class<?> clazz) throws ConfigurerException {
        if(!clazz.isAnnotationPresent(Configurer.class)) {
            throw new ConfigurerException("Class " + clazz.getName() + " is not a configurer.");
        }
        this.service = service;
        this.clazz = clazz;
        this.configurerAnnot = clazz.getDeclaredAnnotation(Configurer.class);
    }

    public abstract ResolverClass getResolverClass();
    public abstract boolean configure();
    public abstract boolean postConfigure();
    public abstract void deconfigure();

}
