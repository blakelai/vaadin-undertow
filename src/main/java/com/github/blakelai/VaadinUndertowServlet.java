package com.github.blakelai;

import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouteAlias;
import com.vaadin.flow.server.VaadinServlet;
import com.vaadin.flow.server.startup.RouteRegistryInitializer;
import com.vaadin.flow.server.startup.ServletContextListeners;
import io.undertow.Handlers;
import io.undertow.Undertow;
import io.undertow.server.handlers.PathHandler;
import io.undertow.servlet.Servlets;
import io.undertow.servlet.api.DeploymentInfo;
import io.undertow.servlet.api.DeploymentManager;
import io.undertow.servlet.api.ServletContainerInitializerInfo;
import io.undertow.websockets.jsr.WebSocketDeploymentInfo;
import org.reflections.Reflections;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.ServletException;
import java.util.Set;

public class VaadinUndertowServlet {
    private static final Logger logger = LoggerFactory.getLogger(VaadinUndertowServlet.class);

    public static final int SERVER_PORT = 8080;
    public static final String SERVER_HOST = "0.0.0.0";

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
                                .addInitParam("async-supported", "true")
                                .addInitParam("pushMode", "automatic")
                                .addMapping("/*")
                )
                .addServletContextAttribute(
                        WebSocketDeploymentInfo.ATTRIBUTE_NAME, new WebSocketDeploymentInfo()
                );

        servletBuilder
                .addServletContainerInitializer(
                        new ServletContainerInitializerInfo(
                                RouteRegistryInitializer.class,
                                setOfRouteAnnotatedClasses()
                        )
                )
                .addListener(Servlets.listener(ServletContextListeners.class));

        final DeploymentManager manager = Servlets.defaultContainer().addDeployment(servletBuilder);
        manager.deploy();

        try {
            PathHandler path = Handlers.path(Handlers.redirect("/"))
                    .addPrefixPath("/" , manager.start());
            Undertow server = Undertow.builder()
                    .addHttpListener(SERVER_PORT, SERVER_HOST)
                    .setHandler(path)
                    .build();
            server.start();

            server.getListenerInfo().forEach(e -> logger.info(e.toString()));
        } catch (ServletException e) {
            logger.warn("Fail to start undertow server: {}", e.getMessage());
            throw new RuntimeException(e);
        }
    }

    public Set<Class<?>> setOfRouteAnnotatedClasses() {
        Reflections reflections = new Reflections("com.github.blakelai.views");
        Set<Class<?>> routeClasses = reflections.getTypesAnnotatedWith(Route.class);
        routeClasses.addAll(reflections.getTypesAnnotatedWith(RouteAlias.class));
        return routeClasses;
    }

}
