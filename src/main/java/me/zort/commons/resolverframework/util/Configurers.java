package me.zort.commons.resolverframework.util;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import me.zort.commons.resolverframework.lifecycle.ResolverConfigurer;
import me.zort.commons.resolverframework.lifecycle.ResolverConfigurer_Data;
import me.zort.commons.resolverframework_dev.ResolverService;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.function.Consumer;

public class Configurers {

    public static Configurers load(ResolverService service) {
        return new Configurers(service);
    }

    @Getter(AccessLevel.PROTECTED)
    private final ResolverService service;

    @Getter
    private final Configurers_Data dataConfigurers;

    private Configurers(ResolverService service) {
        this.service = service;
        this.dataConfigurers = new Configurers_Data(this);
    }

    @AllArgsConstructor
    public class Configurers_Data {

        private final Configurers configurers;

        @Nullable
        public ResolverConfigurer_Data getByClass(Class<?> configurer) {
            Map<Class<?>, ResolverConfigurer> configurers = ((me.zort.commons.resolverframework.ResolverService) this.configurers.getService()).getConfigurers();
            if(!configurers.containsKey(configurer)) return null;
            ResolverConfigurer resolverConfigurer = configurers.get(configurer);
            if(!(resolverConfigurer instanceof ResolverConfigurer_Data)) return null;
            return (ResolverConfigurer_Data) resolverConfigurer;
        }

        public boolean set(Class<?> configurer, String key, Object value) {
            ResolverConfigurer_Data resolverConfigurer = getByClass(configurer);
            if(resolverConfigurer == null) return false;
            resolverConfigurer.getData().put(key, value);
            return true;
        }

        public boolean remove(Class<?> configurer, String key) {
            ResolverConfigurer_Data resolverConfigurer = getByClass(configurer);
            if(resolverConfigurer == null) return false;
            resolverConfigurer.getData().remove(key);
            return true;
        }

        public boolean containsKey(Class<?> configurer, String key) {
            ResolverConfigurer_Data resolverConfigurer = getByClass(configurer);
            if(resolverConfigurer == null) return false;
            return resolverConfigurer.getData().containsKey(key);
        }

        public boolean clear(Class<?> configurer) {
            ResolverConfigurer_Data resolverConfigurer = getByClass(configurer);
            if(resolverConfigurer == null) return false;
            resolverConfigurer.getData().clear();
            return true;
        }

        public boolean custom(Class<?> configurer, Consumer<ResolverConfigurer_Data> action) {
            ResolverConfigurer_Data resolverConfigurer = getByClass(configurer);
            if(resolverConfigurer == null) return false;
            action.accept(resolverConfigurer);
            return true;
        }

    }

}
