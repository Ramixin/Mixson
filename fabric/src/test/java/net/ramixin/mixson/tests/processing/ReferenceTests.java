package net.ramixin.mixson.tests.processing;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.Resource;
import net.ramixin.mixson.Mixson;
import net.ramixin.mixson.ResourceReference;
import net.ramixin.mixson.hooks.StandardHook;
import net.ramixin.mixson.util.Index;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static net.ramixin.mixson.tests.TestUtil.createDummyResource;
import static net.ramixin.mixson.tests.TestUtil.generateRandomJsonObject;

public class ReferenceTests {

    @Test
    public void testReferenceProcessing() {
        Index index = new Index("test:reference_run");
        Identifier indexExt = index.id().withSuffix(".json");

        JsonObject value = generateRandomJsonObject();

        ResourceReference<JsonElement> reference = Mixson.registerReference(
                Mixson.DEFAULT_PRIORITY,
                index,
                "testReferenceRun"
        );

        Map<Identifier, Resource> resourceMap = new HashMap<>(){{
            put(indexExt, createDummyResource(value));
        }};
        Mixson.processHook(new StandardHook(resourceMap));

        assert reference.retrieve().isPresent() : "Reference was not run";
        JsonObject refObject = reference.consume().orElseThrow().getAsJsonObject();
        assert refObject.equals(value) : "Reference was not serialized correctly";

    }

}
