package io.zold.api;

import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.nio.file.Path;
import org.cactoos.Func;
import org.cactoos.collection.Filtered;
import org.cactoos.collection.Mapped;
import org.cactoos.collection.Skipped;
import org.cactoos.collection.ListOf;
import org.cactoos.iterable.IterableOf;
import org.cactoos.iterable.Joined;
import org.cactoos.iterable.FilteredIterable;
import org.cactoos.iterable.MappedIterable;
import org.cactoos.iterable.SkippedIterable;
import org.cactoos.text.FormattedText;
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
@SuppressWarnings({"PMD.ShortMethodName", "PMD.TooManyMethods",
    "PMD.UnusedFormalParameter"})
public interface Wallet {
    /**
     * This wallet's ID: an unsigned 64-bit integer.
     * @return This wallet's id
     * @throws IOException If an IO error occurs
     * @checkstyle ClassDataAbstractionCouplingCheck (500 lines)
     * @checkstyle MethodName (2 lines)
     */
    long id() throws IOException;

    /**
     * Make a payment.
     * @param amt Amount to pay in zents
     * @param bnf Wallet ID of beneficiary
     * @throws IOException If an IO error occurs
     */
    void pay(long amt, long bnf) throws IOException;

    /**
     * Merge both {@code this} and {@code other}. Fails if they are not the
     * same wallet, as identified by their {@link #id() id}.
     * @param other Other wallet
     * @return The merged wallet
     * @throws IOException If an IO error occurs
     */
    Wallet merge(Wallet other) throws IOException;

    /**
     * This wallet's ledger.
     * @return This wallet's ledger
     */
    Iterable<Transaction> ledger();

    /**
     * This wallet's RSA key.
     * @return This wallet's RSA key.
     */
    String key();

    /**
     * A Fake {@link Wallet}.
     * @since 1.0
     * @todo #65:30min Implement key method. This should return the
     *  public RSA key of the wallet owner in Base64. Also add a unit test
     *  to replace WalletTest.keyIsNotYetImplemented().
     */
    final class Fake implements Wallet {

        /**
         * The wallet id.
         */
        private final long id;

        /**
         * Transactions.
         */
        private final Iterable<Transaction> transactions;

        /**
         * Constructor.
         * @param id The wallet id.
         */
        public Fake(final long id) {
            this(id, new IterableOf<>());
        }

        /**
         * Ctor.
         * @param id The wallet id.
         * @param transactions Transactions.
         */
        public Fake(final long id, final Transaction... transactions) {
            this(id, new IterableOf<>(transactions));
        }

        /**
         * Ctor.
         * @param id The wallet id.
         * @param pubkey The public RSA key of the wallet owner.
         * @param network The network the walet belongs to.
         * @checkstyle UnusedFormalParameter (1 line)
         */
        public Fake(final long id, final String pubkey, final String network) {
            this(id);
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
            final Iterable<Transaction> candidates = new FilteredIterable<>(
                incoming -> new FilteredIterable<>(
                    origin -> new Func<Boolean>() {
                        @Override
                        public Boolean apply(final Object input) {
                            final Transaction t = (Transaction) input;
                            return incoming.equals(t)
                                || (incoming.id() == t.id() && incoming.bnf().equals(t.bnf()))
                                || (incoming.id() == t.id() && incoming.amount() < 0L)
                                || incoming.prefix().equals(t.prefix());
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
            return new MappedIterable<>(
                txt -> new RtTransaction(txt.asString()),
                new SkippedIterable<>(
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
}