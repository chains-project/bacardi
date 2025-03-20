package io.zold.api;

import org.cactoos.Text;
import org.cactoos.scalar.LengthOf;
import org.cactoos.Scalar;

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
        final Scalar<Integer> otherLength = new LengthOf(other.suffixes());
        final Scalar<Integer> thisLength = new LengthOf(this.sfxs);
        return otherLength.value() - thisLength.value();
    }

    @Override
    public Iterable<Text> suffixes() {
        return this.sfxs;
    }
}