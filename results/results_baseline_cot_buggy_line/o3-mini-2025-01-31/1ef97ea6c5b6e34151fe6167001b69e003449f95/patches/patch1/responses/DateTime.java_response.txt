package com.github.knaufk.flink.faker;

import java.sql.Timestamp;
import java.util.Date;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.ThreadLocalRandom;
import net.datafaker.DateAndTime;
import net.datafaker.Faker;

public class DateTime extends DateAndTime {

  protected DateTime(Faker faker) {
    super(faker);
  }

  public Timestamp past(int atMost, TimeUnit unit) {
    return new Timestamp(super.past(atMost, unit).getTime());
  }

  public Timestamp past(int atMost, int minimum, TimeUnit unit) {
    return new Timestamp(super.past(atMost, minimum, unit).getTime());
  }

  @Override
  public Timestamp future(int atMost, TimeUnit unit) {
    return new Timestamp(super.future(atMost, unit).getTime());
  }

  @Override
  public Timestamp future(int atMost, int minimum, TimeUnit unit) {
    return new Timestamp(super.future(atMost, minimum, unit).getTime());
  }

  @Override
  public Timestamp future(int atMost, TimeUnit unit, Date referenceDate) {
    return new Timestamp(super.future(atMost, unit, referenceDate).getTime());
  }

  @Override
  public Timestamp past(int atMost, TimeUnit unit, Date referenceDate) {
    return new Timestamp(super.past(atMost, unit, referenceDate).getTime());
  }

  // Removed the @Override annotation, as the parent no longer provides this method.
  public Timestamp between(Date from, Date to) throws IllegalArgumentException {
    if (from == null || to == null) {
      throw new IllegalArgumentException("Dates must not be null");
    }
    if (from.after(to)) {
      throw new IllegalArgumentException("Invalid date range: 'from' is after 'to'");
    }
    long startMillis = from.getTime();
    long endMillis = to.getTime();
    // If both dates are equal, return that timestamp.
    if (startMillis == endMillis) {
      return new Timestamp(startMillis);
    }
    // Generate a random millisecond value between startMillis (inclusive) and endMillis (inclusive).
    long randomMillis = ThreadLocalRandom.current().nextLong(startMillis, endMillis + 1);
    return new Timestamp(randomMillis);
  }

  @Override
  public Timestamp birthday() {
    return new Timestamp(super.birthday().getTime());
  }

  @Override
  public Timestamp birthday(int minAge, int maxAge) {
    return new Timestamp(super.birthday(minAge, maxAge).getTime());
  }
}