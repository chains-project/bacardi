package org.nem.specific.deploy.appconfig;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.configuration.FlywayConfiguration;
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

		final FlywayConfiguration configuration = FlywayConfiguration.defaultConfiguration();
		configuration.setDataSource(this.dataSource());
		configuration.setLocations(prop.getProperty("flyway.locations"));
		configuration.setValidateOnMigrate(Boolean.valueOf(prop.getProperty("flyway.validate")));
		configuration.setClassLoader(NisAppConfig.class.getClassLoader());

		final Flyway flyway = new Flyway(configuration);
		return flyway;
	}

	@Bean
	public SessionFactory sessionFactory() throws IOException {
		return SessionFactoryLoader.load(this.dataSource());
	}

	@Bean
	public BlockChain blockChain() {
		return new BlockChain(this.blockChainLastBlockLayer, this.blockChainUpdater());
	}

	@Bean
	public BlockChainServices blockChainServices() {
		return new BlockChainServices(this.blockDao, this.blockTransactionObserverFactory(), this.blockValidatorFactory(),
				this.transactionValidatorFactory(), this.nisMapperFactory(), this.nisConfiguration().getForkConfiguration());
	}

	@Bean
	public BlockChainUpdater blockChainUpdater() {
		return new BlockChainUpdater(this.nisCache(), this.blockChainLastBlockLayer, this.blockDao, this.blockChainContextFactory(),
				this.unconfirmedTransactions(), this.nisConfiguration());
	}

	@Bean
	public BlockChainContextFactory blockChainContextFactory() {
		return new BlockChainContextFactory(this.nisCache(), this.blockChainLastBlockLayer, this.blockDao, this.blockChainServices(),
				this.unconfirmedTransactions());
	}

	// region mappers

	@Bean
	public MapperFactory mapperFactory() {
		return new DefaultMapperFactory(this.mosaicIdCache());
	}

	@Bean
	public NisMapperFactory nisMapperFactory() {
		return new NisMapperFactory(this.mapperFactory());
	}

	@Bean
	public NisModelToDbModelMapper nisModelToDbModelMapper() {
		return new NisModelToDbModelMapper(this.mapperFactory().createModelToDbModelMapper(new AccountDaoLookupAdapter(this.accountDao)));
	}

	@Bean
	public NisDbModelToModelMapper nisDbModelToModelMapper() {
		return this.nisMapperFactory().createDbModelToModelNisMapper(this.accountCache());
	}

	// endregion

	// region mappers

	@Bean
	public BlockTransactionObserverFactory blockTransactionObserverFactory() {
		final int estimatedBlocksPerYear = this.nisConfiguration().getBlockChainConfiguration().getEstimatedBlocksPerYear();
		final ForkConfiguration forkConfiguration = this.nisConfiguration().getForkConfiguration();
		return new BlockTransactionObserverFactory(this.observerOptions(), estimatedBlocksPerYear, forkConfiguration);
	}

	@Bean
	public BlockValidatorFactory blockValidatorFactory() {
		return new BlockValidatorFactory(this.timeProvider(), this.nisConfiguration().getForkConfiguration());
	}

	@Bean
	public TransactionValidatorFactory transactionValidatorFactory() {
		return new TransactionValidatorFactory(this.timeProvider(), this.nisConfiguration().getNetworkInfo(),
				this.nisConfiguration().getForkConfiguration(), this.nisConfiguration().ignoreFees());
	}

	@Bean
	public SingleTransactionValidator transactionValidator() {
		// this is only consumed by the TransactionController and used in transaction/prepare,
		// which should propagate incomplete transactions
		return this.transactionValidatorFactory().createIncompleteSingleBuilder(this.nisCache()).build();
	}

	// endregion

	// ... (rest of the class remains unchanged)
}