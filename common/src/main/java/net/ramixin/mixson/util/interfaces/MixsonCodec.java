package net.ramixin.mixson.util.interfaces;

import net.minecraft.server.packs.resources.Resource;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

/**
 * Describes how Mixson should read, write, and export resources of type {@link T}.
 * <p>
 * A codec provides the file extension for matching resources, converts raw
 * {@link Resource} instances into the decoded type, serializes modified values
 * back into resources, and optionally exports a resource to a byte stream for debugging
 * or file output.
 *
 * @param <T> the type of resource applicable to this codec
 */
public interface MixsonCodec<T> {

    /**
     * Returns the file extension preceded by a dot.
     *
     * @return the extension and dot
     */
    String extensionAndDot();

    /**
     * Deserializes the given resource into the codec's target type.
     *
     * @param resource the resource to decode
     * @return the decoded value
     * @throws IOException this method permits IOException to be thrown by the underlying implementation
     */
    T deserialize(Resource resource) throws IOException;

    /**
     * Serializes the given value back into a {@link Resource}.
     *
     * @param associatedResource the original resource associated with the value
     * @param elem the value to serialize
     * @return a new resource containing the serialized data
     * @throws IOException this method permits IOException to be thrown by the underlying implementation
     */
    Resource serialize(Resource associatedResource, T elem) throws IOException;

    /**
     * Builds a {@link ByteArrayOutputStream} based off of the given resource. This is used when exporting the
     * resource as a file during debugging.
     *
     * @param resource the resource to export
     * @return a stream based off of the resource
     * @throws IOException this method permits IOException to be thrown by the underlying implementation
     */
    ByteArrayOutputStream export(T resource) throws IOException;
}
