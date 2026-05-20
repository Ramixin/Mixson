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

public class DeletionTests {

    @Test
    public void testResourceDeletion() {
        Index index = new Index("test:standard_deletion");
        Identifier idWithExt = index.id().withSuffix(".json");

        Identifier otherIdWithExt = Identifier.parse("test:standard_other_deletion.json");

        Mixson.registerEvent(
                Mixson.DEFAULT_PRIORITY,
                Lifetime.PERSISTENT,
                ErrorPolicy.THROW,
                "Standard - fileDeletionTest",
                index::idEquals,
                context -> context.markForDeletion(true)
        );

        Map<Identifier, Resource> resourceMap = new HashMap<>(){{
            put(idWithExt, createDummyResource(generateRandomJsonObject()));
            put(otherIdWithExt, createDummyResource(generateRandomJsonObject()));
        }};
        Mixson.processHook(new StandardHook(resourceMap));

        assert !resourceMap.containsKey(idWithExt) : "Resource was not deleted";
        assert resourceMap.containsKey(otherIdWithExt) : "Too many resources were deleted";
    }

    @Test
    public void testListDeletion() throws IOException {
        JsonObject object = generateRandomJsonObject();
        MutableInt net = new MutableInt();
        List<Resource> resourceList = generateRandomResourceList(object, net);
        int originalSize = resourceList.size();

        Index index = new Index("test:list_deletion", (int) net.get());
        Identifier idExt = index.id().withSuffix(".json");

        Mixson.registerEvent(
                Mixson.DEFAULT_PRIORITY,
                Lifetime.PERSISTENT,
                ErrorPolicy.THROW,
                "List - fileCreationTest",
                index::equals,
                context -> {
                    assert context.getFile().equals(object) : "Event ran for wrong resource";
                    context.markForDeletion(true);
                }
        );
        Map<Identifier, List<Resource>> resourceMap = new HashMap<>(){{
            put(idExt, resourceList);
        }};
        Mixson.processHook(new ListHook(resourceMap));
        assert resourceMap.containsKey(idExt) : "Resource list missing";
        List<Resource> resultList = resourceMap.get(idExt);
        assert resultList.size() == originalSize-1 : String.format("Wrong number of resources found. Expected %s but got %s", originalSize-1, resultList.size());
        for (int i = 0; i < resultList.size(); i++) {
            Resource resource = resultList.get(i);
            JsonElement element = MixsonCodecs.JSON_ELEMENT.deserialize(resource);
            assert !object.equals(element) : String.format("Wrong index was deleted. %s instead of %s", i, net.get());
        }
    }

    @Test
    public void testNamespaceDeletion() throws IOException {
        JsonObject object = generateRandomJsonObject();
        MutableInt net = new MutableInt();
        List<Resource> resourceList = generateRandomResourceList(object, net);
        int originalSize = resourceList.size();

        Index index = new Index("test:namespace_deletion", (int) net.get());
        Identifier idExt = index.id().withSuffix(".json");

        Mixson.registerEvent(
                Mixson.DEFAULT_PRIORITY,
                Lifetime.PERSISTENT,
                ErrorPolicy.THROW,
                "Namespace - fileCreationTest",
                index::equals,
                context -> {
                    assert context.getFile().equals(object) : "Event ran for wrong resource";
                    context.markForDeletion(true);
                }
        );
        Mixson.processHook(new NamespaceHook(resourceList, idExt));
        assert resourceList.size() == originalSize-1 : String.format("Wrong number of resources found. Expected %s but got %s", originalSize-1, resourceList.size());
        for (int i = 0; i < resourceList.size(); i++) {
            Resource resource = resourceList.get(i);
            JsonElement element = MixsonCodecs.JSON_ELEMENT.deserialize(resource);
            assert !object.equals(element) : String.format("Wrong index was deleted. %s instead of %s", i, net.get());
        }
    }


}
