package com.maxmind.minfraud.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.maxmind.minfraud.AbstractModel;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.nio.charset.StandardCharsets;

/**
 * Account related data for the minFraud request
 */
public final class Account extends AbstractModel {
    private final String userId;
    private final String usernameMd5;

    private Account(Account.Builder builder) {
        userId = builder.userId;
        usernameMd5 = builder.usernameMd5;
    }

    /**
     * {@code Builder} creates instances of {@code Account}
     * from values set by the builder's methods.
     */
    public static final class Builder {
        String userId;
        String usernameMd5;

        /**
         * @param id A unique user ID associated with the end-user in your
         *           system. If your system allows the login name for the
         *           account to be changed, this should not be the login
         *           name for the account, but rather should be an internal
         *           ID that does not change. This is not your MaxMind user
         *           ID.
         * @return The builder object.
         */
        public Account.Builder userId(String id) {
            this.userId = id;
            return this;
        }

        /**
         * @param username The username associated with the account. This is
         *                 <em>not</em> the MD5 of username. This method
         *                 automatically computes the MD5 hexadecimal digest
         *                 of the string passed to it.
         * @return The builder object.
         */
        public Account.Builder username(String username) {
            this.usernameMd5 = computeMD5Hex(username);
            return this;
        }

        /**
         * Computes the MD5 hexadecimal digest of a given string.
         *
         * @param input the string to hash
         * @return the MD5 hash as a hexadecimal string
         */
        private static String computeMD5Hex(String input) {
            try {
                MessageDigest md = MessageDigest.getInstance("MD5");
                byte[] digest = md.digest(input.getBytes(StandardCharsets.UTF_8));
                StringBuilder hexString = new StringBuilder();
                for (byte b : digest) {
                    String hex = Integer.toHexString(0xff & b);
                    if (hex.length() == 1) hexString.append('0');
                    hexString.append(hex);
                }
                return hexString.toString();
            } catch (NoSuchAlgorithmException e) {
                throw new RuntimeException("MD5 algorithm not available", e);
            }
        }

        /**
         * @return An instance of {@code Account} created from the
         * fields set on this builder.
         */
        public Account build() {
            return new Account(this);
        }
    }

    /**
     * @return The user ID.
     */
    @JsonProperty("user_id")
    public String getUserId() {
        return userId;
    }

    /**
     * @return The MD5 of the username passed to the builder.
     */
    @JsonProperty("username_md5")
    public String getUsernameMd5() {
        return usernameMd5;
    }
}