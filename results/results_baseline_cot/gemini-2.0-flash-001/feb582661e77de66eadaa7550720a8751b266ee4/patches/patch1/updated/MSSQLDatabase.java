package liquibase.ext.mssql.database;

import liquibase.CatalogAndSchema;
import liquibase.Scope;
import liquibase.exception.LiquibaseException;
import liquibase.executor.Executor;
import liquibase.ext.mssql.statement.DropStoredProcedureStatement;

public class MSSQLDatabase extends liquibase.database.core.MSSQLDatabase {

    @Override
    public int getPriority() {
        return PRIORITY_DATABASE;
    }

    @Override
    public void dropDatabaseObjects(CatalogAndSchema schemaToDrop) throws LiquibaseException {
        super.dropDatabaseObjects(schemaToDrop);
        Executor executor = Scope.getCurrentScope().getSingleton(Executor.class);
        executor.execute(new DropStoredProcedureStatement(this.getLiquibaseCatalogName(), this.getLiquibaseSchemaName()));
    }
}