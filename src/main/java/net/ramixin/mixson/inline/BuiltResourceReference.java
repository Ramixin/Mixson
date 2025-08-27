package net.ramixin.mixson.inline;

import net.minecraft.resources.ResourceLocation;
import net.ramixin.mixson.util.ErrorMessageProvider;
import net.ramixin.mixson.util.ResourceLocationUtil;

import java.util.Optional;
import java.util.UUID;

public class BuiltResourceReference<T> implements ErrorMessageProvider {

    private T resource;

    private final ResourceLocation referenceId;

    private final ResourceLocation resourceId;

    private final int ordinal;

    private final UUID uuid = UUID.randomUUID();

    private final MixsonCodec<T> codec;

    protected BuiltResourceReference(ResourceReference reference, MixsonCodec<T> codec) {
        if(reference.ordinal() == -1) throw new IllegalArgumentException(String.format("Ordinal for resource reference: %s cannot be -1", reference.referenceId()));
        if(reference.ordinal() < 0) throw new IllegalArgumentException(String.format("Ordinal for resource reference: %s cannot be negative", reference.referenceId()));
        this.resourceId = ResourceLocationUtil.parse(reference.resourceId());
        this.referenceId = ResourceLocationUtil.parse(reference.referenceId());
        this.ordinal = reference.ordinal();
        this.codec = codec;
    }

    public Optional<T> retrieve() {
        if(resource == null) return Optional.empty();
        return Optional.of(resource);
    }

    public void fulfill(T elem) {
        this.resource = elem;
    }

    public ResourceLocation getReferenceId() {
        return referenceId;
    }

    public ResourceLocation getResourceId() {
        return resourceId;
    }

    public UUID getUuid() {
        return uuid;
    }

    public int getOrdinal() {
        return ordinal;
    }

    public MixsonCodec<T> getCodec() {
        return codec;
    }

    @Override
    public String getRuntimeMessage(ResourceLocation resourceId) {
        return String.format("Failed to capture %s file '%s' for reference '%s'\n", codec.extensionAndDot(), resourceId, referenceId);
    }

    @Override
    public String getRegistrationMessage() {
        return String.format("Failed to register reference '%s' for file '%s'\n", referenceId, resourceId);
    }

    @Override
    public boolean failSilently() {
        return false;
    }

    protected void clear() {
        resource = null;
    }
}
