package net.ramixin.mixson;

import net.minecraft.resources.Identifier;
import net.ramixin.mixson.enums.ErrorPolicy;
import net.ramixin.mixson.enums.Lifetime;
import net.ramixin.mixson.util.Index;
import net.ramixin.mixson.util.functions.Event;
import net.ramixin.mixson.util.interfaces.ErrorMessageProvider;
import net.ramixin.mixson.util.interfaces.MixsonCodec;
import org.jetbrains.annotations.ApiStatus;

import java.util.UUID;
import java.util.function.Predicate;

@ApiStatus.Internal
public record MixsonEvent<T>(UUID uuid, MixsonCodec<T> codec, int priority, Lifetime lifetime, ErrorPolicy errorPolicy, String eventName, Predicate<Index> resourcePredicate, Event<T> event, boolean assertive) implements ErrorMessageProvider {

    public MixsonEvent(MixsonCodec<T> codec, int priority, Lifetime lifetime, ErrorPolicy errorPolicy, String eventName, Predicate<Index> resourcePredicate, Event<T> event, boolean assertive) {
        this(UUID.randomUUID(), codec, priority, lifetime, errorPolicy, eventName, resourcePredicate, event, assertive);
    }

    @Override
    public String getRuntimeErrorMessage(Identifier resourceId) {
        return String.format("Failed to interact with %s file '%s' with event '%s'\n", codec.extensionAndDot(), resourceId, eventName);
    }

    @Override
    public ErrorPolicy getErrorPolicy() {
        return errorPolicy;
    }

    public Predicate<Index> getWrappedPredicate() {
        return (index) -> {
            String ext = codec.extensionAndDot();
            Identifier id = index.id();
            if(!id.getPath().endsWith(ext))
                return false;
            Identifier trimmedLocation = Identifier.fromNamespaceAndPath(id.getNamespace(), id.getPath().substring(0, id.getPath().length() - ext.length()));
            return resourcePredicate.test(new Index(trimmedLocation, index.ordinal()));
        };
    }

    @Override
    public String getRegistrationMessage(int priority) {
        return String.format("Registering event '%s' with priority %s", eventName, priority);
    }
}
