package net.ramixin.mixson.enums;

import net.ramixin.mixson.util.functions.Event;
import net.ramixin.mixson.EventContext;

import java.util.UUID;

/**
 * Defines how an {@link Event} should be handled during registration and after invocation.
 */
public enum Lifetime {
    /**
     * The event can be processed immediately.
     * The event stays valid after the event is run.
     */
    PERSISTENT,

    /**
     * The event can be processed immediately.
     * The event is unregistered after the event is run once.
     */
    ONCE,

    /**
     * The event cannot run until it is activated via {@link EventContext#pullIntoRuntime(UUID)}.
     * The event is unregistered upon being activated.
     */
    DEFERRED
}
