// Previous package and imports remain unchanged...

public class SnmpmanAgent extends BaseAgent {

    // Previous fields and methods remain unchanged...

    /**
     * Sets the private registry value of {@link DefaultMOServer} via reflection.
     * FIXME
     * If there is any possibility to avoid this, then replace!
     *
     * @param group {@link ManagedObject} to register.
     */
    private void registerHard(final MOGroup group) {
        try {
            final Field registry = server.getClass().getDeclaredField("registry");
            registry.setAccessible(true);
            @SuppressWarnings("unchecked")
            final SortedMap<MOScope, ManagedObject<?>> reg = (SortedMap<MOScope, ManagedObject<?>>) server.getRegistry();
            DefaultMOContextScope contextScope = new DefaultMOContextScope(new OctetString(""), group.getScope());
            reg.put(contextScope, group);
            registry.set(server, reg);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            log.warn("could not set server registry", e);
        }
    }

    // Rest of the class remains unchanged...
}