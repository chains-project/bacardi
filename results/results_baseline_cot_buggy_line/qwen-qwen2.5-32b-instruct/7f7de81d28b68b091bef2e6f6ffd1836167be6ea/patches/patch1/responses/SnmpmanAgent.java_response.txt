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
import org.snmp4j.util.ThreadPool;

import java.io.*;
import java.lang.reflect.Field;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Slf4j
public class SnmpmanAgent extends BaseAgent {

    // ... (rest of the class remains unchanged)

    @Override
    protected void registerManagedObjects() {
        unregisterDefaultManagedObjects(null);
        unregisterDefaultManagedObjects(new OctetString());
        final List<Long> vlans = configuration.getDevice().getVlans();
        for (final Long vlan : vlans) {
            unregisterDefaultManagedObjects(new OctetString(String.valueOf(vlan)));
        }

        log.trace("registering managed objects for agent \"{}\"", configuration.getName());
        for (final Long vlan : vlans) {
            try (final FileInputStream fileInputStream = new FileInputStream(configuration.getWalk());
                 final BufferedReader reader = new BufferedReader(new InputStreamReader(fileInputStream, DEFAULT_CHARSET))) {

                Map<OID, Variable> bindings = readVariableBindings(reader);

                final SortedMap<OID, Variable> variableBindings = this.getVariableBindings(configuration.getDevice(), bindings, new OctetString(String.valueOf(vlan)));

                final OctetString context = new OctetString(String.valueOf(vlan));

                final List<OID> roots = SnmpmanAgent.getRoots(variableBindings);
                for (final OID root : roots) {
                    MOGroup group = createGroup(root, variableBindings);
                    final Iterable<VariableBinding> subtree = generateSubtreeBindings(variableBindings, root);
                    DefaultMOContextScope scope = new DefaultMOContextScope(context, root, true, root.nextPeer(), false);
                    ManagedObject mo = server.lookup(new DefaultMOQuery(scope, false));
                    if (mo != null) {
                        for (final VariableBinding variableBinding : subtree) {
                            group = new MOGroup(variableBinding.getOid(), variableBinding.getOid(), variableBinding.getVariable());
                            scope = new DefaultMOContextScope(context, variableBinding.getOid(), true, variableBinding.getOid().nextPeer(), false);
                            mo = server.lookup(new DefaultMOQuery(scope, false));
                            if (mo != null) {
                                log.warn("could not register single OID at {} because ManagedObject {} is already registered.", variableBinding.getOid(), mo);
                            } else {
                                groups.add(group);
                                registerGroupAndContext(group, context);
                            }
                        }
                    } else {
                        groups.add(group);
                        registerGroupAndContext(group, context);
                    }
                }
            } catch (final FileNotFoundException e) {
                log.error("walk file {} not found", configuration.getWalk().getAbsolutePath());
            } catch (final IOException e) {
                log.error("could not read walk file " + configuration.getWalk().getAbsolutePath(), e);
            }
        }
        createAndRegisterDefaultContext();
    }

    // ... (rest of the class remains unchanged)

    @Override
    protected void unregisterManagedObjects() {
        unregisterDefaultManagedObjects(null);
        unregisterDefaultManagedObjects(new OctetString());
        final List<Long> vlans = configuration.getDevice().getVlans();
        for (final Long vlan : vlans) {
            unregisterDefaultManagedObjects(new OctetString(String.valueOf(vlan)));
        }
    }

    // ... (rest of the class remains unchanged)

    private void registerHard(final MOGroup group) {
        try {
            final Field registry = server.getClass().getDeclaredField("registry");
            registry.setAccessible(true);
            final SortedMap<MOScope, ManagedObject<?>> reg = server.getRegistry();
            DefaultMOContextScope contextScope = new DefaultMOContextScope(new OctetString(), group.getScope());
            ManagedObject other = server.lookup(new DefaultMOQuery(contextScope, false));
            if (other != null) {
                log.warn("group {} already existed", group);
                return;
            }

            contextScope = new DefaultMOContextScope(null, group.getScope());
            other = server.lookup(new DefaultMOQuery(contextScope, false);
            if (other != null) {
                registerHard(group);
                return;
            }
            this.server.register(group, new OctetString());
        } catch (NoSuchFieldException | IllegalAccessException e) {
            log.warn("could not set server registry", e);
        }
    }

    // ... (rest of the class remains unchanged)
}