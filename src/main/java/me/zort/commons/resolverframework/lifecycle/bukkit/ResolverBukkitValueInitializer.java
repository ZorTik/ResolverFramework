package me.zort.commons.resolverframework.lifecycle.bukkit;

import me.zort.commons.resolverframework.ResolverService;
import me.zort.commons.resolverframework.util.ResolverUtil;

import java.util.Optional;
import java.util.function.BiFunction;

public enum ResolverBukkitValueInitializer {

    EVENT_PLAYER((service, event) -> {
        return ResolverUtil.blankMethodResult(event, "getPlayer");
    });

    private final BiFunction<ResolverService, Object, Object> dataObject;

    ResolverBukkitValueInitializer(BiFunction<ResolverService, Object, Object> dataObject) {
        this.dataObject = dataObject;
    }

    public Optional<Object> initialize(ResolverService service, Object input) {
        return Optional.ofNullable(dataObject.apply(service, input));
    }

}
