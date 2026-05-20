package net.ramixin.mixson.enums;

import net.ramixin.mixson.Mixson;

/**
 * Debug features that can be enabled via {@link Mixson#enableDebugOption(DebugOption)}.
 */
public enum DebugOption {
    /**
     * Exports the resource to the .mixson folder after an event has been processed.
     */
    EXPORT_PATCHED_FILE,

    /**
     * Exports the resource to the .mixson folder before an event is processed.
     */
    EXPORT_UNPATCHED_FILE,

    /**
     * Logs when any of the following happen occurs:
     * <ul>
     * <li>An event starts processing a resource
     * <li>An event is registered
     * <li>A reference is registered
     * </ul>
     */
    BASIC_LOGGING,

    /**
     * Logs when any of the following happen occurs:
     * <ul>
     * <li>An event is given the opportunity to process files
     * <li>An event finishes processing all resources, if any, along with the time it took to do so.
     * <li>An event finishes processing a single resource, along with the time it took to do so.
     * </ul>
     */
    EXTRA_LOGGING

    ;

    public int getMask() {
        return 1 << ordinal();
    }
}
