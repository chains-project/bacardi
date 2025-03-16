package org.nem.specific.deploy.appconfig;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.configuration.ClassicConfiguration;
import org.hibernate.SessionFactory;
import org.nem.core.model.*;
import org.nem.core.model.primitive.*;
import org.nem.core.node.NodeFeature;
import org.nem.core.time.TimeProvider;
import org.nis.deploy.*;
import org.nis.nis.*;
import org.nis.nis.audit.AuditCollection;
import org.nis.nis.boot.*;
import org.nis.nis.cache.*;
import org.nis.nis.connect.*;
import org.nis.nis.controller.interceptors.LocalHostDetector;
import org.nis.nis.dao.*;
import org.nis.nis.harvesting.*;
import org.nis.nis.mappers.*;
import org.nis.nis.pox.ImportanceCalculator;
import org.nis.nis.pox.poi.*;
import org.nis.nis.pox.pos.PosImportanceCalculator;
import org.nis.nis.secret.*;
import org.nis.nis.service.BlockChainLastBlockLayer;
import org.nis.nis.state.*;
import org.nis.nis.sync.*;
import org.nis.nis.validators.*;
import org.nis.peer.connect.CommunicationMode;
import org.nis.peer.node.*;
import org.nis.peer.services.ChainServices;
import org.nis.peer.trust.*;
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
		"org.nis.nis"
}, excludeFilters = {
		@ComponentScan.Filter(type = FilterType.ANNOTATION, value = org.springframework.stereotype.Controller.class),
		@ComponentScan.Filter(type = FilterType.REGEX, pattern = {
				"org.nis.nis.websocket.*"
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

	@Bean
	public Flyway flyway() throws IOException {
		final Properties prop = new Properties();
		prop.load(NisAppConfig.class.getClassLoader().getResourceAsStream("db.properties"));

		final ClassicConfiguration configuration = new ClassicConfiguration();
		configuration.setDataSource(this.dataSource());
		configuration.setLocations(prop.getProperty("flyway.locations").split(","));
		configuration.setValidateOnMigrate(Boolean.valueOf(prop.getProperty("flyway.validate")));
		configuration.setClassLoader(NisAppConfig.class.getClassLoader());

		return new Flyway(configuration);
	}

	@Bean
	@DependsOn("flyway")
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

	// region observers + validators

	@Bean
	public BlockTransactionObserverFactory blockTransactionObserverFactory() {
		final int estimatedBlocksPerYear = this.nisConfiguration().getBlockChainConfiguration().getEstimatedBlocksPerYear();
		return new BlockTransactionObserverFactory(this.observerOptions(), estimatedBlocksPerYear);
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

	// region harvester

	@Bean
	public Harvester harvester() {
		final NewBlockTransactionsProvider transactionsProvider = new DefaultNewBlockTransactionsProvider(this.nisCache(),
				this.transactionValidatorFactory(), this.blockValidatorFactory(), this.blockTransactionObserverFactory(),
				this.unconfirmedTransactionsFilter(), this.nisConfiguration().getForkConfiguration());

		final BlockGenerator generator = new BlockGenerator(this.nisCache(), transactionsProvider, this.blockDao,
				new BlockScorer(this.accountStateCache()), this.blockValidatorFactory().create(this.nisCache()));
		return new Harvester(this.timeProvider(), this.blockChainLastBlockLayer, this.unlockedAccounts(), this.nisDbModelToModelMapper(),
				generator);
	}

	@Bean
	public SynchronizedAccountCache accountCache() {
		return new SynchronizedAccountCache(new DefaultAccountCache());
	}

	@Bean
	public SynchronizedAccountStateCache accountStateCache() {
		return new SynchronizedAccountStateCache(new DefaultAccountStateCache());
	}

	@Bean
	public SynchronizedHashCache transactionHashCache() {
		return new SynchronizedHashCache(new DefaultHashCache(50000, this.nisConfiguration().getTransactionHashRetentionTime()));
	}

	@Bean
	public SynchronizedPoxFacade poxFacade() {
		return new SynchronizedPoxFacade(new DefaultPoxFacade(this.importanceCalculator()));
	}

	@Bean
	public SynchronizedNamespaceCache namespaceCache() {
		return new SynchronizedNamespaceCache(new DefaultNamespaceCache());
	}

	@Bean
	public ReadOnlyNisCache nisCache() {
		return new DefaultNisCache(this.accountCache(), this.accountStateCache(), this.poxFacade(), this.transactionHashCache(),
				this.namespaceCache());
	}

	@Bean
	@SuppressWarnings("serial")
	public ImportanceCalculator importanceCalculator() {
		final Map<BlockChainFeature, Supplier<ImportanceCalculator>> featureSupplierMap = new HashMap<BlockChainFeature, Supplier<ImportanceCalculator>>() {
			{
				this.put(BlockChainFeature.PROOF_OF_IMPORTANCE,
						() -> new PoiImportanceCalculator(new PoiScorer(), NisAppConfig::getBlockDependentPoiOptions));
				this.put(BlockChainFeature.PROOF_OF_STAKE, PosImportanceCalculator::new);
			}
		};

		return BlockChainFeatureDependentFactory.createObject(this.nisConfiguration().getBlockChainConfiguration(), "consensus algorithm",
				featureSupplierMap);
	}

	@Bean
	public UnlockedAccounts unlockedAccounts() {
		return new UnlockedAccounts(this.accountCache(), this.accountStateCache(), this.blockChainLastBlockLayer,
				this.canHarvestPredicate(), this.nisConfiguration().getUnlockedLimit());
	}

	@Bean
	public CanHarvestPredicate canHarvestPredicate() {
		return new CanHarvestPredicate(this::getBlockDependentMinHarvesterBalance);
	}

	private Amount getBlockDependentMinHarvesterBalance(final BlockHeight height) {
		return getBlockDependentPoiOptions(height).getMinHarvesterBalance();
	}

	private static org.nis.nis.pox.poi.PoiOptions getBlockDependentPoiOptions(final BlockHeight height) {
		return new PoiOptionsBuilder(height).create();
	}

	@Bean
	public Supplier<BlockHeight> lastBlockHeight() {
		return this.blockChainLastBlockLayer::getLastBlockHeight;
	}

	@Bean
	public UnconfirmedTransactions unconfirmedTransactions() {
		final BlockChainConfiguration blockChainConfiguration = this.nisConfiguration().getBlockChainConfiguration();
		final UnconfirmedStateFactory unconfirmedStateFactory = new UnconfirmedStateFactory(this.transactionValidatorFactory(),
				this.blockTransactionObserverFactory()::createExecuteCommitObserver, this.timeProvider(), this.lastBlockHeight(),
				blockChainConfiguration.getMaxTransactionsPerBlock(), this.nisConfiguration().getForkConfiguration());
		final UnconfirmedTransactions unconfirmedTransactions = new DefaultUnconfirmedTransactions(unconfirmedStateFactory,
				this.nisCache());
		return new SynchronizedUnconfirmedTransactions(unconfirmedTransactions);
	}

	@Bean
	public UnconfirmedTransactionsFilter unconfirmedTransactionsFilter() {
		return this.unconfirmedTransactions().asFilter();
	}

	@Bean
	public HibernateTransactionManager transactionManager() throws IOException {
		return new HibernateTransactionManager(this.sessionFactory());
	}

	@Bean
	public NisMain nisMain() {
		// initialize network info
		NetworkInfos.setDefault(this.nisConfiguration().getNetworkInfo());

		// initialize other globals
		final NamespaceCacheLookupAdapters adapters = new NamespaceCacheLookupAdapters(this.namespaceCache());
		if (this.nisConfiguration().ignoreFees()) {
			NemGlobals.setTransactionFeeCalculator(new ZeroTransactionFeeCalculator());
		} else {
			NemGlobals.setTransactionFeeCalculator(new DefaultTransactionFeeCalculator(adapters.asMosaicFeeInformationLookup(),
					() -> this.blockChainLastBlockLayer.getLastBlockHeight().next(), new BlockHeight[]{
							new BlockHeight(BlockMarkerConstants.FEE_FORK(this.nisConfiguration().getNetworkInfo().getVersion() << 24)),
							new BlockHeight(
									BlockMarkerConstants.SECOND_FEE_FORK(this.nisConfiguration().getNetworkInfo().getVersion() << 24))
					}));
		}

		NemGlobals.setBlockChainConfiguration(this.nisConfiguration().getBlockChainConfiguration());
		NemStateGlobals.setWeightedBalancesSupplier(this.weighedBalancesSupplier());

		return new NisMain(this.blockDao, this.nisCache(), this.networkHostBootstrapper(), this.nisModelToDbModelMapper(),
				this.nisConfiguration(), this.blockAnalyzer(), System::exit);
	}

	@SuppressWarnings("serial")
	private Supplier<WeightedBalances> weighedBalancesSupplier() {
		final Map<BlockChainFeature, Supplier<Supplier<WeightedBalances>>> featureSupplierMap = new HashMap<BlockChainFeature, Supplier<Supplier<WeightedBalances>>>() {
			{
				this.put(BlockChainFeature.WB_TIME_BASED_VESTINGING, () -> TimeBasedVestingWeightedBalances::new);
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
	public TrustProvider trustProvider() {
		final int LOW_COMMUNICATION_NODE_WEIGHT = 30;
		final int TRUST_CACHE_TIME = 15 * 60;
		return new CachedTrustProvider(new LowComTrustProvider(new EigenTrustPlusPlus(), LOW_COMMUNICATION_NODE_WEIGHT), TRUST_CACHE_TIME,
				this.timeProvider());
	}

	@Bean
	public NemConfigurationPolicy configurationPolicy() {
		return new NisConfigurationPolicy();
	}

	@Bean
	public ChainServices chainServices() {
		return new DefaultChainServices(this.blockChainLastBlockLayer, this.httpConnectorPool());
	}

	@Bean
	public CommonStarter commonStarter() {
		return CommonStarter.INSTANCE;
	}

	@Bean
	public ValidationState validationState() {
		return NisCacheUtils.createValidationState(this.nisCache());
	}

	@Bean
	public LocalHostDetector localHostDetector() {
		return new LocalHostDetector(this.nisConfiguration().getAdditionalLocalIps());
	}

	@Bean
	public NodeCompatibilityChecker nodeCompatibilityChecker() {
		return new DefaultNodeCompatibilityChecker();
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
