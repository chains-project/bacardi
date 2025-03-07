package com.feedzai.commons.sql.abstraction.engine.impl.mysql;

import com.feedzai.commons.sql.abstraction.engine.handler.QueryExceptionHandler;

import java.sql.SQLException;

/**
 * A specific implementation of {@link QueryExceptionHandler} for MySQL engine.
 *
 * @author José Fidalgo (jose.fidalgo@feedzai.com)
 * @since 2.5.1
 */
public class MySqlQueryExceptionHandler extends QueryExceptionHandler {

    /**
     * The MySQL error code that indicates a unique constraint violation.
     */
    private static final int UNIQUE_CONSTRAINT_VIOLATION_ERROR_CODE = 1062;

    @Override
    public boolean isTimeoutException(final SQLException exception) {
        // Check if the SQL state code indicates a timeout exception
        return "08006".equals(exception.getSQLState()) || super.isTimeoutException(exception);
    }

    @Override
    public boolean isUniqueConstraintViolationException(final SQLException exception) {
        return UNIQUE_CONSTRAINT_VIOLATION_ERROR_CODE == exception.getErrorCode()
                || super.isUniqueConstraintViolationException(exception);
    }
}