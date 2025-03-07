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
import org.cactoos.text.FormattedText;
import org.cactoos.text.SplitText;
import org.cactoos.text.TextOf;
import org.cactoos.text.UncheckedText;

/**
 * Wallet.
 * @since 0.1
 * @todo #16:30min Merge method should update transactions
 *  in wallet's file and return concrete implementation not a fake one.
 *  Beware that tests should be refactored to take care of file cleanup
 *  after each case that merges wallets.
 */
@SuppressWarnings({"PMD.ShortMethodName", "PMD.TooManyMethods", "PMD.UnusedFormalParameter"})
public interface Wallet {
    // ... (rest of the interface remains unchanged)

    final class File implements Wallet {

        // ... (rest of the class remains unchanged)

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
                    origin -> {
                        try {
                            return incoming.equals(origin)
                                || (incoming.id() == origin.id()
                                && incoming.bnf().equals(origin.bnf()))
                                || (incoming.id() == origin.id()
                                && incoming.amount() < 0L)
                                || incoming.prefix().equals(origin.prefix());
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                    },
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
                        new SplitText(
                            new TextOf(this.path),
                            "\\n"
                        )
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
}