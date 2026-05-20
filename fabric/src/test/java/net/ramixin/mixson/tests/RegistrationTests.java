package net.ramixin.mixson.tests;

import com.google.gson.JsonElement;
import net.ramixin.mixson.Mixson;
import net.ramixin.mixson.ResourceReference;
import net.ramixin.mixson.enums.ErrorPolicy;
import net.ramixin.mixson.enums.Lifetime;
import net.ramixin.mixson.util.Index;
import org.junit.jupiter.api.Test;

import java.util.UUID;

public class RegistrationTests {

    @Test
    public void testEventRegistration() {
        UUID eventId = Mixson.registerEvent(
                Mixson.DEFAULT_PRIORITY,
                Lifetime.PERSISTENT,
                ErrorPolicy.THROW,
                "testEventRegistration",
                (_) -> false,
                _ -> {}
        );
        assert Mixson.hasEvent(eventId) : "Event was not registered";
    }

    @Test
    public void testReferenceRegistration() {
        ResourceReference<JsonElement> ref = Mixson.registerReference(
                Mixson.DEFAULT_PRIORITY,
                new Index("test:test"),
                "testReferenceRegistration"
        );
        assert Mixson.hasReference(ref.getUuid()) : "Reference was not registered";
    }



}
