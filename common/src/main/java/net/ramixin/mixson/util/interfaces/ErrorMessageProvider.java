package net.ramixin.mixson.util.interfaces;

import net.minecraft.resources.Identifier;
import net.ramixin.mixson.enums.ErrorPolicy;
import org.jetbrains.annotations.ApiStatus;

@ApiStatus.Internal
public interface ErrorMessageProvider {

    String getRuntimeErrorMessage(Identifier resourceId);

    ErrorPolicy getErrorPolicy();

    String getRegistrationMessage(int priority);
}
