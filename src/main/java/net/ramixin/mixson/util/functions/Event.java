package net.ramixin.mixson.util.functions;

import net.ramixin.mixson.EventContext;
import net.ramixin.mixson.Mixson;

/**
 * Intended to be used as a lambda for event handlers when registering events
 * in the {@link Mixson} class.
 *
 * @param <T> The type resource the event will be applied to.
 */
@FunctionalInterface
public interface Event<T> {

    /**
     * The event itself.
     *
     * @param context The context of the given run of the event. This should not be stored or kept in any way after
     *                the event is run. Doing so may cause unexpected behavior.
     *
     */
    void runEvent(EventContext<T> context);
}
