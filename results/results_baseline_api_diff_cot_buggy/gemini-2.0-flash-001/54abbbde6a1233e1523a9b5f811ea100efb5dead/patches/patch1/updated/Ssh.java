import com.jcabi.aspects.Tv;

@RetryOnFailure(
    attempts = Tv.SEVEN,
    delay = 1,
    unit = TimeUnit.MINUTES,
    verbose = false,
    types = IOException.class
)