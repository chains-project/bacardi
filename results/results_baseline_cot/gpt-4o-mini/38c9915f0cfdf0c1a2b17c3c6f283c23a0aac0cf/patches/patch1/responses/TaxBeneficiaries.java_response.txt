package io.zold.api;

import java.util.Comparator;
import org.cactoos.iterable.Filtered;
import org.cactoos.iterable.IterableEnvelope;
import org.cactoos.iterable.LengthOf;
import org.cactoos.iterable.Sorted;
import org.cactoos.iterable.MapOf; // Added import for MapOf

/**
 * {@link Remote} nodes that should receive taxes.
 *
 * @since 1.0
 */
public final class TaxBeneficiaries extends IterableEnvelope<Remote> {

    /**
     * Ctor.
     *
     * @param nodes Remote nodes to select from.
     */
    public TaxBeneficiaries(final Iterable<Remote> nodes) {
        super(() -> new Sorted<>(
            Comparator.comparing(Remote::score),
            new Filtered<>(
                n -> new MapOf<>(n.score().suffixes()).size() >= 16, // Changed LengthOf to MapOf
                nodes
            )
        ));
    }
}