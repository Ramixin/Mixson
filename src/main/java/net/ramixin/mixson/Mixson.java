package net.ramixin.mixson;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import net.minecraft.resource.Resource;
import net.minecraft.util.Identifier;
import net.minecraft.util.JsonHelper;
import org.jetbrains.annotations.NotNull;

import java.io.ByteArrayInputStream;
import java.util.Iterator;
import java.util.Map;
import java.util.TreeSet;

public class Mixson {

     private static final TreeSet<AssociatedMixsonEvent> events = new TreeSet<>();


    public static void registerModificationEvent(Identifier resourceId, Identifier eventId, final MixsonEvent event) {
        registerModificationEvent(1000, resourceId.withSuffixedPath(".json"), eventId, event);
    }

    public static void registerModificationEvent(int priority, Identifier resourceId, Identifier eventId, final MixsonEvent event) {
        events.add(new AssociatedMixsonEvent(resourceId, eventId, event, priority));
    }

    public static Iterator<AssociatedMixsonEvent> getEventIterators() {
        return events.iterator();
    }

    public record AssociatedMixsonEvent(Identifier resourceId, Identifier eventId, MixsonEvent event, int priority) implements Comparable<AssociatedMixsonEvent> {

        @Override
        public int compareTo(@NotNull Mixson.AssociatedMixsonEvent o) {
            return o.priority - priority;
        }
    }

    public static Map<Identifier, Resource> runEvents(Map<Identifier, Resource> original) {
        for (Map.Entry<Identifier, Resource> entry : original.entrySet()) {
            Identifier id = entry.getKey();
            if(!id.getPath().endsWith(".json")) {
                continue;
            }
            Resource resource = entry.getValue();
            JsonElement cachedResource = null;
            Iterator<Mixson.AssociatedMixsonEvent> events = Mixson.getEventIterators();
            while (events.hasNext()) {
                Mixson.AssociatedMixsonEvent event = events.next();
                if(!event.resourceId().equals(id)) continue;
                try {
                    if(cachedResource == null) cachedResource = JsonHelper.deserialize(new Gson(), entry.getValue().getReader(), JsonElement.class);
                    cachedResource = event.event().run(cachedResource);
                } catch (Exception e) {
                    throw new MixsonError(String.format("Failed to modify json file '%s' with event '%s'\n", id, event.eventId())+e);
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
