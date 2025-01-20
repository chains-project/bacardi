package liquibase.ext.mssql.database;

import liquibase.CatalogAndSchema;
import liquibase.exception.LiquibaseException;
import liquibase.executor.ExecutorService;
import liquibase.ext.mssql.statement.DropStoredProcedureStatement;
import liquibase.executor.Executor; // Import the Executor class

public class MSSQLDatabase extends liquibase.database.core.MSSQLDatabase {
    
    @Override
    public int getPriority() {
        return PRIORITY_DATABASE;
    }
    
    @Override
    public void dropDatabaseObjects(CatalogAndSchema schemaToDrop) throws LiquibaseException {
        super.dropDatabaseObjects(schemaToDrop);
        
        // Get the executor instance
        Executor executor = ExecutorService.getInstance().getExecutor(this);
        
        // Execute the DropStoredProcedureStatement
        executor.execute(new DropStoredProcedureStatement(this.getLiquibaseCatalogName(), this.getLiquibaseSchemaName()));
    }
}