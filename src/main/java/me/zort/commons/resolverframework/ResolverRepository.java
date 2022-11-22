package me.zort.commons.resolverframework;

import com.google.common.collect.Maps;
import me.zort.commons.resolverframework.util.ResolverUtil;
import org.jetbrains.annotations.Nullable;
import org.reflections.Reflections;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;

public class ResolverRepository {

    private static Map<String, ResolverService> cache = Maps.newHashMap();
    private static Map<String, Reflections> reflections = Maps.newHashMap();

    public static Optional<ResolverService> getByPackageSpace(String packageSpace) {
        return Optional.ofNullable(cache.getOrDefault(packageSpace, null));
    }

    public static Optional<Reflections> getReflectionsByPackageSpace(String packageSpace) {
        return Optional.ofNullable(reflections.getOrDefault(packageSpace, null));
    }

    public static Collection<ResolverService> getAll() {
        return cache.values();
    }

    protected static void add(ResolverService service) {
        add(service, null);
    }

    protected static void add(ResolverService service, @Nullable ClassLoader classLoader) {
        String packageSpace = service.getPackageSpace();
        Optional<ResolverService> byPackageSpace = getByPackageSpace(packageSpace);
        if(!byPackageSpace.isPresent()) {
            Reflections reflections1 = ResolverUtil.newReflections(packageSpace, classLoader);
            reflections.put(packageSpace, reflections1);
        }
        cache.putIfAbsent(packageSpace, service);
    }

}
