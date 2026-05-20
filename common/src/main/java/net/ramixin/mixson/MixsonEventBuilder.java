package net.ramixin.mixson;

import net.ramixin.mixson.enums.ErrorPolicy;
import net.ramixin.mixson.enums.Lifetime;
import net.ramixin.mixson.util.Index;
import net.ramixin.mixson.util.functions.Event;
import net.ramixin.mixson.util.interfaces.MixsonCodec;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;
import java.util.function.Predicate;

public class MixsonEventBuilder<T> {

    private MixsonCodec<T> codec;
    private Predicate<Index> resourcePredicate;
    private String eventName;
    private Event<T> event;
    private boolean assertive = false;
    private Lifetime lifetime;
    private ErrorPolicy errorPolicy;
    private int priority = Mixson.DEFAULT_PRIORITY;

    public MixsonEventBuilder<T> setErrorPolicy(ErrorPolicy errorPolicy) {
        this.errorPolicy = errorPolicy;
        return this;
    }

    public MixsonEventBuilder<T> setCodec(MixsonCodec<T> codec) {
        this.codec = codec;
        return this;
    }

    public MixsonEventBuilder<T> setResourcePredicate(Predicate<Index> resourcePredicate) {
        this.resourcePredicate = resourcePredicate;
        return this;
    }

    public MixsonEventBuilder<T> setEventName(String eventName) {
        this.eventName = eventName;
        return this;
    }

    public MixsonEventBuilder<T> setEvent(Event<T> event) {
        this.event = event;
        return this;
    }

    public MixsonEventBuilder<T> setAssertive(boolean assertive) {
        this.assertive = assertive;
        return this;
    }

    public MixsonEventBuilder<T> setLifetime(Lifetime lifetime) {
        this.lifetime = lifetime;
        return this;
    }

    public MixsonEventBuilder<T> setPriority(int priority) {
        this.priority = priority;
        return this;
    }

    protected @NotNull MixsonEvent<T> build() {
        Objects.requireNonNull(codec, "codec must be set");
        Objects.requireNonNull(resourcePredicate, "resource predicate must be set");
        Objects.requireNonNull(event, "event must be set");
        Objects.requireNonNull(eventName, "event name must be set");
        Objects.requireNonNull(lifetime, "lifetime must be set");
        Objects.requireNonNull(errorPolicy, "error policy must be set");

        MixsonEvent<T> builtEvent = new MixsonEvent<>(codec, priority, lifetime, errorPolicy, eventName, resourcePredicate, event, assertive);
        Mixson.logBasic(builtEvent.getRegistrationMessage(priority));
        return builtEvent;
    }

    public MixsonEventBuilder<T> copy() {
        return new MixsonEventBuilder<T>()
                .setCodec(codec)
                .setResourcePredicate(resourcePredicate)
                .setEventName(String.valueOf(eventName))
                .setEvent(event)
                .setAssertive(assertive)
                .setErrorPolicy(errorPolicy)
                .setPriority(priority)
                .setLifetime(lifetime);
    }
}
