package net.ramixin.mixson.tests.context;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.Resource;
import net.ramixin.mixson.Mixson;
import net.ramixin.mixson.MixsonException;
import net.ramixin.mixson.enums.ErrorPolicy;
import net.ramixin.mixson.enums.Lifetime;
import net.ramixin.mixson.hooks.ListHook;
import net.ramixin.mixson.hooks.NamespaceHook;
import net.ramixin.mixson.hooks.StandardHook;
import net.ramixin.mixson.util.Index;
import org.apache.commons.lang3.mutable.Mutable;
import org.apache.commons.lang3.mutable.MutableInt;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static net.ramixin.mixson.tests.TestUtil.*;

public class CaptureTests {

    private static final Logger log = LoggerFactory.getLogger(CaptureTests.class);

    @Test
    public void testStandardCapturing() {
        Index index = new Index("test:standard_capturing");
        Identifier idWithExt = index.id().withSuffix(".json");

        Index otherIndex = new Index("test:standard_other_captured");
        Identifier otherIdWithExt = otherIndex.id().withSuffix(".json");
        JsonObject goalObject = generateRandomJsonObject();

        Mixson.registerEvent(
                Mixson.DEFAULT_PRIORITY,
                Lifetime.PERSISTENT,
                ErrorPolicy.THROW,
                "Standard - capturingTest",
                index::idEquals,
                context -> {
                    List<Mutable<JsonElement>> captures = context.captureFiles(otherIndex);
                    assert captures.size() == 1 : "Incorrect number of files captured. Expected 1, but got "+captures.size();
                    Mutable<JsonElement> capture = captures.getFirst();
                    log.info("captured file: {}", capture.get());
                    log.info("expected file: {}", goalObject);
                    assert capture.get().equals(goalObject) : "Captured file was not serialized correctly";

                    Throwable exception = Assertions.assertThrows(MixsonException.class, () -> context.captureFiles(otherIndex)).getCause();
                    log.error("e: ", exception);
                    assert exception.getMessage().contains("cannot capture same file twice") : "Caught wrong exception";
                }
        );
        Map<Identifier, Resource> resourceMap = new HashMap<>(){{
            put(idWithExt, createDummyResource(generateRandomJsonObject()));
            put(otherIdWithExt, createDummyResource(goalObject));
        }};
        Mixson.processHook(new StandardHook(resourceMap));
    }

    @Test
    public void testListCapturing() {
        Index index = new Index("test:list_capturing");
        Identifier idWithExt = index.id().withSuffix(".json");

        JsonObject goalObject = generateRandomJsonObject();
        MutableInt net = new MutableInt();
        List<Resource> resourceList = generateRandomResourceList(goalObject, net);
        Index otherIndex = new Index("test:list_captured", (int) net.get());
        Identifier otherIdWithExt = otherIndex.id().withSuffix(".json");


        Mixson.registerEvent(
                Mixson.DEFAULT_PRIORITY,
                Lifetime.PERSISTENT,
                ErrorPolicy.THROW,
                "List - capturingTest",
                index::idEquals,
                context -> {
                    List<Mutable<JsonElement>> captures = context.captureFiles(otherIndex);
                    assert captures.size() == 1 : "Incorrect number of files captured. Expected 1, but got "+captures.size();
                    Mutable<JsonElement> capture = captures.getFirst();
                    assert capture.get().equals(goalObject) : "Captured file was not serialized correctly";

                    Throwable exception = Assertions.assertThrows(MixsonException.class, () -> context.captureFiles(otherIndex)).getCause();
                    log.error("e: ", exception);
                    assert exception.getMessage().contains("cannot capture same file twice") : "Caught wrong exception";

                    Index newIndex = new Index(otherIndex.id(), randomListOrdinal(resourceList.size(), (int) net.get()));
                    context.captureFiles(newIndex);
                }
        );
        Map<Identifier, List<Resource>> resourceMap = new HashMap<>(){{
            put(idWithExt, generateRandomResourceList(new JsonObject(), new MutableInt()));
            put(otherIdWithExt, resourceList);
        }};
        Mixson.processHook(new ListHook(resourceMap));
    }

    @Test
    public void testNamespaceCapturing() {
        Index index = new Index("test:namespace_capturing");
        Identifier idWithExt = index.id().withSuffix(".json");

        JsonObject goalObject = generateRandomJsonObject();
        MutableInt net = new MutableInt();
        List<Resource> resourceList = generateRandomResourceList(goalObject, net);
        Index otherIndex = new Index("test:namespace_capturing", (int) net.get());

        Mixson.registerEvent(
                Mixson.DEFAULT_PRIORITY,
                Lifetime.PERSISTENT,
                ErrorPolicy.THROW,
                "Namespace - capturingTest",
                index::idEquals,
                context -> {
                    List<Mutable<JsonElement>> captures = context.captureFiles(otherIndex);
                    assert captures.size() == 1 : "Incorrect number of files captured. Expected 1, but got "+captures.size();
                    Mutable<JsonElement> capture = captures.getFirst();
                    assert capture.get().equals(goalObject) : "Captured file was not serialized correctly";

                    Throwable exception = Assertions.assertThrows(MixsonException.class, () -> context.captureFiles(otherIndex)).getCause();
                    assert exception.getMessage().contains("cannot capture same file twice") : "Caught wrong exception: "+ exception;

                    Index newIndex = new Index(otherIndex.id(), randomListOrdinal(resourceList.size(), (int) net.get()));
                    context.captureFiles(newIndex);
                }
        );
        Mixson.processHook(new NamespaceHook(resourceList, idWithExt));
    }

}
