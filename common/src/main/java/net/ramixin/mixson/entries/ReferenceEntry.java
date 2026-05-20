package net.ramixin.mixson.entries;

import net.ramixin.mixson.ResourceReference;
import org.jetbrains.annotations.ApiStatus;

@ApiStatus.Internal
public class ReferenceEntry<T> extends AbstractEntry {

    private final ResourceReference<T> reference;

    public ReferenceEntry(int priority, ResourceReference<T> event) {
        super(priority);
        this.reference = event;
    }

    public ResourceReference<T> reference() {
        return reference;
    }

}
