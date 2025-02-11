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

import java.io.IOException;
import java.math.BigInteger;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.regex.Pattern;

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
     * The original transaction string.
     */
    private final String transaction;

    /**
     * The split parts of the transaction.
     */
    private final String[] parts;

    /**
     * Ctor.
     * @param trnsct String representation of transaction
     * @throws IOException if the transaction string is invalid
     */
    RtTransaction(final String trnsct) throws IOException {
        if (trnsct == null || trnsct.trim().isEmpty()) {
            throw new IOException("Invalid transaction string: string is empty");
        }
        String[] fields = trnsct.split(";", -1);
        if (fields.length != 7) {
            throw new IOException(String.format(
                "Invalid transaction string: expected 7 fields, but found %d", fields.length));
        }
        this.transaction = trnsct;
        this.parts = fields;
    }

    @Override
    @SuppressWarnings("PMD.ShortMethodName")
    public int id() throws IOException {
        final String ident = this.parts[0].trim();
        if (!RtTransaction.IDENT.matcher(ident).matches()) {
            throw new IOException(String.format(
                "Invalid ID '%s' expecting 16-bit unsigned hex string with 4 symbols",
                ident));
        }
        return Integer.parseUnsignedInt(ident, 16);
    }

    @Override
    public ZonedDateTime time() throws IOException {
        final String timeStr = this.parts[1].trim();
        try {
            return ZonedDateTime.parse(timeStr, DateTimeFormatter.ISO_OFFSET_DATE_TIME);
        } catch (Exception e) {
            throw new IOException("Invalid time format: " + timeStr, e);
        }
    }

    @Override
    public long amount() throws IOException {
        final String amnt = this.parts[2].trim();
        if (!RtTransaction.HEX.matcher(amnt).matches()) {
            throw new IOException(String.format(
                "Invalid amount '%s' expecting 64-bit signed hex string with 16 symbols",
                amnt));
        }
        return new BigInteger(amnt, 16).longValue();
    }

    @Override
    public String prefix() throws IOException {
        final String pfx = this.parts[3].trim();
        if (pfx.length() < 8 || pfx.length() > 32) {
            throw new IOException("Invalid prefix size");
        }
        if (!RtTransaction.PREFIX.matcher(pfx).matches()) {
            throw new IOException("Invalid base64 prefix");
        }
        return pfx;
    }

    @Override
    public String bnf() throws IOException {
        final String bnfStr = this.parts[4].trim();
        if (!RtTransaction.HEX.matcher(bnfStr).matches()) {
            throw new IOException(String.format(
                "Invalid bnf string '%s', expecting hex string with 16 symbols", bnfStr));
        }
        return bnfStr;
    }

    @Override
    public String details() throws IOException {
        final String dtlsStr = this.parts[5].trim();
        if (!RtTransaction.DTLS.matcher(dtlsStr).matches()) {
            throw new IOException(String.format(
                "Invalid details string '%s', does not match pattern '%s'",
                dtlsStr, DTLS.pattern()));
        }
        return dtlsStr;
    }

    @Override
    public String signature() throws IOException {
        final String sign = this.parts[6].trim();
        if (sign.length() != 684 || !RtTransaction.SIGN.matcher(sign).matches()) {
            throw new IOException(String.format(
                "Invalid signature '%s', expecting base64 string with 684 characters", sign));
        }
        return sign;
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