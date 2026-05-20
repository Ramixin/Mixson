package net.ramixin.mixson;

import org.jetbrains.annotations.ApiStatus;

@ApiStatus.Internal
public class MixsonException extends RuntimeException {

    public MixsonException(String message, Exception e) {
        super(message, e);
    }

    public MixsonException(String message, Object... args) {
        super(String.format(message, args));
    }
}
