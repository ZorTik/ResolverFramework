package me.zort.commons.resolverframework.logging;

import java.util.concurrent.atomic.AtomicReference;

public enum ResolverMessage {

    STARTING("Starting resolver initialized on package space %s"),
    DEBUG_NO_SCHEDULED_SERVICE_LOADED("Auto stopping service %s because no scheduled service was found."),
    ERR_REFLECTIONS_NOT_PRESENT_STARTUP("Cannot resolve package space. Reflections are not present.");

    private final String message;

    ResolverMessage(String message) {
        this.message = message;
    }

    public String get(final String... replacements) {
        AtomicReference<String> s = new AtomicReference<>(message);
        return s.updateAndGet(s1 -> {
            for(String replacement : replacements) {
                s1 = s1.replaceFirst("%s", replacement);
            }
            return s1;
        });
    }

}
