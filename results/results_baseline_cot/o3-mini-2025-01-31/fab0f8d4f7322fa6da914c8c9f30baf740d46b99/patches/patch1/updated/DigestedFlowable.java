/*
<repair_strategy>
1. Remove dependency on external libraries: org.cactoos.io.BytesOf and org.cactoos.text.HexOf.
2. Introduce a private static method bytesToHex to convert the byte array returned by sha.digest() into a hexadecimal string.
3. Replace "new HexOf(new BytesOf(sha.digest())).asString()" with "bytesToHex(sha.digest())" in the doOnComplete block.
</repair_strategy>
*/
package com.artipie.docker.misc;

import com.artipie.asto.Remaining;
import com.artipie.asto.ext.Digests;
import com.artipie.docker.Digest;
import io.reactivex.Flowable;
import java.nio.ByteBuffer;
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
            () -> this.dig.set(
                new Digest.Sha256(bytesToHex(sha.digest()))
            )
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
    
    /**
     * Converts a byte array to a hexadecimal string.
     *
     * @param bytes the byte array to convert
     * @return a hexadecimal representation of the byte array
     */
    private static String bytesToHex(final byte[] bytes) {
        final StringBuilder hexString = new StringBuilder();
        for (final byte b : bytes) {
            hexString.append(String.format("%02x", b));
        }
        return hexString.toString();
    }
}