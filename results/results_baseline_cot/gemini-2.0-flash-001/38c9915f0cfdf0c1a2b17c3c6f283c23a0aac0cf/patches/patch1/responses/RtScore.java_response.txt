package io.zold.api;

import org.cactoos.Text;
import org.cactoos.scalar.LengthOf;
import org.cactoos.scalar.Unchecked;

/**
 * Default implementation for {@link Score}.
 *
 * @since 1.0
 */
public final class RtScore implements Score {

    /**
     * The suffixes.
     */
    private final Iterable<Text> sfxs;

    /**
     * Ctor.
     *
     * @param sfxs The suffixes.
     */
    RtScore(final Iterable<Text> sfxs) {
        this.sfxs = sfxs;
    }

    @Override
    public int compareTo(final Score other) {
        final LengthOf left = new LengthOf(other.suffixes());
        final LengthOf right = new LengthOf(this.sfxs);
        return new Unchecked<>(left).value().intValue()
            - new Unchecked<>(right).value().intValue();
    }

    @Override
    public Iterable<Text> suffixes() {
        return this.sfxs;
    }
}