package io.zold.api;

import java.io.IOException;
import java.math.BigInteger;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.regex.Pattern;

final class RtTransaction implements Transaction {

    private static final Pattern PREFIX = Pattern.compile(
        "^([A-Za-z0-9+/]{4})*([A-Za-z0-9+/]{4}|[A-Za-z0-9+/]{3}=|[A-Za-z0-9+/]{2}==)$"
    );

    private static final Pattern HEX = Pattern.compile("[A-Fa-f0-9]{16}");

    private static final Pattern SIGN = Pattern.compile("[A-Za-z0-9+/]+={0,3}");

    private static final Pattern DTLS = Pattern.compile("[A-Za-z0-9 -.]{1,512}");

    private static final Pattern IDENT = Pattern.compile("[A-Fa-f0-9]{4}");

    private final String transaction;
    private String[] tokensCache;

    RtTransaction(final String trnsct) {
        this.transaction = trnsct;
    }

    private String[] tokens() throws IOException {
        if (this.tokensCache == null) {
            if (this.transaction.trim().isEmpty()) {
                throw new IOException("Invalid transaction string: string is empty");
            }
            String[] parts = this.transaction.split(";");
            if (parts.length != 7) {
                throw new IOException(String.format(
                    "Invalid transaction string: expected 7 fields, but found %d",
                    parts.length
                ));
            }
            this.tokensCache = parts;
        }
        return this.tokensCache;
    }

    @Override
    public int id() throws IOException {
        String ident = tokens()[0];
        if (!IDENT.matcher(ident).matches()) {
            throw new IOException(String.format(
                "Invalid ID '%s' expecting 16-bit unsigned hex string with 4 symbols",
                ident
            ));
        }
        return Integer.parseUnsignedInt(ident, 16);
    }

    @Override
    public ZonedDateTime time() throws IOException {
        return ZonedDateTime.parse(tokens()[1], DateTimeFormatter.ISO_OFFSET_DATE_TIME);
    }

    @Override
    public long amount() throws IOException {
        String amnt = tokens()[2];
        if (!HEX.matcher(amnt).matches()) {
            throw new IOException(String.format(
                "Invalid amount '%s' expecting 64-bit signed hex string with 16 symbols",
                amnt
            ));
        }
        return new BigInteger(amnt, 16).longValue();
    }

    @Override
    public String prefix() throws IOException {
        String prefix = tokens()[3];
        if (prefix.length() < 8 || prefix.length() > 32) {
            throw new IOException("Invalid prefix size");
        }
        if (!PREFIX.matcher(prefix).matches()) {
            throw new IOException("Invalid base64 prefix");
        }
        return prefix;
    }

    @Override
    public String bnf() throws IOException {
        String bnf = tokens()[4];
        if (!HEX.matcher(bnf).matches()) {
            throw new IOException(String.format(
                "Invalid bnf string '%s', expecting hex string with 16 symbols",
                bnf
            ));
        }
        return bnf;
    }

    @Override
    public String details() throws IOException {
        String dtls = tokens()[5];
        if (!DTLS.matcher(dtls).matches()) {
            throw new IOException(String.format(
                "Invalid details string '%s', does not match pattern '%s'",
                dtls, DTLS.toString()
            ));
        }
        return dtls;
    }

    @Override
    public String signature() throws IOException {
        String sign = tokens()[6];
        if (sign.length() != 684 || !SIGN.matcher(sign).matches()) {
            throw new IOException(String.format(
                "Invalid signature '%s', expecting base64 string with 684 characters",
                sign
            ));
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
