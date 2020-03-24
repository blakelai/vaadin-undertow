package com.github.blakelai;

import com.vaadin.flow.server.VaadinServletRequest;
import com.vaadin.flow.server.VaadinServletResponse;
import org.pac4j.core.authorization.authorizer.IsAnonymousAuthorizer;
import org.pac4j.core.authorization.authorizer.IsAuthenticatedAuthorizer;
import org.pac4j.core.authorization.authorizer.RequireAnyRoleAuthorizer;
import org.pac4j.core.client.Clients;
import org.pac4j.core.client.direct.AnonymousClient;
import org.pac4j.core.config.Config;
import org.pac4j.core.config.ConfigFactory;
import org.pac4j.core.context.J2EContext;
import org.pac4j.core.context.WebContext;
import org.pac4j.core.http.callback.CallbackUrlResolver;
import org.pac4j.core.http.callback.PathParameterCallbackUrlResolver;
import org.pac4j.core.http.url.DefaultUrlResolver;
import org.pac4j.core.http.url.UrlResolver;
import org.pac4j.oidc.client.GoogleOidcClient;
import org.pac4j.oidc.config.OidcConfiguration;

public class SecurityConfigFactory implements ConfigFactory {
    public static final String CALLBACK_URL = "/callback";
    public static final String LOGOUT_URL = "/logout";
    public static final String ANONYMOUS_CLIENT_NAME = "anonymous";
    public static final String GOOGLE_CLIENT_NAME = "google";
    public static final String GOOGLE_CLIENT_ID = "XXXXXXXXXX.apps.googleusercontent.com";
    public static final UrlResolver URL_RESOLVER = new DefaultUrlResolver(true);
    public static final CallbackUrlResolver CALLBACK_URL_RESOLVER = new PathParameterCallbackUrlResolver();

    private static final String GOOGLE_SECRET = "XXXXXXXXXX";

    public static WebContext getWebContext() {
        return new J2EContext(VaadinServletRequest.getCurrent(), VaadinServletResponse.getCurrent());
    }

    public static String getCallbackUrl(WebContext context, String clientName) {
        return CALLBACK_URL_RESOLVER.compute(URL_RESOLVER, CALLBACK_URL, clientName, context);
    }

    @Override
    public Config build(Object... objects) {
        final AnonymousClient anonymousClient = new AnonymousClient();
        anonymousClient.setName(ANONYMOUS_CLIENT_NAME);

        OidcConfiguration oidcConfig = new OidcConfiguration();
        oidcConfig.setClientId(GOOGLE_CLIENT_ID);
        oidcConfig.setSecret(GOOGLE_SECRET);
        final GoogleOidcClient googleClient = new GoogleOidcClient(oidcConfig);
        googleClient.setName(GOOGLE_CLIENT_NAME);

        final Clients clients = new Clients(CALLBACK_URL, anonymousClient, googleClient);
        clients.setDefaultSecurityClients(anonymousClient.getName());
        clients.setUrlResolver(URL_RESOLVER);
        clients.setCallbackUrlResolver(CALLBACK_URL_RESOLVER);

        final Config config = new Config(clients);
        config.addAuthorizer("anon", new IsAnonymousAuthorizer<>());
        config.addAuthorizer("user", new IsAuthenticatedAuthorizer<>());
        config.addAuthorizer("admin", new RequireAnyRoleAuthorizer<>("ROLE_ADMIN"));
        return config;
    }
}
