package org.nem.specific.deploy.appconfig;

import org.flywaydb.core.Flyway;
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
	public Flyway flyway() throws IOException {
		final Properties prop = new Properties();
		prop.load(NisAppConfig.class.getClassLoader().getResourceAsStream("db.properties"));

		final Flyway flyway = Flyway.configure()
				.dataSource(this.dataSource())
				.locations(prop.getProperty("flyway.locations"))
				.classLoader(NisAppConfig.class.getClassLoader())
				.validateOnMigrate(Boolean.valueOf(prop.getProperty("flyway.validate")))
				.load();

		return flyway;
	}

	@Bean
	public DataSource dataSource() throws IOException {
		final Properties prop = new Properties();
		prop.load(NisAppConfig.class.getClassLoader().getResourceAsStream("db.properties"));

		final DriverManagerDataSource dataSource = new DriverManagerDataSource();
		dataSource.setDriverClassName(prop.getProperty("jdbc.driverClassName"));
		dataSource.setUrl(prop.getProperty("jdbc.url"));
		dataSource.setUsername(prop.getProperty("jdbc.username"));
		dataSource.setPassword(prop.getProperty("jdbc.password"));
		return dataSource;
	}

	@Bean
	@Autowired
	public HibernateTransactionManager transactionManager() throws IOException {
		return new HibernateTransactionManager(this.sessionFactory());
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
		return new BlockChainServices(this.blockDao, this.blockChainUpdater(), this.blockChainLastBlockLayer, this.nisMapperFactory(),
				this.nisConfiguration().getForkConfiguration());
	}

	@Bean
	public BlockChainUpdater blockChainUpdater() {
		return new BlockChainUpdater(this.nisCache(), this.blockDao, this.blockChainContextFactory(), this.unconfirmedTransactionsFilter(),
				this.nisConfiguration().getForkConfiguration());
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
		return this.nisMapperFactory().createDbModelToModelNisMapper(this.accountCache());
	}

	// endregion

	@Bean
	public NisMain nisMain() {
		// initialize network info
		NetworkInfos.setDefault(this.nisConfiguration().getNetworkInfo());

		// initialize other globals
		NemGlobals.setBlockChainConfiguration(this.nisConfiguration().getBlockChainConfiguration());
		NemStateGlobals.setWeightedBalancesSupplier(this.weighedBalancesSupplier());

		return new NisMain(this.blockDao, this.nisCache(), this.networkHostBootstrapper(), this.nisModelToDbModelMapper(),
				this.nisConfiguration(), this.blockAnalyzer(), System::exit);
	}

	@SuppressWarnings("serial")
	private Supplier<WeightedBalances> weighedBalancesSupplier() {
		final Map<BlockChainFeature, Supplier<Supplier<WeightedBalances>>> featureSupplierMap = new HashMap<BlockChainFeature, Supplier<Supplier<WeightedBalances>>>() {
			{
				this.put(BlockChainFeature.WB_TIME_BASED_VESTING, () -> TimeBasedVestingWeightedBalances::new);
				this.put(BlockChainFeature.WB_IMMEDIATE_VESTING, () -> AlwaysVestedBalances::new);
			}
		};

		return BlockChainFeatureDependentFactory.createObject(this.nisConfiguration().getBlockChainConfiguration(),
				"weighted balance scheme", featureSupplierMap);
	}

	@Bean
	public BlockAnalyzer blockAnalyzer() {
		final int estimatedBlocksPerYear = this.nisConfiguration().getBlockChainConfiguration().getEstimatedBlocksPerYear();
		return new BlockAnalyzer(this.blockDao, this.blockChainUpdater(), this.blockChainLastBlockLayer, this.nisMapperFactory(),
				estimatedBlocksPerYear);
	}

	@Bean
	public HttpConnectorPool httpConnectorPool() {
		final CommunicationMode communicationMode = this.nisConfiguration().useBinaryTransport()
				? CommunicationMode.BINARY
				: CommunicationMode.JSON;
		return new HttpConnectorPool(communicationMode, this.outgoingAudits());
	}

	@Bean
	public NisPeerNetworkHost nisPeerNetworkHost() {
		final HarvestingTask harvestingTask = new HarvestingTask(this.blockChain(), this.harvester(), this.unconfirmedTransactions());

		final PeerNetworkScheduler scheduler = new PeerNetworkScheduler(this.timeProvider(), harvestingTask);

		final CountingBlockSynchronizer synchronizer = new CountingBlockSynchronizer(this.blockChain());

		return new NisPeerNetworkHost(this.nisCache(), synchronizer, scheduler, this.chainServices(), this.nodeCompatibilityChecker(),
				this.nisConfiguration(), this.httpConnectorPool(), this.trustProvider(), this.incomingAudits(), this.outgoingAudits());
	}

	@Bean
	public NetworkHostBootstrapper networkHostBootstrapper() {
		return new HarvestAwareNetworkHostBootstrapper(this.nisPeerNetworkHost(), this.unlockedAccounts(), this.nisConfiguration());
	}

	@Bean
	public NisConfiguration nisConfiguration() {
		return new NisConfiguration();
	}

	@Bean
	public TimeProvider timeProvider() {
		return CommonStarter.TIME_PROVIDER;
	}

	@Bean
	public TrustProvider trusProvider() {
		final int LOW_COMMUNICATION_NODE_WEIGHT = 30;
		final int TRUST_CACHE_TIME = 15 * 60;
		return new CachedTrustProvider(new LowComTrustProvider(new EigenTrustPlusPlus(), LOW_COMMUNICATION_NODE_WEIGHT), TRUST_CACHE_TIME,
				this.timeProvider());
	}

	@Bean
	public EnumSet<ObserverOption> observerOptions() {
		final EnumSet<ObserverOption> observerOptions = EnumSet.noneOf(ObserverOption.class);
		if (this.nisConfiguration().isFeatureSupported(NodeFeature.HISTORICAL_ACCOUNT_DATA)) {
			observerOptions.add(ObserverOption.NoHistoricalDataPruning);
		}

		final BlockChainConfiguration blockChainConfiguration = this.nisConfiguration().getBlockChainConfiguration();
		if (blockChainConfiguration.isBlockChainFeatureSupported(BlockChainFeature.PROOF_OF_STAKE)) {
			observerOptions.add(ObserverOption.NoOutlinkObserver);
		}

		return observerOptions;
	}

	@Bean
	public Function<Address, Collection<Address>> cosignatoryLookup() {
		return a -> this.accountStateCache().findStateByAddress(a).getMultisigLinks().getCosignatories();
	}

	@Bean
	public MosaicIdCache mosaicIdCache() {
		return new SynchronizedMosaicIdCache(new DefaultMosaicIdCache());
	}
}