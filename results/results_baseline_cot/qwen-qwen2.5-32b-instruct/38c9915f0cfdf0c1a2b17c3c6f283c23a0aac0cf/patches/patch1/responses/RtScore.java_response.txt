package io.zold.api;

import org.cactoos.Text;
import org.cactoos.iterable.IterableOf;

import java.util.stream.Collectors;

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
        return (int) other.suffixes().stream().count()
            - (int) this.sfxs.stream().count();
    }

    @Override
    public Iterable<Text> suffixes() {
        return this.sfxs;
    }
}