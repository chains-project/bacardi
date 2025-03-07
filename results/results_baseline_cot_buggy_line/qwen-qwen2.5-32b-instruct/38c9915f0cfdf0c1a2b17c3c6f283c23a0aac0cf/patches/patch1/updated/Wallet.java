package io.zold.api;

import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.nio.file.Path;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;
import org.cactoos.text.FormattedText;
import org.cactoos.text.Splitter;
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
    // ... (rest of the interface remains unchanged)

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
                StreamSupport.stream(
                    new Splitter(
                        new TextOf(this.path),
                        "\n"
                    ).value().spliterator(), false
                ).skip(2).findFirst().orElseThrow(
                    () -> new IOException("Failed to parse wallet ID")
                ), 16
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
            final Iterable<Transaction> candidates = StreamSupport.stream(
                other.ledger().spliterator(), false
            ).filter(incoming -> StreamSupport.stream(
                ledger.spliterator(), false
            ).noneMatch(origin -> incoming.equals(origin) ||
                (incoming.id() == origin.id() && incoming.bnf().equals(origin.bnf())) ||
                (incoming.id() == origin.id() && incoming.amount() < 0L) ||
                incoming.prefix().equals(origin.prefix())
            )).collect(Collectors.toList());
            return new Wallet.Fake(
                this.id(),
                StreamSupport.stream(
                    new Joined<Transaction>(ledger, candidates).spliterator(), false
                ).collect(Collectors.toList())
            );
        }

        @Override
        public Iterable<Transaction> ledger() {
            return StreamSupport.stream(
                new Splitter(
                    new TextOf(this.path),
                    "\\n"
                ).value().spliterator(), false
            ).skip(5).map(txt -> new RtTransaction(txt)).collect(Collectors.toList());
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
}