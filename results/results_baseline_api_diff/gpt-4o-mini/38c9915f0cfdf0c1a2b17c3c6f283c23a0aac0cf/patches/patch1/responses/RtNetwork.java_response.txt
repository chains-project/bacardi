package io.zold.api;

import java.io.IOException;
import java.util.Iterator;
import org.cactoos.iterable.Mapped;
import org.cactoos.iterable.Sorted;
import org.cactoos.scalar.Reduced;
import org.cactoos.scalar.IoChecked;

public final class RtNetwork implements Network {

    private final Iterable<Remote> nodes;

    RtNetwork(final Iterable<Remote> remotes) {
        this.nodes =  remotes;
    }

    @Override
    public void push(final Wallet wallet) {
        this.nodes.forEach(
            remote -> remote.push(wallet)
        );
    }

    @Override
    public Wallet pull(final long id) throws IOException {
        return new IoChecked<>(
            new Reduced<>(
                Wallet::merge,
                new Mapped<>(
                    c -> c::wallet,
                    new Sorted<>(new Copies(id, this))
                )
            )
        ).value();
    }

    @Override
    public Iterator<Remote> iterator() {
        return this.nodes.iterator();
    }
}