import javax.servlet.DispatcherType;
import javax.servlet.FilterRegistration;
...
final FilterRegistration.Dynamic filterRegistration = environment
                .servlets().addFilter(filter.getClass().getName(), filter);
...
filterRegistration.addMappingForUrlPatterns(
                EnumSet.of(DispatcherType.REQUEST), true, mapping);