package io.zold.api;

import java.io.IOException;
import java.math.BigInteger;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.regex.Pattern;

/**
 * RtTransaction.
 *
 * @since 0.1
 * @checkstyle ClassDataAbstractionCoupling (3 lines)
 */
@SuppressWarnings({"PMD.AvoidFieldNameMatchingMethodName", "PMD.ShortMethodName"})
final class RtTransaction implements Transaction {

    /**
     * Pattern for Prefix String.
     */
    private static final Pattern PREFIX = Pattern.compile(
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
    private static final Pattern DTLS = Pattern.compile("[A-Za-z0-9 -.]{1,512}");

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
     * @throws IOException If an I/O error occurs
     */
    RtTransaction(final String trnsct) throws IOException {
        this.transaction = trnsct.trim();
        if (this.transaction.isEmpty()) {
            throw new IOException("Invalid transaction string: string is empty");
        }
        final String[] pieces = this.transaction.split(";");
        if (pieces.length != 7) {
            throw new IOException(
                String.format(
                    "Invalid transaction string: expected 7 fields, but found %d",
                    pieces.length
                )
            );
        }
    }

    @Override
    public int id() throws IOException {
        final String ident = pieces[0];
        if (!RtTransaction.IDENT.matcher(ident).matches()) {
            throw new IOException(
                String.format(
                    "Invalid ID '%s' expecting 16-bit unsigned hex string with 4 symbols",
                    ident
                )
            );
        }
        return Integer.parseUnsignedInt(ident, 16);
    }

    @Override
    public ZonedDateTime time() throws IOException {
        return ZonedDateTime.parse(
            pieces[1],
            DateTimeFormatter.ISO_OFFSET_DATE_TIME
        );
    }

    @Override
    public long amount() throws IOException {
        final String amnt = pieces[2];
        if (!RtTransaction.HEX.matcher(amnt).matches()) {
            throw new IOException(
                String.format(
                    "Invalid amount '%s' expecting 64-bit signed hex string with 16 symbols",
                    amnt
                )
            );
        }
        return new BigInteger(amnt, 16).longValue();
    }

    @Override
    public String prefix() throws IOException {
        final String prefix = pieces[3];
        if (prefix.length() < 8 || prefix.length() > 32) {
            throw new IOException("Invalid prefix size");
        }
        if (!RtTransaction.PREFIX.matcher(prefix).matches()) {
            throw new IOException("Invalid base64 prefix");
        }
        return prefix;
    }

    @Override
    public String bnf() throws IOException {
        final String bnf = pieces[4];
        if (!RtTransaction.HEX.matcher(bnf).matches()) {
            throw new IOException(
                String.format(
                    "Invalid bnf string '%s', expecting hex string with 16 symbols",
                    bnf
                )
            );
        }
        return bnf;
    }

    @Override
    public String details() throws IOException {
        final String dtls = pieces[5];
        if (!RtTransaction.DTLS.matcher(dtls).matches()) {
            throw new IOException(
                String.format(
                    "Invalid details string '%s', does not match pattern '%s'",
                    dtls, RtTransaction.DTLS
                )
            );
        }
        return dtls;
    }

    @Override
    public String signature() throws IOException {
        final String sign = pieces[6];
        if (sign.length() != 684 || !RtTransaction.SIGN.matcher(sign).matches()) {
            throw new IOException(
                String.format(
                    "Invalid signature '%s', expecting base64 string with 684 characters",
                    sign
                )
            );
        }
        return sign;
    }

    @Override
    public String toString() {
        return this.transaction;
    }

    @Override
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