/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2018-2023 Yegor Bugayenko
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included
 * in all copies or substantial portions of the Software.
 */
package io.zold.api;

import java.util.Comparator;
import org.cactoos.iterable.Filtered;
import org.cactoos.iterable.IterableEnvelope;
import org.cactoos.scalar.LengthOf;
import org.cactoos.iterable.Sorted;

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
                n -> new LengthOf(n.score().suffixes()).value().intValue() >= 16,
                nodes
            )
        ));
    }
}
