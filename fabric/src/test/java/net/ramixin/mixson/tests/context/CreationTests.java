package net.ramixin.mixson.tests.context;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.Resource;
import net.ramixin.mixson.Mixson;
import net.ramixin.mixson.MixsonCodecs;
import net.ramixin.mixson.enums.ErrorPolicy;
import net.ramixin.mixson.enums.Lifetime;
import net.ramixin.mixson.hooks.ListHook;
import net.ramixin.mixson.hooks.NamespaceHook;
import net.ramixin.mixson.hooks.StandardHook;
import net.ramixin.mixson.util.Index;
import org.apache.commons.lang3.mutable.MutableInt;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static net.ramixin.mixson.tests.TestUtil.*;

public class CreationTests {


    @Test
    public void testStandardCreation() throws IOException {
        Index index = new Index("test:standard_creation");
        Identifier idWithExt = index.id().withSuffix(".json");
        JsonObject goalObject = generateRandomJsonObject();

        Index goalIndex = new Index("test:standard_goal");
        Identifier goalIdWithExt = goalIndex.id().withSuffix(".json");

        Mixson.registerEvent(
                Mixson.DEFAULT_PRIORITY,
                Lifetime.PERSISTENT,
                ErrorPolicy.THROW,
                "Standard - fileCreationTest",
                index::idEquals,
                context -> context.createResource(goalIndex, goalObject)
        );
        Map<Identifier, Resource> resourceMap = new HashMap<>(){{
            put(idWithExt, createDummyResource(new JsonObject()));
        }};
        Mixson.processHook(new StandardHook(resourceMap));
        assert resourceMap.containsKey(goalIdWithExt) : "Resource was not created";
        Resource resource = resourceMap.get(goalIdWithExt);
        assert MixsonCodecs.JSON_ELEMENT.deserialize(resource).equals(goalObject) : "Resource was not created correctly";
    }

    @Test
    public void testListCreation() throws IOException {
        JsonObject object = generateRandomJsonObject();
        MutableInt net = new MutableInt();
        List<Resource> resourceList = generateRandomResourceList(object, net);
        int originalSize = resourceList.size();

        Index index = new Index("test:list_creation", (int) net.get());
        Identifier idWithExt = index.id().withSuffix(".json");

        Index IndexA = new Index("test:list_goal");
        Identifier IdAExt = IndexA.id().withSuffix(".json");
        JsonObject objectA = generateRandomJsonObject();

        int randomOrdinal = randomListOrdinal(resourceList.size(), (int) net.get());
        Index IndexB = new Index("test:list_creation", randomOrdinal);
        Identifier IdBExt = IndexB.id().withSuffix(".json");
        JsonObject objectB = generateRandomJsonObject();

        Mixson.registerEvent(
                Mixson.DEFAULT_PRIORITY,
                Lifetime.PERSISTENT,
                ErrorPolicy.THROW,
                "List - fileCreationTest",
                index::equals,
                context -> {
                    assert context.getFile().equals(object) : "Event ran for wrong resource";
                    context.createResource(IndexA, objectA);
                    context.createResource(IndexB, objectB);
                }
        );
        Map<Identifier, List<Resource>> resourceMap = new HashMap<>(){{
            put(idWithExt, resourceList);
        }};
        Mixson.processHook(new ListHook(resourceMap));

        assert resourceMap.containsKey(IdAExt) : "Resource list missing (A)";
        List<Resource> resourceListA = resourceMap.get(IdAExt);
        assert resourceListA.size() == 1 : "Wrong number of resources (A)";
        JsonElement element = MixsonCodecs.JSON_ELEMENT.deserialize(resourceListA.getFirst());
        assert element.equals(objectA) : "Deserialized resource does not match (A)";

        assert resourceMap.containsKey(IdBExt) : "Resource list missing (B)";
        List<Resource> resourceListB = resourceMap.get(IdBExt);
        assert resourceListB.size() == originalSize + 1 : "Wrong number of resources (B)";
        element = MixsonCodecs.JSON_ELEMENT.deserialize(resourceListB.get(randomOrdinal));
        assert element.equals(objectB) : "Deserialized resource does not match (B)";
    }

    @Test
    public void testNamespaceCreation() throws IOException {
        JsonObject object = generateRandomJsonObject();
        MutableInt net = new MutableInt();
        List<Resource> resourceList = generateRandomResourceList(object, net);
        int originalSize = resourceList.size();

        Index index = new Index("test:namespace_creation", (int) net.get());
        Identifier idWithExt = index.id().withSuffix(".json");

        int goalOrdinal = randomListOrdinal(resourceList.size(), (int) net.get());
        Index goalIndex = new Index(index.id(), goalOrdinal);
        JsonObject goalObject = generateRandomJsonObject();

        Mixson.registerEvent(
                Mixson.DEFAULT_PRIORITY,
                Lifetime.PERSISTENT,
                ErrorPolicy.THROW,
                "Namespace - fileCreationTest",
                index::equals,
                context -> {
                    assert context.getFile().equals(object) : "Event ran for wrong resource";
                    context.createResource(goalIndex, goalObject);
                }
        );
        Mixson.processHook(new NamespaceHook(resourceList, idWithExt));


        assert resourceList.size() == originalSize + 1 : "Wrong number of resources";
        JsonElement element = MixsonCodecs.JSON_ELEMENT.deserialize(resourceList.get(goalOrdinal));
        assert element.equals(goalObject) : "Deserialized resource does not match";
    }

}
