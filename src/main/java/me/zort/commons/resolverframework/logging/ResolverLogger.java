package me.zort.commons.resolverframework.logging;

import lombok.Getter;
import me.zort.commons.common.cache.ExpiringMapCache;
import me.zort.commons.resolverframework.ResolverService;

import java.time.temporal.ChronoUnit;

public class ResolverLogger {

    public static ResolverLogger createLogger(Class<?> clazz, ResolverService service) {
        return createLogger(clazz, service, false, 0L, ChronoUnit.SECONDS);
    }

    public static ResolverLogger createLogger(Class<?> clazz, ResolverService service, boolean preventSpam, long maxMessageInterval, ChronoUnit maxMessageIntervalUnit) {
        return new ResolverLogger(clazz, service, preventSpam, maxMessageInterval, maxMessageIntervalUnit);
    }

    @Getter
    private final ExpiringMapCache<String, Integer> duplicatesCache;

    private final ResolverService service;
    private final Class<?> clazz;

    public ResolverLogger(Class<?> clazz, ResolverService service) {
        this(clazz, service, false, 0L, ChronoUnit.SECONDS);
    }

    public ResolverLogger(Class<?> clazz, ResolverService service, boolean preventSpam, long maxMessageInterval, ChronoUnit maxMessageIntervalUnit) {
        this.service = service;
        this.clazz = clazz;
        this.duplicatesCache = preventSpam
                ? ExpiringMapCache.newCache((k, v) -> {}, maxMessageInterval, maxMessageIntervalUnit, true)
                : null;
    }

    public void bc(ResolverMessage message, String... replacements) {
        bc(message.get(replacements));
    }

    public void bc(String message) {
        StringBuilder sb = new StringBuilder("[" + clazz.getName() + "] RESOLVER");
        sb.append(" :: " + message);
        System.out.println(sb.toString());
    }

    public void info(ResolverMessage message, String... replacements) {
        info(message.get(replacements));
    }

    public void warn(ResolverMessage message, String... replacements) {
        warn(message.get(replacements));
    }

    public void err(ResolverMessage message, String... replacements) {
        err(message.get(replacements));
    }

    public void crit(ResolverMessage message, String... replacements) {
        crit(message.get(replacements));
    }

    public void info(String message) {
        if(duplicatesCache != null) {
            if(!duplicatesCache.containsKey(message)) {
                duplicatesCache.put(message, 1);
            } else {
                duplicatesCache.put(message, duplicatesCache.get(message) + 1);
                return;
            }
        }
        System.out.println(construct("INFO", message));
    }

    public void warn(String message) {
        System.out.println(construct("WARNING", message));
    }

    public void err(String message) {
        System.out.println(construct("ERROR", message));
    }

    public void crit(String message) {
        System.out.println(construct("CRITICAL", message));
    }

    public void debug(ResolverMessage message, String... replacements) {
        debug(message.get(replacements));
    }

    public void debug(String code, String message) {
        service.debug(code, message);
    }

    public void debug(String message) {
        if(!service.isDebug()) return;
        System.out.println(construct("DEBUG", message));
    }

    public void log(ResolverLoggerLevel level, String message) {
        System.out.println(construct(level.name(), message));
    }

    private String construct(String level, String message) {
        StringBuilder sb = new StringBuilder("[" + clazz.getName() + "] " + level.toUpperCase());
        sb.append(" -- " + message);
        return sb.toString();
    }

}
