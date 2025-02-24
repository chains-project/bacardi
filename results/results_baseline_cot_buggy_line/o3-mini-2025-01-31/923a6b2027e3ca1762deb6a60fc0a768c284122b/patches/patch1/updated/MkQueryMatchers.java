/*
 * Copyright (c) 2011-2017, jcabi.com
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met: 1) Redistributions of source code must retain the above
 * copyright notice, this list of conditions and the following
 * disclaimer. 2) Redistributions in binary form must reproduce the above
 * copyright notice, this list of conditions and the following
 * disclaimer in the documentation and/or other materials provided
 * with the distribution. 3) Neither the name of the jcabi.com nor
 * the names of its contributors may be used to endorse or promote
 * products derived from this software without specific prior written
 * permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT
 * NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND
 * FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL
 * THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT,
 * INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION)
 * HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT,
 * STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED
 * OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package com.jcabi.http.mock;

import java.net.URI;
import java.util.function.Function;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;

/**
 * Convenient set of matchers for {@link MkQuery}.
 *
 * @since 1.5
 */
@SuppressWarnings("PMD.ProhibitPublicStaticMethods")
public final class MkQueryMatchers {

    /**
     * Private ctor.
     */
    private MkQueryMatchers() {
        // Utility class - cannot instantiate
    }

    /**
     * Matches the value of the MkQuery's body against the given matcher.
     *
     * @param matcher The matcher to use.
     * @return Matcher for checking the body of MkQuery
     */
    public static Matcher<MkQuery> hasBody(final Matcher<String> matcher) {
        return new MkQueryBodyMatcher(matcher);
    }

    /**
     * Matches the content of the MkQuery's header against the given matcher.
     * Note that for a valid match to occur, the header entry must exist
     * <i>and</i> its value(s) must match the given matcher.
     *
     * @param header The header to check.
     * @param matcher The matcher to use.
     * @return Matcher for checking the header of MkQuery
     */
    public static Matcher<MkQuery> hasHeader(
        final String header,
        final Matcher<Iterable<? extends String>> matcher
    ) {
        return new MkQueryHeaderMatcher(header, matcher);
    }

    /**
     * Matches the path of the MkQuery.
     *
     * @param path The matcher that checks the path.
     * @return Matcher for checking the path of MkQuery
     */
    public static Matcher<MkQuery> hasPath(final Matcher<String> path) {
        return new MkQueryUriMatcher(
            new UriPropertyMatcher("rawPath", URI::getRawPath, path)
        );
    }

    /**
     * Matches the query of the MkQuery.
     *
     * @param query The matcher to check the query.
     * @return Matcher for checking the query of MkQuery
     */
    public static Matcher<MkQuery> hasQuery(final Matcher<String> query) {
        return new MkQueryUriMatcher(
            new UriPropertyMatcher("rawQuery", URI::getRawQuery, query)
        );
    }

    /**
     * A custom matcher that checks a specific String property of a URI.
     */
    private static final class UriPropertyMatcher extends TypeSafeMatcher<URI> {
        /**
         * Name of the property, used only for description.
         */
        private final String propertyName;

        /**
         * Function to extract the property value from the URI.
         */
        private final Function<URI, String> extractor;

        /**
         * Matcher to check the extracted property.
         */
        private final Matcher<String> matcher;

        /**
         * Public ctor.
         *
         * @param propertyName Name of the property (e.g. "rawPath" or "rawQuery")
         * @param extractor Function to extract the property from a URI
         * @param matcher Matcher for the extracted property value
         */
        UriPropertyMatcher(final String propertyName,
                           final Function<URI, String> extractor,
                           final Matcher<String> matcher) {
            this.propertyName = propertyName;
            this.extractor = extractor;
            this.matcher = matcher;
        }

        @Override
        protected boolean matchesSafely(final URI uri) {
            return matcher.matches(extractor.apply(uri));
        }

        @Override
        public void describeTo(final Description description) {
            description.appendText("URI with ")
                       .appendValue(propertyName)
                       .appendText(" matching: ");
            matcher.describeTo(description);
        }
    }

}