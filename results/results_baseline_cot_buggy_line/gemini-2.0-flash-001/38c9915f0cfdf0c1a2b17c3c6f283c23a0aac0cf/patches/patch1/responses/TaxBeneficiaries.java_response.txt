package io.zold.api;

import java.util.Comparator;
import org.cactoos.iterable.Filtered;
import org.cactoos.iterable.IterableEnvelope;
import org.cactoos.text.TextOf;
import org.cactoos.scalar.LengthOf;
import org.cactoos.scalar.Unchecked;

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
                // @checkstyle MagicNumberCheck (1 line)
                n -> new Unchecked<>(new LengthOf(new TextOf(n.score().suffixes().toString()))).value() >= 16,
                nodes
            )
        ));
    }
}