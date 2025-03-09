import org.cactoos.text.RandomText;

public interface Remote {
    ...
    public Fake(final int val) {
        this(new RtScore(
            new Repeated<>(val, new RandomText())
        ));
    }
    ...
}