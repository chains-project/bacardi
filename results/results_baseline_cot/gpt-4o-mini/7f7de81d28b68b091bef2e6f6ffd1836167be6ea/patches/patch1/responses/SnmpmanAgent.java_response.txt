package com.oneandone.snmpman;

import com.google.common.primitives.UnsignedLong;
import com.oneandone.snmpman.configuration.AgentConfiguration;
import com.oneandone.snmpman.configuration.Device;
import com.oneandone.snmpman.configuration.modifier.CommunityContextModifier;
import com.oneandone.snmpman.configuration.modifier.ModifiedVariable;
import com.oneandone.snmpman.configuration.modifier.Modifier;
import com.oneandone.snmpman.configuration.modifier.VariableModifier;
import com.oneandone.snmpman.snmp.MOGroup;
import lombok.extern.slf4j.Slf4j;
import org.snmp4j.TransportMapping;
import org.snmp4j.agent.*;
import org.snmp4j.agent.io.ImportMode;
import org.snmp4j.agent.mo.ext.StaticMOGroup;
import org.snmp4j.agent.mo.snmp.*;
import org.snmp4j.agent.security.MutableVACM;
import org.snmp4j.mp.MPv3;
import org.snmp4j.security.SecurityLevel;
import org.snmp4j.security.SecurityModel;
import org.snmp4j.security.USM;
import org.snmp4j.smi.*;
import org.snmp4j.transport.TransportMappings;
import org.snmp4j.util.ThreadPool;

import java.io.*;
import java.lang.reflect.Field;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * This is the core class of the {@code Snmpman}. The agent simulates the SNMP-capable devices.
 * <br>
 * This class can be instantiated via the constructor {@link #SnmpmanAgent(com.oneandone.snmpman.configuration.AgentConfiguration)}, which
 * requires an instance of the {@link com.oneandone.snmpman.configuration.AgentConfiguration}.
 */
@Slf4j
public class SnmpmanAgent extends BaseAgent {

    /**
     * The default charset for files being read.
     */
    private static final Charset DEFAULT_CHARSET = Charset.forName("UTF-8");

    /**
     * The pattern of variable bindings in a walk file.
     */
    private static final Pattern VARIABLE_BINDING_PATTERN = Pattern.compile("(((iso)?\\.[0-9]+)+) = ((([a-zA-Z0-9-]+): (.*)$)|(\"\"$))");

    /**
     * The configuration of this agent.
     */
    private final AgentConfiguration configuration;

    /**
     * The list of managed object groups.
     */
    private final List<ManagedObject> groups = new ArrayList<>();

    /**
     * Initializes a new instance of an SNMP agent.
     *
     * @param configuration the configuration for this agent
     */
    public SnmpmanAgent(final AgentConfiguration configuration) {
        super(SnmpmanAgent.getBootCounterFile(configuration), SnmpmanAgent.getConfigurationFile(configuration), new CommandProcessor(new OctetString(MPv3.createLocalEngineID())));
        this.agent.setWorkerPool(ThreadPool.create("RequestPool", 3));
        this.configuration = configuration;
    }

    // ... (rest of the class remains unchanged)

    @Override
    protected void unregisterManagedObjects() {
        log.trace("unregistered managed objects for agent \"{}\"", agent);
        for (final ManagedObject mo : groups) {
            server.unregister(mo, null);
        }
    }

    @Override
    protected void addUsmUser(final USM usm) {
        log.trace("adding usm user {} for agent \"{}\"", usm.toString(), configuration.getName());
        // do nothing here
    }

    @Override
    protected void addNotificationTargets(final SnmpTargetMIB snmpTargetMIB, final SnmpNotificationMIB snmpNotificationMIB) {
        log.trace("adding notification targets {}, {} for agent \"{}\"", snmpTargetMIB.toString(), snmpNotificationMIB.toString(), configuration.getName());
        // do nothing here
    }

    @Override
    protected void addViews(final VacmMIB vacmMIB) {
        log.trace("adding views in the vacm MIB {} for agent \"{}\"", vacmMIB.toString(), configuration.getName());
        vacmMIB.addGroup(SecurityModel.SECURITY_MODEL_SNMPv1, new OctetString(configuration.getCommunity()), new OctetString("v1v2group"), StorageType.nonVolatile);
        vacmMIB.addGroup(SecurityModel.SECURITY_MODEL_SNMPv2c, new OctetString(configuration.getCommunity()), new OctetString("v1v2group"), StorageType.nonVolatile);
        vacmMIB.addGroup(SecurityModel.SECURITY_MODEL_USM, new OctetString("SHADES"), new OctetString("v3group"), StorageType.nonVolatile);
        vacmMIB.addGroup(SecurityModel.SECURITY_MODEL_USM, new OctetString("TEST"), new OctetString("v3test"), StorageType.nonVolatile);
        vacmMIB.addGroup(SecurityModel.SECURITY_MODEL_USM, new OctetString("SHA"), new OctetString("v3restricted"), StorageType.nonVolatile);
        vacmMIB.addGroup(SecurityModel.SECURITY_MODEL_USM, new OctetString("v3notify"), new OctetString("v3restricted"), StorageType.nonVolatile);

        // configure community index contexts
        for (final Long vlan : configuration.getDevice().getVlans()) {
            vacmMIB.addGroup(SecurityModel.SECURITY_MODEL_SNMPv1, new OctetString(configuration.getCommunity() + "@" + vlan), new OctetString("v1v2group"), StorageType.nonVolatile);
            vacmMIB.addGroup(SecurityModel.SECURITY_MODEL_SNMPv2c, new OctetString(configuration.getCommunity() + "@" + vlan), new OctetString("v1v2group"), StorageType.nonVolatile);
            vacmMIB.addAccess(new OctetString("v1v2group"), new OctetString(String.valueOf(vlan)), SecurityModel.SECURITY_MODEL_ANY, SecurityLevel.NOAUTH_NOPRIV, MutableVACM.VACM_MATCH_EXACT, new OctetString("fullReadView"), new OctetString("fullWriteView"), new OctetString("fullNotifyView"), StorageType.nonVolatile);
        }

        vacmMIB.addAccess(new OctetString("v1v2group"), new OctetString(), SecurityModel.SECURITY_MODEL_ANY, SecurityLevel.NOAUTH_NOPRIV, MutableVACM.VACM_MATCH_EXACT, new OctetString("fullReadView"), new OctetString("fullWriteView"), new OctetString("fullNotifyView"), StorageType.nonVolatile);
        vacmMIB.addAccess(new OctetString("v3group"), new OctetString(), SecurityModel.SECURITY_MODEL_USM, SecurityLevel.AUTH_PRIV, MutableVACM.VACM_MATCH_EXACT, new OctetString("fullReadView"), new OctetString("fullWriteView"), new OctetString("fullNotifyView"), StorageType.nonVolatile);
        vacmMIB.addAccess(new OctetString("v3restricted"), new OctetString(), SecurityModel.SECURITY_MODEL_USM, SecurityLevel.NOAUTH_NOPRIV, MutableVACM.VACM_MATCH_EXACT, new OctetString("restrictedReadView"), new OctetString("restrictedWriteView"), new OctetString("restrictedNotifyView"), StorageType.nonVolatile);
        vacmMIB.addAccess(new OctetString("v3test"), new OctetString(), SecurityModel.SECURITY_MODEL_USM, SecurityLevel.AUTH_PRIV, MutableVACM.VACM_MATCH_EXACT, new OctetString("testReadView"), new OctetString("testWriteView"), new OctetString("testNotifyView"), StorageType.nonVolatile);

        vacmMIB.addViewTreeFamily(new OctetString("fullReadView"), new OID("1"), new OctetString(), VacmMIB.vacmViewIncluded, StorageType.nonVolatile);
        vacmMIB.addViewTreeFamily(new OctetString("fullWriteView"), new OID("1"), new OctetString(), VacmMIB.vacmViewIncluded, StorageType.nonVolatile);
        vacmMIB.addViewTreeFamily(new OctetString("fullNotifyView"), new OID("1"), new OctetString(), VacmMIB.vacmViewIncluded, StorageType.nonVolatile);

        vacmMIB.addViewTreeFamily(new OctetString("restrictedReadView"), new OID("1.3.6.1.2"), new OctetString(), VacmMIB.vacmViewIncluded, StorageType.nonVolatile);
        vacmMIB.addViewTreeFamily(new OctetString("restrictedWriteView"), new OID("1.3.6.1.2.1"), new OctetString(), VacmMIB.vacmViewIncluded, StorageType.nonVolatile);
        vacmMIB.addViewTreeFamily(new OctetString("restrictedNotifyView"), new OID("1.3.6.1.2"), new OctetString(), VacmMIB.vacmViewIncluded, StorageType.nonVolatile);
        vacmMIB.addViewTreeFamily(new OctetString("restrictedNotifyView"), new OID("1.3.6.1.6.3.1"), new OctetString(), VacmMIB.vacmViewIncluded, StorageType.nonVolatile);

        vacmMIB.addViewTreeFamily(new OctetString("testReadView"), new OID("1.3.6.1.2"), new OctetString(), VacmMIB.vacmViewIncluded, StorageType.nonVolatile);
        vacmMIB.addViewTreeFamily(new OctetString("testReadView"), new OID("1.3.6.1.2.1.1"), new OctetString(), VacmMIB.vacmViewExcluded, StorageType.nonVolatile);
        vacmMIB.addViewTreeFamily(new OctetString("testWriteView"), new OID("1.3.6.1.2.1"), new OctetString(), VacmMIB.vacmViewIncluded, StorageType.nonVolatile);
        vacmMIB.addViewTreeFamily(new OctetString("testNotifyView"), new OID("1.3.6.1.2"), new OctetString(), VacmMIB.vacmViewIncluded, StorageType.nonVolatile);
    }

    @Override
    protected void addCommunities(final SnmpCommunityMIB snmpCommunityMIB) {
        log.trace("adding communities {} for agent \"{}\"", snmpCommunityMIB.toString(), configuration.getName());
        // configure community index contexts
        for (final Long vlan : configuration.getDevice().getVlans()) {
            configureSnmpCommunity(snmpCommunityMIB, vlan);
        }
        configureSnmpCommunity(snmpCommunityMIB, null);
    }

    /**
     * Configures an SNMP community for a given SNMP community context.
     *
     * @param snmpCommunityMIB SNMP community.
     * @param context          SNMP community context.
     */
    private void configureSnmpCommunity(final SnmpCommunityMIB snmpCommunityMIB, final Long context) {
        String communityString;
        OctetString contextName;
        if (context != null) {
            communityString = configuration.getCommunity() + "@" + context;
            contextName = new OctetString(String.valueOf(context));
        } else {
            communityString = configuration.getCommunity();
            contextName = new OctetString();
        }
        final Variable[] com2sec = new Variable[]{
                new OctetString(communityString),       // community name
                new OctetString(communityString),       // security name
                getAgent().getContextEngineID(),        // local engine ID
                contextName,                            // default context name
                new OctetString(),                      // transport tag
                new Integer32(StorageType.readOnly),    // storage type
                new Integer32(RowStatus.active)         // row status
        };
        final SnmpCommunityMIB.SnmpCommunityEntryRow row = snmpCommunityMIB.getSnmpCommunityEntry().createRow(
                new OctetString(communityString + "2" + communityString).toSubIndex(true), com2sec);
        snmpCommunityMIB.getSnmpCommunityEntry().addRow(row);
    }
}