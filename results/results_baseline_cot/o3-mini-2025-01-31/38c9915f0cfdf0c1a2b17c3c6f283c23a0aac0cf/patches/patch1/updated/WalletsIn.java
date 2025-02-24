package io.zold.api;

import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Iterator;
import java.util.Random;
import org.cactoos.Scalar;
import org.cactoos.func.IoCheckedFunc;
import org.cactoos.io.Directory;
import org.cactoos.iterable.Filtered;
import org.cactoos.iterable.Mapped;
import org.cactoos.scalar.Sticky;
import org.cactoos.text.FormattedText;
import org.cactoos.text.Joined;
import org.cactoos.text.UncheckedText;

/**
 * Wallets in path.
 * @since 0.1
 * @checkstyle ClassDataAbstractionCoupling (2 lines)
 */
public final class WalletsIn implements Wallets {

    /**
     * Path containing wallets.
     */
    private final Scalar<Path> path;

    /**
     * Filter for matching file extensions.
     */
    private final IoCheckedFunc<Path, Boolean> filter;

    /**
     * Wallets file extension.
     */
    private final String ext;

    /**
     * Randomizer.
     */
    private final Random random;

    /**
     * Ctor.
     * @param pth Path with wallets
     */
    public WalletsIn(final Path pth) {
        this(
            () -> pth,
            "z",
            new Random()
        );
    }

    /**
     * Ctor.
     * @param pth Path with wallets
     * @param random Randomizer
     */
    public WalletsIn(final Path pth, final Random random) {
        this(
            () -> pth,
            "z",
            random
        );
    }

    /**
     * Ctor.
     * @param pth Path with wallets
     * @param random Randomizer
     * @param ext Wallets file extension
     */
    public WalletsIn(final Scalar<Path> pth, final String ext,
        final Random random) {
        this.path = new Sticky<>(pth);
        this.filter = new IoCheckedFunc<Path, Boolean>(
            (file) -> file.toFile().isFile()
                && FileSystems.getDefault()
                .getPathMatcher(String.format("glob:**.%s", ext))
                .matches(file)
        );
        this.ext = ext;
        this.random = random;
    }

    @Override
    public Wallet create() throws IOException {
        final Path base = this.base();
        final Path wpth = base.resolve(
            new Joined(
                ".",
                Long.toHexString(this.random.nextLong()),
                this.ext
            ).asString()
        );
        if (wpth.toFile().exists()) {
            throw new IOException(
                new UncheckedText(
                    new FormattedText(
                        "Wallet in path %s already exists",
                        wpth.toUri().getPath()
                    )
                ).asString()
            );
        }
        Files.createFile(wpth);
        return new Wallet.File(wpth);
    }

    @Override
    // @todo #65:30min Create the new wallet in the path with all wallets.
    //  It should contain the correct content according to the
    //  white paper (network, protocol version, id and public RSA key). After
    //  this remove exception expect for tests on WalletsInTest.
    public Wallet create(final long id, final String pubkey, final String
        network) throws IOException {
        throw new UnsupportedOperationException(
            "WalletsIn.create(String, String, String) not supported"
        );
    }

    @Override
    public Iterator<Wallet> iterator() {
        try {
            return new Mapped<Path, Wallet>(
                (pth) -> new Wallet.File(pth),
                new Filtered<>(this.filter, new Directory(this.base()))
            ).iterator();
        } catch (final IOException ex) {
            throw new IllegalStateException(ex);
        }
    }
    
    /**
     * Returns the base path from the scalar, wrapping any exception into an IOException.
     * @return Base path.
     * @throws IOException if obtaining the path fails.
     */
    private Path base() throws IOException {
        try {
            return this.path.value();
        } catch (final Exception ex) {
            throw new IOException(ex);
        }
    }
}