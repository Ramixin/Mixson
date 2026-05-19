package net.ramixin.mixson.enums;

import net.ramixin.mixson.util.functions.Event;
import net.ramixin.mixson.util.interfaces.MixsonCodec;

/**
 * Defines how an {@link Event} should be handled if it throws an exception during its execution time. The policy
 * controls internal processes, such as resource capturing and {@link MixsonCodec} usages.
 */
public enum ErrorPolicy {
    /**
     * The error is ignored, and program execution will continue.
     * There will be no trace that the event threw an exception.
     */
    IGNORE,

    /**
     * The error will be logged, and program execution will continue.
     */
    LOG,

    /**
     * The error will be rethrown.
     */
    THROW
}
