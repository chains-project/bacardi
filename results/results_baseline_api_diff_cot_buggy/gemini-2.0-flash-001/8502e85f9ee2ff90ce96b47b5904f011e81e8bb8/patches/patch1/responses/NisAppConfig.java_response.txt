@Bean(initMethod = "migrate")
public Flyway flyway() throws IOException {
    final Properties prop = new Properties();
    prop.load(NisAppConfig.class.getClassLoader().getResourceAsStream("db.properties"));

    final org.flywaydb.core.Flyway flyway = new Flyway();
    flyway.setDataSource(this.dataSource());
    flyway.setClassLoader(NisAppConfig.class.getClassLoader());
    flyway.setLocations(prop.getProperty("flyway.locations"));
    flyway.setValidateOnMigrate(Boolean.valueOf(prop.getProperty("flyway.validate")));
    return flyway;
}