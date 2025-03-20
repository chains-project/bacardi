package com.google.inject.struts2;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Module;
import com.google.inject.Provides;
import com.google.inject.servlet.GuiceServletContextListener;
import com.google.inject.servlet.ServletModule;
import java.util.Date;
import junit.framework.TestCase;
import org.apache.struts2.dispatcher.filter.StrutsPrepareAndExecuteFilter; // Updated import

/**
 * Test for Struts2Factory
 *
 * @author benmccann.com
 */
public class Struts2FactoryTest extends TestCase {

  private static final Date TODAY = new Date();

  public static class TestListener extends GuiceServletContextListener {

    private final Module module;

    public TestListener(Module module) {
      this.module = module;
    }

    protected Injector getInjector() {
      return Guice.createInjector(
          new Struts2GuicePluginModule(),
          new ServletModule() {
            protected void configureServlets() {
              // Struts 2 setup
              bind(StrutsPrepareAndExecuteFilter.class)
                  .in(com.google.inject.Singleton.class);
              filter("/*").through(StrutsPrepareAndExecuteFilter.class);
            }
          },
          module);
    }
  }

  public void testStruts2Factory() {
    Struts2Factory s2Factory = new Struts2Factory();
    TestListener testListener =
        new TestListener(
            new AbstractModule() {

              @Provides
              @SuppressWarnings("unused")
              Date provideDate() {
                return TODAY;
              }
            });
    assertEquals(TODAY, testListener.getInjector().getInstance(Date.class));
    assertEquals(TODAY, s2Factory.buildBean(Date.class, null));
  }
}