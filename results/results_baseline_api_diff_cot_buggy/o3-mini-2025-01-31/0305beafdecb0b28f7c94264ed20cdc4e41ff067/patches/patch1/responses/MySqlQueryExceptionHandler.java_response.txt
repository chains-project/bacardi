package com.feedzai.commons.sql.abstraction.engine.impl.mysql;

import com.feedzai.commons.sql.abstraction.engine.handler.QueryExceptionHandler;
import com.mysql.cj.jdbc.exceptions.MySQLTimeoutException;

import java.sql.SQLException;

public class MySqlQueryExceptionHandler extends QueryExceptionHandler {

    private static final int UNIQUE_CONSTRAINT_VIOLATION_ERROR_CODE = 1062;

    @Override
    public boolean isTimeoutException(final SQLException exception) {
        return exception instanceof MySQLTimeoutException || super.isTimeoutException(exception);
    }

    @Override
    public boolean isUniqueConstraintViolationException(final SQLException exception) {
        return UNIQUE_CONSTRAINT_VIOLATION_ERROR_CODE == exception.getErrorCode()
                || super.isUniqueConstraintViolationException(exception);
    }
}
