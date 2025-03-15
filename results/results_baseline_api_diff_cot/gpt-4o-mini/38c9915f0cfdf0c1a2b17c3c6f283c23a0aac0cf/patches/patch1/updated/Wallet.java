package io.zold.api;

import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.nio.file.Path;
import org.cactoos.collection.Filtered;
import org.cactoos.iterable.IterableOf;
import org.cactoos.iterable.Joined;
import org.cactoos.iterable.Mapped;
import org.cactoos.iterable.Skipped;
import org.cactoos.list.ListOf;
import org.cactoos.scalar.Or;
import org.cactoos.text.FormattedText;
import org.cactoos.text.TextOf;

@SuppressWarnings({"PMD.ShortMethodName", "PMD.TooManyMethods",
    "PMD.UnusedFormalParameter"})
public interface Wallet {
    long id() throws IOException;

    void pay(long amt, long bnf) throws IOException;

    Wallet merge(Wallet other) throws IOException;

    Iterable<Transaction> ledger();

    String key();

    final class Fake implements Wallet {
        private final long id;
        private final Iterable<Transaction> transactions;

        public Fake(final long id) {
            this(id, new IterableOf<>());
        }

        public Fake(final long id, final Transaction... transactions) {
            this(id, new IterableOf<>(transactions));
        }

        public Fake(final long id, final String pubkey, final String network) {
            this(id);
        }

        public Fake(final long id, final Iterable<Transaction> transactions) {
            this.id = id;
            this.transactions = transactions;
        }

        @Override
        public long id() throws IOException {
            return this.id;
        }

        @Override
        public void pay(final long amt, final long bnf) {
            // nothing
        }

        @Override
        public Wallet merge(final Wallet other) {
            return other;
        }

        @Override
        public Iterable<Transaction> ledger() {
            return this.transactions;
        }

        @Override
        public String key() {
            return Long.toString(this.id);
        }
    }

    final class File implements Wallet {
        private final Path path;

        File(final Path path) {
            this.path = path;
        }

        @Override
        public long id() throws IOException {
            return Long.parseUnsignedLong(
                new ListOf<>(
                    new TextOf(this.path).asString().split("\n")
                )[2],
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