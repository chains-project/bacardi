import com.mysql.jdbc.exceptions.MySQLTimeoutException;

  public class MySqlQueryExceptionHandler extends QueryExceptionHandler {
      @Override
      public boolean isTimeoutException(final SQLException exception) {
          return exception instanceof MySQLTimeoutException || super.isTimeoutException(exception);
      }
  }