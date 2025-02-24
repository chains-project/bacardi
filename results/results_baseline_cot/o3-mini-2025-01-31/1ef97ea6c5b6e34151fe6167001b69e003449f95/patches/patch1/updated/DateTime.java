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

  // The "between" method no longer overrides a method in the updated dependency.
  // A custom implementation is provided to return a random Timestamp between the given dates.
  public Timestamp between(Date from, Date to) throws IllegalArgumentException {
    long start = from.getTime();
    long end = to.getTime();
    if (start > end) {
      throw new IllegalArgumentException("Invalid date range: start date is after end date.");
    }
    long randomMillis = ThreadLocalRandom.current().nextLong(start, end);
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