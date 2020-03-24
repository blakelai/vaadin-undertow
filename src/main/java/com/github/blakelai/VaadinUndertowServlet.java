package com.github.blakelai;

import com.amadeus.session.SessionConfiguration;
import com.amadeus.session.servlet.InitializeSessionManagement;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouteAlias;
import com.vaadin.flow.server.VaadinServlet;
import com.vaadin.flow.server.startup.RouteRegistryInitializer;
import com.vaadin.flow.server.startup.ServletContextListeners;
import io.undertow.Handlers;
import io.undertow.Undertow;
import io.undertow.UndertowOptions;
import io.undertow.server.HttpHandler;
import io.undertow.server.handlers.accesslog.AccessLogHandler;
import io.undertow.servlet.Servlets;
import io.undertow.servlet.api.DeploymentInfo;
import io.undertow.servlet.api.DeploymentManager;
import io.undertow.servlet.api.InstanceHandle;
import io.undertow.servlet.api.ServletContainerInitializerInfo;
import io.undertow.websockets.jsr.WebSocketDeploymentInfo;
import org.pac4j.core.config.Config;
import org.pac4j.core.context.session.J2ESessionStore;
import org.pac4j.core.matching.PathMatcher;
import org.pac4j.j2e.filter.CallbackFilter;
import org.pac4j.j2e.filter.LogoutFilter;
import org.pac4j.j2e.filter.SecurityFilter;
import org.reflections.Reflections;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.KeyManager;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.servlet.DispatcherType;
import javax.servlet.Filter;
import javax.servlet.ServletException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.KeyStore;
import java.security.SecureRandom;
import java.util.HashMap;
import java.util.Set;

import static com.github.blakelai.SecurityConfigFactory.*;

public class VaadinUndertowServlet {
    private static final Logger logger = LoggerFactory.getLogger(VaadinUndertowServlet.class);

    public static final int HTTP_PORT = 8080;
    public static final int HTTPS_PORT = 8443;
    public static final String SERVER_HOST = "0.0.0.0";
    private static final char[] STORE_PASSWORD = "password".toCharArray();

    public static void main(String[] args) {
        new VaadinUndertowServlet().startup();
    }

    public void startup() {
        final ClassLoader classLoader = VaadinUndertowServlet.class.getClassLoader();
        DeploymentInfo servletBuilder
                = Servlets.deployment()
                .setClassLoader(classLoader)
                .setContextPath("/")
                .setDeploymentName("ROOT.war")
                .setDefaultEncoding("UTF-8")
                .setResourceManager(new VaadinFlowResourceManager(classLoader));

        servletBuilder
                .addServlet(
                        Servlets.servlet("vaadin-app", VaadinServlet.class)
                                .addInitParam("UI", VaadinUI.class.getName())
                                .addInitParam("pushMode", "manual")
                                .setAsyncSupported(true)
                                .addMapping("/*")
                )
                .addServletContextAttribute(
                        WebSocketDeploymentInfo.ATTRIBUTE_NAME, new WebSocketDeploymentInfo()
                );

        Config config = new SecurityConfigFactory().build();
        config.setSessionStore(new J2ESessionStore());
        config.addMatcher("vaadin", new PathMatcher("^\\/VAADIN\\/.*$"));
        SecurityFilter securityFilter = new SecurityFilter(config);
        securityFilter.setClients(String.join(",", ANONYMOUS_CLIENT_NAME, GOOGLE_CLIENT_NAME));
        securityFilter.setMatchers("vaadin");
        CallbackFilter callbackFilter = new CallbackFilter(config);
        LogoutFilter logoutFilter = new LogoutFilter(config, "/");
        logoutFilter.setDestroySession(true);

        SessionConfiguration sessionConfig = new SessionConfiguration();
        sessionConfig.setDistributable(false);

        servletBuilder
                .addServletContextAttribute(SessionConfiguration.class.getName(), sessionConfig)
                .addServletContainerInitializer(
                        new ServletContainerInitializerInfo(
                                InitializeSessionManagement.class,
                                null
                        )
                )
                .addServletContainerInitializer(
                        new ServletContainerInitializerInfo(
                                RouteRegistryInitializer.class,
                                setOfRouteAnnotatedClasses()
                        )
                )
                .addFilter(Servlets.filter("callbackFilter", CallbackFilter.class, () -> new InstanceHandle<Filter>() {
                    @Override
                    public Filter getInstance() {
                        return callbackFilter;
                    }

                    @Override
                    public void release() { }
                }))
                .addFilterUrlMapping("callbackFilter", "/callback/*", DispatcherType.REQUEST)
                .addFilter(Servlets.filter("securityFilter", SecurityFilter.class, () -> new InstanceHandle<Filter>() {
                    @Override
                    public Filter getInstance() {
                        return securityFilter;
                    }

                    @Override
                    public void release() { }
                }))
                .addFilterUrlMapping("securityFilter", "/*", DispatcherType.REQUEST)
                .addFilter(Servlets.filter("logoutFilter", LogoutFilter.class, () -> new InstanceHandle<Filter>() {
                    @Override
                    public Filter getInstance() {
                        return logoutFilter;
                    }

                    @Override
                    public void release() { }
                }))
                .addFilterUrlMapping("logoutFilter", LOGOUT_URL, DispatcherType.REQUEST)
                .addListener(Servlets.listener(ServletContextListeners.class));

        final DeploymentManager manager = Servlets.defaultContainer().addDeployment(servletBuilder);
        manager.deploy();

        try {
            HttpHandler servletHandler = new AccessLogHandler.Builder()
                    .build(new HashMap<String, Object>() {{
                        put("format", "%h %l %u %t \"%r\" %s %b \"%{i,Referer}\" \"%{i,User-Agent}\"");
                    }})
                    .wrap(manager.start());
            HttpHandler pathHandler = Handlers.path(Handlers.redirect("/"))
                    .addPrefixPath("/", servletHandler);

            SSLContext sslContext = createSSLContext("keystore", "truststore");
            Undertow server = Undertow.builder()
                    .setServerOption(UndertowOptions.ENABLE_HTTP2, true)
                    .addHttpListener(HTTP_PORT, SERVER_HOST)
                    .addHttpsListener(HTTPS_PORT, SERVER_HOST, sslContext)
                    .setHandler(pathHandler)
                    .build();
            server.start();

            server.getListenerInfo().forEach(e -> logger.info(e.toString()));
        } catch (ServletException e) {
            logger.warn("Fail to start undertow server: {}", e.getMessage());
            throw new RuntimeException(e);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public Set<Class<?>> setOfRouteAnnotatedClasses() {
        Reflections reflections = new Reflections("com.github.blakelai.views");
        Set<Class<?>> routeClasses = reflections.getTypesAnnotatedWith(Route.class);
        routeClasses.addAll(reflections.getTypesAnnotatedWith(RouteAlias.class));
        return routeClasses;
    }

    private KeyStore loadKeyStore(String name) throws Exception {
        String storeLoc = name + ".pkcs12";
        final InputStream stream = Files.newInputStream(Paths.get(storeLoc));
        KeyStore loadedKeystore = KeyStore.getInstance("pkcs12");
        loadedKeystore.load(stream, password(name));
        return loadedKeystore;
    }

    static char[] password(String name) {
        String pw = System.getProperty(name + ".password");
        return pw != null ? pw.toCharArray() : STORE_PASSWORD;
    }

    private SSLContext createSSLContext(final String keyStoreName, final String trustStoreName) throws Exception {
        KeyManager[] keyManagers;
        KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
        keyManagerFactory.init(loadKeyStore(keyStoreName), password(keyStoreName));
        keyManagers = keyManagerFactory.getKeyManagers();

        /*
        TrustManager[] trustManagers;
        TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
        trustManagerFactory.init(loadKeyStore(trustStoreName));
        trustManagers = trustManagerFactory.getTrustManagers();
         */

        SSLContext sslContext;
        sslContext = SSLContext.getInstance("TLS");
        sslContext.init(keyManagers, null, SecureRandom.getInstanceStrong());

        return sslContext;
    }
}
