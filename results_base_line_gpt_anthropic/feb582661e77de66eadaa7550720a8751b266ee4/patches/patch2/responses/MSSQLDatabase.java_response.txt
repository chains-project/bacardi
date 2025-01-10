package liquibase.ext.mssql.database;

import liquibase.CatalogAndSchema;
import liquibase.exception.LiquibaseException;
import liquibase.executor.ExecutorService;
import liquibase.ext.mssql.statement.DropStoredProcedureStatement;
import liquibase.executor.Executor; // Assuming Executor is the new class to use.

// Check if the method getInstance() has been renamed or removed in the updated version.
// Change the call to ExecutorService.getInstance() according to the new API.
public class MSSQLDatabase extends liquibase.database.core.MSSQLDatabase {
    
    @Override
    public int getPriority() {
        return PRIORITY_DATABASE;
    }
    
    @Override
    public void dropDatabaseObjects(CatalogAndSchema schemaToDrop) throws LiquibaseException {
        super.dropDatabaseObjects(schemaToDrop);
        // Updated to use the correct method to get the executor instance.
        Executor executorService = ExecutorService.getExecutor(this); // Use the correct method to obtain the executor.
        executorService.execute(new DropStoredProcedureStatement(this.getLiquibaseCatalogName(), this.getLiquibaseSchemaName()));
    }
}