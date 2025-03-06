package com.jcabi.simpledb;

import com.amazonaws.services.simpledb.model.SelectRequest;
import org.apache.commons.lang3.RandomStringUtils;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

/**
 * Integration case for {@link Region}.
 *
 * @since 0.1
 */
final class RegionITCase {

    /**
     * SimpleDB key.
     */
    private static final String KEY =
        System.getProperty("failsafe.sdb.key");

    /**
     * SimpleDB secret key.
     */
    private static final String SECRET =
        System.getProperty("failsafe.sdb.secret");

    /**
     * Constant for ten.
     */
    private static final int TEN = 10;

    /**
     * Constant for eight.
     */
    private static final int EIGHT = 8;

    @Test
    void putsAndRemovesIndividualItems() {
        final Domain domain = this.domain();
        try {
            final String name = RandomStringUtils.randomAlphanumeric(TEN);
            final String attr = RandomStringUtils.randomAlphabetic(EIGHT);
            final String value = RandomStringUtils.randomAlphanumeric(TEN);
            for (int idx = 0; idx < 2; ++idx) {
                domain.item(name).put(attr, value);
                MatcherAssert.assertThat(
                    domain.item(name), Matchers.hasKey(attr)
                );
                domain.item(name).remove(attr);
                MatcherAssert.assertThat(
                    domain.item(name), Matchers.not(Matchers.hasKey(attr))
                );
            }
        } finally {
            domain.drop();
        }
    }

    @Test
    void selectsMultipleItems() {
        final Domain domain = this.domain();
        try {
            final String attr = "alpha";
            domain.item("first").put(attr, "val-99");
            domain.item("second").put("beta", "");
            MatcherAssert.assertThat(
                domain.select(
                    new SelectRequest().withSelectExpression(
                        String.format(
                            "SELECT * FROM `%s` WHERE `%s` = 'val-99'",
                            domain.name(), attr
                        )
                    ).withConsistentRead(true)
                ),
                Matchers.hasItem(Matchers.hasKey(attr))
            );
        } finally {
            domain.drop();
        }
    }

    /**
     * Region.Simple can select many items.
     */
    @Test
    void selectsManyItems() {
        final Domain domain = this.domain();
        try {
            for (int idx = 0; idx < TEN; ++idx) {
                domain.item(String.format("i-%d", idx)).put("hey", "");
            }
            MatcherAssert.assertThat(
                domain.select(
                    new SelectRequest().withSelectExpression(
                        String.format("SELECT * FROM `%s`", domain.name())
                    ).withConsistentRead(true)
                ),
                Matchers.iterableWithSize(TEN)
            );
        } finally {
            domain.drop();
        }
    }

    /**
     * Make domain.
     * @return Domain
     */
    private Domain domain() {
        Assumptions.assumeFalse(RegionITCase.KEY.isEmpty());
        final Region region = new Region.Simple(
            new Credentials.Simple(RegionITCase.KEY, RegionITCase.SECRET)
        );
        final Domain domain = region.domain(
            String.format(
                "jcabi-test-%s",
                RandomStringUtils.randomAlphabetic(5)
            )
        );
        domain.create();
        return domain;
    }

}