package liquibase.ext.mssql.database;

import liquibase.CatalogAndSchema;
import liquibase.exception.LiquibaseException;
import liquibase.executor.ExecutorService;
import liquibase.ext.mssql.statement.DropStoredProcedureStatement;
import liquibase.executor.Executor; // Ensure the correct import for Executor

public class MSSQLDatabase extends liquibase.database.core.MSSQLDatabase {
    
    @Override
    public int getPriority() {
        return PRIORITY_DATABASE;
    }
    
    @Override
    public void dropDatabaseObjects(CatalogAndSchema schemaToDrop) throws LiquibaseException {
        super.dropDatabaseObjects(schemaToDrop);
        Executor executor = ExecutorService.getInstance().getExecutor(this); // Create an executor variable
        executor.execute(new DropStoredProcedureStatement(this.getLiquibaseCatalogName(), this.getLiquibaseSchemaName()));
    }
}