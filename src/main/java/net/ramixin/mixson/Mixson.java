package net.ramixin.mixson;


import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import net.minecraft.resource.Resource;
import net.minecraft.util.Identifier;

import java.io.ByteArrayInputStream;
import java.util.*;

public class Mixson {

    private static final TreeMap<Integer, Set<AssociatedMixsonEvent>> events = new TreeMap<>();


    public static void registerModificationEvent(Identifier resourceId, Identifier eventId, final MixsonEvent event) {
        registerModificationEvent(1000, resourceId, eventId, event);
    }

    public static void registerModificationEvent(int priority, Identifier resourceId, Identifier eventId, final MixsonEvent event) {
        Set<AssociatedMixsonEvent> eventSet;
        if(events.get(priority) == null) eventSet = new HashSet<>();
        else eventSet = events.get(priority);
        eventSet.add(new AssociatedMixsonEvent(resourceId.withSuffixedPath(".json"), eventId, event));
        events.put(priority, eventSet);
    }

    public record AssociatedMixsonEvent(Identifier resourceId, Identifier eventId, MixsonEvent event) {}

    public static Map<Identifier, Resource> runEvents(Map<Identifier, Resource> original) {
        for (Map.Entry<Identifier, Resource> entry : original.entrySet()) {
            Identifier id = entry.getKey();
            if(!id.getPath().endsWith(".json")) continue;
            Resource resource = entry.getValue();
            JsonElement cachedResource = null;
            for (Set<AssociatedMixsonEvent> eventSet : Mixson.events.sequencedValues()) for (AssociatedMixsonEvent event : eventSet) {
                if (!event.resourceId().equals(id)) continue;
                try {
                    if (cachedResource == null)
                        cachedResource = JsonParser.parseReader(entry.getValue().getReader());
                    cachedResource = event.event().run(cachedResource);
                } catch (Exception e) {
                    throw new MixsonError(String.format("Failed to modify json file '%s' with event '%s'\n", id, event.eventId()) + e);
                }
            }
            if(cachedResource != null) {
                JsonElement finalCachedResource = cachedResource;
                original.put(id, new Resource(resource.getPack(), () -> new ByteArrayInputStream(finalCachedResource.toString().getBytes()), resource::getMetadata));
            }
        }
        return original;
    }

    @FunctionalInterface
    public interface MixsonEvent {

        JsonElement run(JsonElement elem);

    }
}
