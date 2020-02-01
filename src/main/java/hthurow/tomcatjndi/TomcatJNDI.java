package hthurow.tomcatjndi;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.Map;
import java.util.Objects;

import javax.naming.Context;

import org.apache.catalina.Lifecycle;
import org.apache.catalina.LifecycleEvent;
import org.apache.catalina.Server;
import org.apache.catalina.core.StandardContext;
import org.apache.catalina.core.StandardEngine;
import org.apache.catalina.core.StandardHost;
import org.apache.catalina.core.StandardServer;
import org.apache.catalina.core.StandardService;
import org.apache.catalina.deploy.NamingResourcesImpl;
import org.apache.catalina.startup.Catalina;
import org.apache.catalina.startup.ContextRuleSet;
import org.apache.catalina.startup.NamingRuleSet;
import org.apache.tomcat.util.descriptor.web.ContextEjb;
import org.apache.tomcat.util.descriptor.web.ContextEnvironment;
import org.apache.tomcat.util.descriptor.web.ContextResource;
import org.apache.tomcat.util.descriptor.web.WebRuleSet;
import org.apache.tomcat.util.descriptor.web.WebXml;
//import org.apache.catalina.startup.WebRuleSet;
import org.apache.tomcat.util.digester.Digester;
import org.xml.sax.SAXException;

/*
 * TODO Ensure correct files are provided to {@link #processDefaultWebXml(File)}, {@link #processHostWebXml(File)}, {@link #processServerXml(File)}, {@link #processWebXml(File)} and {@link #processContextXml(File)}.<br>
 * TODO Ensure correct order: server.xml > context xml files > web xml files.<br>
 * TODO Host/Context/Resource with DataSource, JavaBean, JavaMail Session.<br>
 * TODO UserTransaction with true JTA Provider.<br>
 * TODO message-destination (web.xml)<br>
 * TODO Web fragment support<br>
 * TODO Test all factories in org.apache.naming.factory, e. g. SendMailFactory etc.
 * TODO "Please note that JNDI resource configuration changed somewhat between Tomcat 7.x and Tomcat 8.x as they are using different versions of Apache Commons DBCP library. You will most likely need to modify older JNDI resource configurations to match the syntax in the example below in order to make them work in Tomcat 8". See http://tomcat.apache.org/tomcat-8.0-doc/jndi-datasource-examples-howto.html.
 */
/**
 * @author Holger Thurow (thurow.h@gmail.com)
 * @since 29.07.17
 */
public class TomcatJNDI {

    private static final String URL_PKG_PREFIX = "org.apache.naming";
    private NamingResourcesImpl namingResources;
    private org.apache.catalina.core.NamingContextListener namingContextListener;
    private Server server;
    private StandardContext standardContext;
    private NamingContextListener globalNamingContextListener;
    private String hostName;
    private String engineName;
    private String contextName;

    public TomcatJNDI() {
        /* See Tomcat.enableNaming()
javax.naming.Context
public static final String URL_PKG_PREFIXES = "java.naming.factory.url.pkgs"
Constant that holds the name of the environment property for specifying the list of package prefixes to use when loading in URL context factories. The value of the property should be a colon-separated list of package prefixes for the class name of the factory class that will create a URL context factory. This property may be specified in the environment, an applet parameter, a system property, or one or more resource files. The prefix com.sun.jndi.url is always appended to the possibly empty list of package prefixes.
The value of this constant is "java.naming.factory.url.pkgs".

See also javax.naming.spi.NamingManager.getURLContext() */
        System.setProperty
                (Context.INITIAL_CONTEXT_FACTORY,
                        "org.apache.naming.java.javaURLContextFactory");

        String urlPkgPrefixes = System.getProperty(Context.URL_PKG_PREFIXES);
        urlPkgPrefixes =
                urlPkgPrefixes != null ? urlPkgPrefixes + ":" + URL_PKG_PREFIX
                                       : URL_PKG_PREFIX;
        System.setProperty(Context.URL_PKG_PREFIXES, urlPkgPrefixes);


    }

    private void initializeContext() {
        if (standardContext == null) {
            standardContext = new StandardContext();
            standardContext.setName("TomcatJNDI");
            standardContext.setParent(new StandardHost());
            StandardEngine standardEngine = new StandardEngine();
            standardContext.getParent().setParent(standardEngine);
            StandardService service = new StandardService();
            if (server == null) {
                server = new StandardServer();
            }
            service.setServer(server);
            standardEngine.setService(service);
            namingResources = new NamingResourcesImpl();
            standardContext.setNamingResources(namingResources);
            namingContextListener = new NamingContextListener();
            namingContextListener.setName("TomcatJNDI");

        }
    }

    /**
     * Subsequent calls with different context.xml files are possible. All objects are merged in one context. Comply with the correct order: conf/context.xml > context.xml.default > META-INF/context.xml respectively conf/Catalina/[host_name]/[context_name].xml).
     * @see #processServerXml(File)
     */
    public TomcatJNDI processContextXml(File contextXml) {
        initializeContext();
        Digester digester = new Digester();
        digester.push(standardContext);
        // Siehe org.apache.catalina.startup.ContextConfig.createContextDigester()
        NamingRuleSet namingRuleSet = new NamingRuleSet("Context/");
        ContextRuleSet contextRuleSet = new ContextRuleSet("", false);
//        namingRuleSet.addRuleInstances(digester);
        digester.addRuleSet(namingRuleSet);
        digester.addRuleSet(contextRuleSet);
        try {
            digester.parse(contextXml);
        }
        catch (IOException | SAXException e) {
            throw new RuntimeException(e);
        }
        return this;
    }

    /**
     * Initialization sequence is: {@link #processServerXml(File)} > {@link #processContextXml(File)} > {@link #processDefaultWebXml(File)} > {@link #processHostWebXml(File)} > {@link #processWebXml(File)}. Not every method must be called, but you have to comply with the given order.
     *
     * @param serverXml conf/server.xml
     */
    public TomcatJNDI processServerXml(File serverXml) {
        if (server == null) {
            if (serverXml.getName().equals("server.xml")) {
                TomcatJNDICatalina catalina = new TomcatJNDICatalina();
                Digester digester = catalina.getDigester();
                digester.push(catalina);
                try {
                    digester.parse(serverXml);
                    server = catalina.getServer();
                    initializeGlobalNamingContext();
                    initializeContextFromServerXml();
                }
                catch (IOException | SAXException e) {
                    throw new RuntimeException(e);
                }
            }
            else {
                throw new RuntimeException("Not a server.xml file");
            }
        }
        else {
            throw new RuntimeException("There can only be one server.xml");
        }
        return this;
    }

    private void initializeGlobalNamingContext() {
        NamingResourcesImpl globalNamingResources = server.getGlobalNamingResources();
        globalNamingContextListener = new NamingContextListener();
        globalNamingResources.addPropertyChangeListener(globalNamingContextListener);
        globalNamingContextListener.setName("TomcatJNDIServer");
        //ContextAccessController.setWritable("TomcatJNDIServer", server);
        globalNamingContextListener.lifecycleEvent(new LifecycleEvent(server, Lifecycle.CONFIGURE_START_EVENT, null));
    }

    /**
     * @see #processServerXml(File, String)
     */
    public TomcatJNDI processServerXml(File serverXml, String engineName, String hostName, String contextName) {
        Objects.requireNonNull(serverXml);
        this.engineName = Objects.requireNonNull(engineName);
        this.hostName = Objects.requireNonNull(hostName);
        this.contextName = Objects.requireNonNull(contextName);
        processServerXml(serverXml);
        return this;
    }

    /**
     * If you have declared some JNDI resources in server.xml within a Context element call this method or {@link #processServerXml(File, String, String, String)}. Here Engine name defaults to "Catalina",  Host name to "localhost".
     *
     * @param contextName name of the context as in Context's path attribute. With leading slash!
     */
    public TomcatJNDI processServerXml(File serverXml, String contextName) {
        processServerXml(serverXml, "Catalina", "localhost", contextName);
        return this;
    }

    private void initializeContextFromServerXml() {
        if (contextName != null) {
            standardContext = (StandardContext) server.findService(engineName).getContainer().findChild(hostName).findChild(contextName);
            namingContextListener = standardContext.getNamingContextListener();
            if (namingContextListener == null) {
                namingContextListener = new NamingContextListener();
                namingContextListener.setName(standardContext.getName());
            }
            namingResources = standardContext.getNamingResources();
            namingResources.addPropertyChangeListener(namingContextListener);
        }
    }

    /**
     *
     * @param hostWebXml web.xml.default
     * @see #processServerXml(File)
     */
    public TomcatJNDI processHostWebXml(File hostWebXml) {
        if (hostWebXml.getName().equals("web.xml.default")) {
            processWebXml(hostWebXml, true);
        }
        else {
            throw new RuntimeException("Not a web.xml.default file");
        }
        return this;
    }

    /**
     * @param setOverrideable web.xml.default: true. conf/web.xml and WEB-INF/web.xml: false.
     */
    @SuppressWarnings("WeakerAccess")
    private void processWebXml(File anyWebXml, boolean setOverrideable) {
        initializeContext();
        Digester digester = new Digester();
        WebXml webXml = new WebXml();
        digester.push(webXml);
        WebRuleSet webRuleSet = new WebRuleSet();
        webRuleSet.addRuleInstances(digester);
        try {
            digester.parse(anyWebXml);
            addEnvironment(webXml, setOverrideable);
            Collection<ContextResource> resources = webXml.getResourceRefs().values();
            for (ContextResource resource : resources) {
                namingResources.addResource(resource);
            }
            Map<String, ContextEjb> ejbRefs = webXml.getEjbRefs();
            for (ContextEjb contextEjb : ejbRefs.values()) {
                namingResources.addEjb(contextEjb);
            }
        }
        catch (IOException | SAXException e) {
            throw new RuntimeException(e);
        }
    }

    private void addEnvironment(WebXml webXml, boolean setOverrideable) {
        Collection<ContextEnvironment> envEntries = webXml.getEnvEntries().values();
        for (ContextEnvironment envEntry : envEntries) {
            envEntry.setOverride(setOverrideable);
            namingResources.addEnvironment(envEntry);
        }
    }

    /**
     *
     * @param webXml WEB-INF/web.xml.
     * @see #processServerXml(File)
     */
    public TomcatJNDI processWebXml(File webXml) {
        processWebXml(webXml, true);
        return this;
    }

    void _processWebXml(File webXmlFile) {
        processWebXml(webXmlFile, true);
    }

    /**
     *
     * @param defaultWebXml conf/web.xml
     * @see #processServerXml(File)
     */
    public TomcatJNDI processDefaultWebXml(File defaultWebXml) {
        processWebXml(defaultWebXml, false);
        return this;
    }

    public void start() {
        namingContextListener.lifecycleEvent(new LifecycleEvent(standardContext, Lifecycle.CONFIGURE_START_EVENT, null));
    }

    // TODO restore URL_PKG_PREFIXES
    public void tearDown() {
        if (namingContextListener != null) {
            namingContextListener.lifecycleEvent(new LifecycleEvent(standardContext, Lifecycle.CONFIGURE_STOP_EVENT, null));
//            namingResources.removePropertyChangeListener(namingContextListener);
            standardContext = null;
        }
        if (globalNamingContextListener != null) {
            globalNamingContextListener.lifecycleEvent(new LifecycleEvent(server, Lifecycle.CONFIGURE_STOP_EVENT, null));
        }
    }

    private static class TomcatJNDICatalina extends Catalina {
        private Digester getDigester() {
            return super.createStartDigester();
        }
    }
}
