/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2018-2023 Yegor Bugayenko
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies, and to permit persons to whom the Software is furnished to do so,
 * subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included
 * in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NON-INFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package io.zold.api;

import java.io.IOException;
import java.math.BigInteger;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;
import org.cactoos.Text;
import org.cactoos.list.ListOf;
import org.cactoos.scalar.ItemAt;
import org.cactoos.text.FormattedText;
import org.cactoos.text.TextOf;
import org.cactoos.time.ZonedDateTimeOf;

/**
 * RtTransaction.
 *
 * @since 0.1
 * @checkstyle ClassDataAbstractionCoupling (3 lines)
 */
@SuppressWarnings({"PMD.AvoidCatchingGenericException",
    "PMD.AvoidFieldNameMatchingMethodName"})
final class RtTransaction implements Transaction {

    /**
     * Pattern for Prefix String.
     */
    private static final Pattern PREFIX = Pattern.compile(
        //@checkstyle LineLengthCheck (1 line)
        "^([A-Za-z0-9+\\/]{4})*([A-Za-z0-9+\\/]{4}|[A-Za-z0-9+\\/]{3}=|[A-Za-z0-9+\\/]{2}==)$"
    );

    /**
     * Pattern for 16 symbol hex string.
     */
    private static final Pattern HEX = Pattern.compile("[A-Fa-f0-9]{16}");

    /**
     * Pattern for parsing Signature.
     */
    private static final Pattern SIGN = Pattern.compile("[A-Za-z0-9+/]+={0,3}");

    /**
     * Pattern for Details string.
     */
    private static final Pattern DTLS =
        Pattern.compile("[A-Za-z0-9 -.]{1,512}");

    /**
     * Pattern for ID String.
     */
    private static final Pattern IDENT = Pattern.compile("[A-Fa-f0-9]{4}");

    /**
     * String representation of transaction.
     */
    private final String transaction;

    /**
     * Ctor.
     * @param trnsct String representation of transaction
     */
    RtTransaction(final String trnsct) throws IOException {
        String tempTransaction;
        try {
            final String trimmed = trnsct.trim();
            if (trimmed.isEmpty()) {
                throw new IOException(
                    "Invalid transaction string: string is empty"
                );
            }
            final List<String> pieces = Arrays.asList(trimmed.split(";"));
            // @checkstyle MagicNumberCheck (1 line)
            if (pieces.size() != 7) {
                throw new IOException(
                    new FormattedText(
                        // @checkstyle LineLength (1 line)
                        "Invalid transaction string: expected 7 fields, but found %d",
                        pieces.size()
                    ).asString()
                );
            }
            tempTransaction = trnsct;
        } catch (final IOException ex) {
            throw ex;
        } catch (final Exception ex) {
            throw new IOException(ex);
        }
        this.transaction = tempTransaction;
    }

    @Override
    @SuppressWarnings("PMD.ShortMethodName")
    public int id() throws IOException {
        try {
            final String ident = new ItemAt<>(
                0, new ListOf<>(this.transaction.split(";"))
            ).value();
            if (!RtTransaction.IDENT.matcher(ident).matches()) {
                throw new IOException(
                    new FormattedText(
                        // @checkstyle LineLength (1 line)
                        "Invalid ID '%s' expecting 16-bit unsigned hex string with 4 symbols",
                        ident
                    ).asString()
                );
            }
            // @checkstyle MagicNumber (1 line)
            return Integer.parseUnsignedInt(ident, 16);
        } catch (final IOException ex) {
            throw ex;
        } catch (final Exception ex) {
            throw new IOException(ex);
        }
    }

    @Override
    public ZonedDateTime time() throws IOException {
        try {
            return new ZonedDateTimeOf(
                new ItemAt<>(
                    1, new ListOf<>(this.transaction.split(";"))
                ).value(),
                DateTimeFormatter.ISO_OFFSET_DATE_TIME
            ).value();
        } catch (final IOException ex) {
            throw ex;
        } catch (final Exception ex) {
            throw new IOException(ex);
        }
    }

    @Override
    public long amount() throws IOException {
        try {
            final String amnt = new ItemAt<>(
                2, new ListOf<>(this.transaction.split(";"))
            ).value();
            if (!RtTransaction.HEX.matcher(amnt).matches()) {
                throw new IOException(
                    new FormattedText(
                        // @checkstyle LineLength (1 line)
                        "Invalid amount '%s' expecting 64-bit signed hex string with 16 symbols",
                        amnt
                    ).asString()
                );
            }
            // @checkstyle MagicNumber (1 line)
            return new BigInteger(amnt, 16).longValue();
        } catch (final IOException ex) {
            throw ex;
        } catch (final Exception ex) {
            throw new IOException(ex);
        }
    }

    @Override
    public String prefix() throws IOException {
        try {
            final String prefix = new ItemAt<>(
                //@checkstyle MagicNumberCheck (1 line)
                3, new ListOf<>(this.transaction.split(";"))
            ).value();
            //@checkstyle MagicNumberCheck (1 line)
            if (prefix.length() < 8 || prefix.length() > 32) {
                throw new IOException("Invalid prefix size");
            }
            if (!RtTransaction.PREFIX.matcher(prefix).matches()) {
                throw new IOException("Invalid base64 prefix");
            }
            return prefix;
        } catch (final IOException ex) {
            throw ex;
        } catch (final Exception ex) {
            throw new IOException(ex);
        }
    }

    @Override
    public String bnf() throws IOException {
        try {
            final String bnf = new ItemAt<>(
                //@checkstyle MagicNumberCheck (1 line)
                4, new ListOf<>(this.transaction.split(";"))
            ).value();
            if (!RtTransaction.HEX.matcher(bnf).matches()) {
                throw new IOException(
                    new FormattedText(
                        // @checkstyle LineLength (1 line)
                        "Invalid bnf string '%s', expecting hex string with 16 symbols",
                        bnf
                    ).asString()
                );
            }
            return bnf;
        } catch (final IOException ex) {
            throw ex;
        } catch (final Exception ex) {
            throw new IOException(ex);
        }
    }

    @Override
    public String details() throws IOException {
        try {
            final String dtls = new ItemAt<>(
                //@checkstyle MagicNumberCheck (1 line)
                5, new ListOf<>(this.transaction.split(";"))
            ).value();
            if (!RtTransaction.DTLS.matcher(dtls).matches()) {
                throw new IOException(
                    new FormattedText(
                        // @checkstyle LineLength (1 line)
                        "Invalid details string '%s', does not match pattern '%s'",
                        dtls, RtTransaction.DTLS
                    ).asString()
                );
            }
            return dtls;
        } catch (final IOException ex) {
            throw ex;
        } catch (final Exception ex) {
            throw new IOException(ex);
        }
    }

    @Override
    public String signature() throws IOException {
        try {
            final String sign = new ItemAt<>(
                //@checkstyle MagicNumberCheck (1 line)
                6, new ListOf<>(this.transaction.split(";"))
            ).value();
            // @checkstyle MagicNumber (1 line)
            if (sign.length() != 684
                || !RtTransaction.SIGN.matcher(sign).matches()) {
                throw new IOException(
                    new FormattedText(
                        // @checkstyle LineLength (1 line)
                        "Invalid signature '%s', expecting base64 string with 684 characters",
                        sign
                    ).asString()
                );
            }
            return sign;
        } catch (final IOException ex) {
            throw ex;
        } catch (final Exception ex) {
            throw new IOException(ex);
        }
    }

    @Override
    public String toString() {
        return this.transaction;
    }

    @Override
    @SuppressWarnings("PMD.OnlyOneReturn")
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || this.getClass() != obj.getClass()) {
            return false;
        }
        final RtTransaction that = (RtTransaction) obj;
        return this.transaction.equals(that.transaction);
    }

    @Override
    public int hashCode() {
        return this.transaction.hashCode();
    }
}
