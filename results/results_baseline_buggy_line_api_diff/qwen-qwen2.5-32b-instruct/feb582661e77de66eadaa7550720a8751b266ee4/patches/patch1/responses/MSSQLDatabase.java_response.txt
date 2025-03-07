package liquibase.ext.mssql.database;

import liquibase.CatalogAndSchema;
import liquibase.database.Database;
import liquibase.exception.LiquibaseException;
import liquibase.ext.mssql.statement.DropStoredProcedureStatement;
import liquibase.executor.Executor;
import liquibase.executor.ExecutorService;

public class MSSQLDatabase extends liquibase.database.core.MSSQLDatabase {

    @Override
    public int getPriority() {
        return PRIORITY_DATABASE;
    }

    @Override
    public void dropDatabaseObjects(CatalogAndSchema schemaToDrop) throws LiquibaseException {
        super.dropDatabaseObjects(schemaToDrop);
        Executor executor = ExecutorService.getInstance().getExecutor("default", this);
        executor.execute(new DropStoredProcedureStatement(this.getLiquibaseCatalogName(), this.getLiquibaseSchemaName()));
    }
}