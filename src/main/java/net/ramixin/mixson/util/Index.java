package net.ramixin.mixson.util;

import net.minecraft.resources.Identifier;

import java.util.Objects;

/**
 * A single class representation of both an {@link Identifier} and an ordinal. When not specified,
 * ordinals default to {@code 0}. Ordinals of {@code -1} will match all resources with the same ID.
 */
@SuppressWarnings("ClassCanBeRecord") // NO IT CANNOT
public final class Index implements Comparable<Index> {

    private final Identifier id;
    private final int ordinal;

    /**
     * @param id The identifier of the resource.
     * @param ordinal The ordinal of the resource. Will throw an {@link IllegalArgumentException} if less than -1.
     */
    public Index(Identifier id, int ordinal) {
        this.id = id;
        if(ordinal < -1) throw new IllegalArgumentException("Ordinal must be greater than or equal to -1");
        this.ordinal = ordinal;
    }

    /**
     * @param stringId The identifier of the resource as a string. This will be parsed into an {@link Identifier}.
     * @param ordinal The ordinal of the resource. Will throw an {@link IllegalArgumentException} if less than -1.
     */
    public Index(String stringId, int ordinal) {
        this(Identifier.parse(stringId), ordinal);
    }

    /**
     * @param id The identifier of the resource.
     * The ordinal will default to {@code 0}.
     */
    public Index(Identifier id) {
        this(id, 0);
    }

    /**
     * @param stringId The identifier of the resource as a string. This will be parsed into an {@link Identifier}.
     * The ordinal will default to {@code 0}.
     */
    public Index(String stringId) {
        this(Identifier.parse(stringId), 0);
    }

    /**
     * @param suffix The suffix to append to the ID.
     * @return A new {@link Index} instance with the suffix appended to the ID.
     */
    public Index withSuffixedId(String suffix) {
        return new Index(id.withSuffix(suffix), ordinal);
    }

    @Override
    public String toString() {
        return "Index{namespace=" +
                id.getNamespace() +
                ", path=" +
                id.getPath() +
                ", ordinal=" +
                ordinal +
                '}';
    }

    @Override
    public int compareTo(Index other) {
        int idCompare = this.id.compareTo(other.id);
        if (idCompare != 0)
            return idCompare;
        return Integer.compare(this.ordinal, other.ordinal);
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof Index index)) return false;
        return ordinal == index.ordinal && Objects.equals(id, index.id);
    }

    public boolean idEquals(Index other) {
        return this.id.equals(other.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, ordinal);
    }

    /**
     * @return creates a copy by value of the {@link Index}.
     */
    public Index copy() {
        return new Index(Identifier.parse(id.toString()), ordinal);
    }

    public Identifier id() {
        return id;
    }

    public int ordinal() {
        return ordinal;
    }
}
