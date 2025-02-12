/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2018-2023 Yegor Bugayenko
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included
 * in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NON-INFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package io.zold.api;

import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.ArrayList;
import java.util.stream.Collectors;
import org.cactoos.iterable.IterableOf;
import org.cactoos.iterable.Joined;
import org.cactoos.iterable.Mapped;
import org.cactoos.iterable.Skipped;
import org.cactoos.list.ListOf;
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
     * @todo #65:30min Complete Wallet implementations with id, public RSA
     *  key and network id. Wallets.create(Long, String, String) must
     *  create a Wallet with these strings set and a constructor must be
     *  added to all Walletl realizations (Wallet(final long id, final String
     *  pubkey, final String network, final Transaction... transactions). After
     *  completing these implementations fix tests that uses Wallets.create()
     *  and all Wallet realizations.
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
         * Constructor.
         * @param id The wallet id.
         * @param pubkey The public RSA key of the wallet owner.
         * @param network The network the walet belongs to.
         * @checkstyle UnusedFormalParameter (2 lines)
         */
        public Fake(final long id, final String pubkey, final String network) {
            this(id);
        }

        /**
         * Ctor.
         * @param id The wallet id.
         * @param transactions Transactions.
         */
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
            try {
                List<String> lines = Files.readAllLines(this.path);
                return Long.parseUnsignedLong(lines.get(2), 16);
            } catch (Exception e) {
                throw new IOException(e);
            }
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
            long ourId = this.id();
            long otherId = other.id();
            if (ourId != otherId) {
                throw new IOException(
                    new FormattedText(
                        "Wallet ID mismatch, ours is %d, theirs is %d",
                        ourId,
                        otherId
                    ).asString()
                );
            }
            Iterable<Transaction> ledgerIterable = this.ledger();
            List<Transaction> ledger = new ArrayList<>();
            for (Transaction t : ledgerIterable) {
                ledger.add(t);
            }
            List<Transaction> candidates = new ArrayList<>();
            for (Transaction incoming : other.ledger()) {
                boolean found = false;
                for (Transaction origin : ledger) {
                    if (incoming.equals(origin)
                        || (incoming.id() == origin.id() && incoming.bnf().equals(origin.bnf()))
                        || (incoming.id() == origin.id() && incoming.amount() < 0L)
                        || incoming.prefix().equals(origin.prefix())) {
                        found = true;
                        break;
                    }
                }
                if (!found) {
                    candidates.add(incoming);
                }
            }
            List<Transaction> merged = new ArrayList<>(ledger);
            merged.addAll(candidates);
            return new Wallet.Fake(ourId, merged);
        }

        @Override
        public Iterable<Transaction> ledger() {
            try {
                List<String> lines = Files.readAllLines(this.path);
                return lines.stream()
                    .skip(5)
                    .map(RtTransaction::new)
                    .collect(Collectors.toList());
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        @Override
        public String key() {
            throw new UnsupportedOperationException(
                "key() not yet supported"
            );
        }
    }
}