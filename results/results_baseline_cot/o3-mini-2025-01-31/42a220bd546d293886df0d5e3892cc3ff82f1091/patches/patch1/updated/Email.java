package com.maxmind.minfraud.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.maxmind.minfraud.AbstractModel;
import org.apache.commons.validator.routines.DomainValidator;
import org.apache.commons.validator.routines.EmailValidator;
import java.net.IDN;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.nio.charset.StandardCharsets;

public final class Email extends AbstractModel {
    private final String address;
    private final boolean hashAddress;
    private final String domain;
    private static final Map<String, String> typoDomains;

    static {
        HashMap<String, String> m = new HashMap<>() {{
            put("35gmai.com", "gmail.com");
            put("636gmail.com", "gmail.com");
            put("gamil.com", "gmail.com");
            put("gmail.comu", "gmail.com");
            put("gmial.com", "gmail.com");
            put("gmil.com", "gmail.com");
            put("yahoogmail.com", "gmail.com");
            put("putlook.com", "outlook.com");
        }};

        typoDomains = Collections.unmodifiableMap(m);
    }

    private Email(Email.Builder builder) {
        address = builder.address;
        hashAddress = builder.hashAddress;
        domain = builder.domain;
    }

    public static final class Builder {
        private final boolean enableValidation;
        private String address;
        private boolean hashAddress;
        private String domain;

        public Builder() {
            enableValidation = true;
        }

        public Builder(boolean enableValidation) {
            this.enableValidation = enableValidation;
        }

        public Email.Builder address(String address) {
            if (enableValidation && !EmailValidator.getInstance().isValid(address)) {
                throw new IllegalArgumentException("The email address " + address + " is not valid.");
            }

            if (this.domain == null) {
                int domainIndex = address.lastIndexOf('@') + 1;
                if (domainIndex > 0 && domainIndex < address.length()) {
                    this.domain = address.substring(domainIndex);
                }
            }
            this.address = address;
            return this;
        }

        public Email.Builder hashAddress() {
            this.hashAddress = true;
            return this;
        }

        public Email.Builder domain(String domain) {
            if (enableValidation && !DomainValidator.getInstance().isValid(domain)) {
                throw new IllegalArgumentException("The email domain " + domain + " is not valid.");
            }
            this.domain = domain;
            return this;
        }

        public Email build() {
            return new Email(this);
        }
    }

    @JsonProperty("address")
    public String getAddress() {
        if (address == null) {
            return null;
        }
        if (hashAddress) {
            return md5Hex(cleanAddress(address));
        }
        return address;
    }

    private String cleanAddress(String address) {
        address = address.trim().toLowerCase();

        int domainIndex = address.lastIndexOf('@');
        if (domainIndex == -1 || domainIndex + 1 == address.length()) {
            return address;
        }

        String localPart = address.substring(0, domainIndex);
        String domain = address.substring(domainIndex + 1);

        domain = cleanDomain(domain);

        int stopChar;
        if (domain.equals("yahoo.com")) {
            stopChar = '-';
        } else {
            stopChar = '+';
        }
        int stopCharIndex = localPart.indexOf(stopChar);
        if (stopCharIndex > 0) {
            localPart = localPart.substring(0, stopCharIndex);
        }

        return localPart + "@" + domain;
    }

    private String cleanDomain(String domain) {
        if (domain == null) {
            return null;
        }

        domain = domain.trim();

        if (domain.endsWith(".")) {
            domain = domain.substring(0, domain.length() - 1);
        }

        domain = IDN.toASCII(domain);

        if (typoDomains.containsKey(domain)) {
            domain = typoDomains.get(domain);
        }

        return domain;
    }

    @JsonProperty("domain")
    public String getDomain() {
        return domain;
    }

    private static String md5Hex(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] digest = md.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : digest) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }
}