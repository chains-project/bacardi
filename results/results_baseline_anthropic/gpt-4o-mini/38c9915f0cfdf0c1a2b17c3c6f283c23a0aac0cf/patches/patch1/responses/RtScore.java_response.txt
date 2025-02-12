package io.zold.api;

import org.cactoos.Text;
import java.util.Iterator;

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
        return sizeOf(other.suffixes()) - sizeOf(this.sfxs);
    }

    @Override
    public Iterable<Text> suffixes() {
        return this.sfxs;
    }

    private int sizeOf(Iterable<Text> iterable) {
        int size = 0;
        for (Text text : iterable) {
            size++;
        }
        return size;
    }
}