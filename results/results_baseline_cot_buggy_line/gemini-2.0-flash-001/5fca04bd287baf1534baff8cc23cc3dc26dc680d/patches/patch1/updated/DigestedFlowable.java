/*
 * The MIT License (MIT) Copyright (c) 2020-2021 artipie.com
 * https://github.com/artipie/docker-adapter/LICENSE.txt
 */
package com.artipie.docker.misc;

import com.artipie.asto.Remaining;
import com.artipie.asto.ext.Digests;
import com.artipie.docker.Digest;
import io.reactivex.Flowable;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;

/**
 * {@link Flowable} that calculates digest of origin {@link Publisher} bytes when they pass by.
 *
 * @since 0.12
 */
public final class DigestedFlowable extends Flowable<ByteBuffer> {

    /**
     * Origin publisher.
     */
    private final Publisher<ByteBuffer> origin;

    /**
     * Calculated digest.
     */
    private final AtomicReference<Digest> dig;

    /**
     * Ctor.
     *
     * @param origin Origin publisher.
     */
    public DigestedFlowable(final Publisher<ByteBuffer> origin) {
        this.dig = new AtomicReference<>();
        this.origin = origin;
    }

    @Override
    public void subscribeActual(final Subscriber<? super ByteBuffer> subscriber) {
        final MessageDigest sha = Digests.SHA256.get();
        Flowable.fromPublisher(this.origin).map(
            buf -> {
                sha.update(new Remaining(buf, true).bytes());
                return buf;
            }
        ).doOnComplete(
            () -> {
                byte[] digestBytes = sha.digest();
                StringBuilder hexString = new StringBuilder(2 * digestBytes.length);
                for (byte b : digestBytes) {
                    String hex = Integer.toHexString(0xff & b);
                    if (hex.length() == 1) {
                        hexString.append('0');
                    }
                    hexString.append(hex);
                }
                this.dig.set(
                    new Digest.Sha256(hexString.toString())
                );
            }
        ).subscribe(subscriber);
    }

    /**
     * Calculated digest.
     *
     * @return Digest.
     */
    public Digest digest() {
        return Objects.requireNonNull(this.dig.get(), "Digest is not yet calculated.");
    }
}