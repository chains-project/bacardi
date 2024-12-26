package io.zold.api;

import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.nio.file.Path;
import org.cactoos.collection.Filtered;
import org.cactoos.collection.IterableEnvelope;
import org.cactoos.iterable.IterableOf;
import org.cactoos.iterable.Joined;
import org.cactoos.iterable.Mapped;
import org.cactoos.iterable.Skipped;
import org.cactoos.list.ListOf;
import org.cactoos.scalar.Checked;
import org.cactoos.scalar.Or;
import org.cactoos.scalar.Unchecked;
import org.cactoos.text.FormattedText;
import org.cactoos.text.TextOf;

/**
 * Wallet.
 * @since 0.1
 * @todo #16:30min Merge method should update transactions
 *  in wallet's file and return concrete implementation not a fake one.
 *  Beware that tests should be refactored to take care of file cleanup
 *  after each case that merges wallets.
 */
@SuppressWarnings({"PMD.ShortMethodName", "PMD.TooManyMethods",
    "PMD.UnusedFormalParameter"})
public interface Wallet {
    // ... (rest of the code remains unchanged)

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
            return new Checked<>(
                () -> Long.parseUnsignedLong(
                    new ListOf<>(
                        new TextOf(this.path).asString().split("\n")
                    )[2],
                    // @checkstyle MagicNumber (1 line)
                    16
                ),
                e -> new IOException(e)
            ).value();
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
                    new UncheckedText(
                        new FormattedText(
                            "Wallet ID mismatch, ours is %d, theirs is %d",
                            other.id(),
                            this.id()
                        )
                    ).asString()
                );
            }
            final Iterable<Transaction> ledger = this.ledger();
            final Iterable<Transaction> candidates = new Filtered<>(
                incoming -> new Filtered<>(
                    origin -> new Unchecked<>(
                        new Or(
                            () -> incoming.equals(origin),
                            () -> incoming.id() == origin.id()
                                && incoming.bnf().equals(origin.bnf()),
                            () -> incoming.id() == origin.id()
                                && incoming.amount() < 0L,
                            () -> incoming.prefix().equals(origin.prefix())
                        )
                    ).value(),
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

        @Override
        public String key() {
            throw new UnsupportedOperationException(
                "key() not yet supported"
            );
        }
    }
}