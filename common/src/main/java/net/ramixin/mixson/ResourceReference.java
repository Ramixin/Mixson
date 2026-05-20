package net.ramixin.mixson;

import net.minecraft.resources.Identifier;
import net.ramixin.mixson.enums.ErrorPolicy;
import net.ramixin.mixson.util.Index;
import net.ramixin.mixson.util.interfaces.ErrorMessageProvider;
import net.ramixin.mixson.util.interfaces.MixsonCodec;

import java.util.Optional;
import java.util.UUID;

@SuppressWarnings("unused")
public class ResourceReference<T> implements ErrorMessageProvider {

    private T resource;
    private final String referenceName;
    private final Index index;
    private final UUID uuid = UUID.randomUUID();
    private final MixsonCodec<T> codec;
    private final int priority;

    protected ResourceReference(MixsonCodec<T> codec, int priority, Index index, String referenceName) {
        this.index = index;
        this.referenceName = referenceName;
        this.codec = codec;
        this.priority = priority;
    }

    public Optional<T> retrieve() {
        return Optional.ofNullable(resource);
    }

    public Optional<T> consume() {
        Optional<T> elem = retrieve();
        clear();
        return elem;
    }

    public void fulfill(T elem) {
        this.resource = elem;
    }

    public String getName() {
        return referenceName;
    }

    public Index getIndex() {
        return index;
    }

    public Identifier getResourceId() {
        return index.id();
    }

    public UUID getUuid() {
        return uuid;
    }

    public int getOrdinal() {
        return index.ordinal();
    }

    public MixsonCodec<T> getCodec() {
        return codec;
    }

    public int getPriority() {
        return priority;
    }

    @Override
    public String getRuntimeErrorMessage(Identifier resourceId) {
        return String.format("Failed to capture %s file '%s' for reference '%s'\n", codec.extensionAndDot(), resourceId, referenceName);
    }

    @Override
    public ErrorPolicy getErrorPolicy() {
        return ErrorPolicy.THROW;
    }

    @Override
    public String getRegistrationMessage(int priority) {
        return String.format("Registering reference '%s' with priority %s", referenceName, priority);
    }

    protected void clear() {
        resource = null;
    }
}
