package org.nem.specific.deploy.appconfig;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.configuration.Configuration;
import org.hibernate.SessionFactory;
import org.nem.core.model.*;
import org.nem.core.model.primitive.*;
import org.nem.core.node.NodeFeature;
import org.nem.core.time.TimeProvider;
import org.nem.deploy.*;
import org.nem.nis.*;
import org.nem.nis.audit.AuditCollection;
import org.nem.nis.boot.*;
import org.nem.nis.cache.*;
import org.nem.nis.connect.*;
import org.nem.nis.controller.interceptors.LocalHostDetector;
import org.nem.nis.dao.*;
import org.nem.nis.harvesting.*;
import org.nem.nis.mappers.*;
import org.nem.nis.pox.ImportanceCalculator;
import org.nem.nis.pox.poi.*;
import org.nem.nis.pox.pos.PosImportanceCalculator;
import org.nem.nis.secret.*;
import org.nem.nis.service.BlockChainLastBlockLayer;
import org.nem.nis.state.*;
import org.nem.nis.sync.*;
import org.nem.nis.validators.*;
import org.nem.peer.connect.CommunicationMode;
import org.nem.peer.node.*;
import org.nem.peer.services.ChainServices;
import org.nem.peer.trust.*;
import org.nem.specific.deploy.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.*;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.orm.hibernate4.HibernateTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import javax.sql.DataSource;
import java.io.IOException;
import java.util.*;
import java.util.function.*;

@Configuration
@ComponentScan(basePackages = {
        "org.nem.nis"
}, excludeFilters = {
        @ComponentScan.Filter(type = FilterType.ANNOTATION, value = org.springframework.stereotype.Controller.class),
        @ComponentScan.Filter(type = FilterType.REGEX, pattern = {
                "org.nem.nis.websocket.*"
        })
})
@EnableTransactionManagement
public class NisAppConfig {

    @Autowired
    private AccountDao accountDao;

    @Autowired
    private BlockDao blockDao;

    @Autowired
    private BlockChainLastBlockLayer blockChainLastBlockLayer;

    @Autowired
    @SuppressWarnings("unused")
    private TransferDao transferDao;

    private static final int MAX_AUDIT_HISTORY_SIZE = 50;

    @Bean
    protected AuditCollection outgoingAudits() {
        return this.createAuditCollection();
    }

    @Bean
    protected AuditCollection incomingAudits() {
        return this.createAuditCollection();
    }

    private AuditCollection createAuditCollection() {
        return new AuditCollection(MAX_AUDIT_HISTORY_SIZE, this.timeProvider());
    }

    @Bean
    public DataSource dataSource() throws IOException {
        final NisConfiguration configuration = this.nisConfiguration();
        final String nemFolder = configuration.getNemFolder();
        final Properties prop = new Properties();
        prop.load(NisAppConfig.class.getClassLoader().getResourceAsStream("db.properties"));

        // replace url parameters with values from configuration
        final String jdbcUrl = prop.getProperty("jdbc.url").replace("${nem.folder}", nemFolder).replace("${nem.network}",
                configuration.getNetworkName());

        final DriverManagerDataSource dataSource = new DriverManagerDataSource();
        dataSource.setDriverClassName(prop.getProperty("jdbc.driverClassName"));
        dataSource.setUrl(jdbcUrl);
        dataSource.setUsername(prop.getProperty("jdbc.username"));
        dataSource.setPassword(prop.getProperty("jdbc.password"));
        return dataSource;
    }

    @Bean(initMethod = "migrate")
    public Flyway flyway() throws IOException {
        final Properties prop = new Properties();
        prop.load(NisAppConfig.class.getClassLoader().getResourceAsStream("db.properties"));

        // Create a Flyway configuration instance
        Configuration configuration = Flyway.configure()
                .dataSource(this.dataSource())
                .locations(prop.getProperty("flyway.locations"))
                .validateOnMigrate(Boolean.valueOf(prop.getProperty("flyway.validate")));

        // Return a Flyway instance with the configuration
        return new Flyway(configuration);
    }

    @Bean
    @DependsOn("flyway")
    public SessionFactory sessionFactory() throws IOException {
        return SessionFactoryLoader.load(this.dataSource());
    }

    // Remaining beans and methods unchanged...

    // (The rest of the class remains the same)
}