package io.zold.api;

import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.nio.file.Path;
import org.cactoos.iterable.Filtered;
import org.cactoos.iterable.IterableOf;
import org.cactoos.iterable.Joined;
import org.cactoos.iterable.Mapped;
import org.cactoos.iterable.Skipped;
import org.cactoos.list.ListOf;
import org.cactoos.scalar.Or;
import org.cactoos.text.FormattedText;
import org.cactoos.text.TextOf;

/**
 * Default File implementation.
 * @checkstyle ClassDataAbstractionCouplingCheck (2 lines)
 */
final class File implements Wallet {

    /**
     * Path of this wallet.
     */
    private final Path path;

    /**
     * Ctor.
     * @param path Path of wallet
     */
    File(final Path path) {
        this.path = path;
    }

    @Override
    public long id() throws IOException {
        return Long.parseUnsignedLong(
            new ListOf<>(
                new TextOf(this.path).asString().split("\n")
            ).get(2),
            // @checkstyle MagicNumber (1 line)
            16
        );
    }

    @Override
    public void pay(final long amt, final long bnf) throws IOException {
        try (final Writer out = new FileWriter(this.path.toFile(), true)) {
            out.write('\n');
            out.write(new CpTransaction(amt, bnf).toString());
        }
    }

    @Override
    public Wallet merge(final Wallet other) throws IOException {
        if (other.id() != this.id()) {
            throw new IOException(
                new FormattedText(
                    "Wallet ID mismatch, ours is %d, theirs is %d",
                    other.id(),
                    this.id()
                ).asString()
            );
        }
        final Iterable<Transaction> ledger = this.ledger();
        final Iterable<Transaction> candidates = new Filtered<>(
            incoming -> new Filtered<>(
                origin -> 
                    incoming.equals(origin) ||
                    (incoming.id() == origin.id() && incoming.bnf().equals(origin.bnf())) ||
                    (incoming.id() == origin.id() && incoming.amount() < 0L) ||
                    incoming.prefix().equals(origin.prefix()),
                ledger
            ).isEmpty(),
            other.ledger()
        );
        return new Wallet.Fake(
            this.id(),
            new Joined<Transaction>(ledger, candidates)
        );
    }

    @Override
    public Iterable<Transaction> ledger() {
        return new Mapped<>(
            txt -> new RtTransaction(txt.asString()),
            new Skipped<>(
                new ListOf<>(
                    new TextOf(this.path).asString().split("\\n")
                ),
                // @checkstyle MagicNumberCheck (1 line)
                5
            )
        );
    }

    // @todo #54:30min Implement key method. This should return the
    //  public RSA key of the wallet owner in Base64. Also add a unit test
    //  to replace WalletTest.keyIsNotYetImplemented().
    @Override
    public String key() {
        throw new UnsupportedOperationException(
            "key() not yet supported"
        );
    }
}