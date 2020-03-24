package com.github.blakelai;

import com.nimbusds.oauth2.sdk.id.State;
import com.nimbusds.openid.connect.sdk.Nonce;
import com.vaadin.flow.server.ServiceInitEvent;
import com.vaadin.flow.server.VaadinServiceInitListener;
import com.vaadin.flow.server.WrappedSession;
import okhttp3.HttpUrl;
import org.pac4j.oidc.config.OidcConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class ApplicationServiceInitListener implements VaadinServiceInitListener {
    private static final Logger logger = LoggerFactory.getLogger(ApplicationServiceInitListener.class);

    private static final String GOOGLE_AUTHORIZATION_ENDPOINT = "https://accounts.google.com/o/oauth2/v2/auth";

    @Override
    public void serviceInit(ServiceInitEvent event) {
        event.getSource().addUIInitListener(
                initEvent -> logger.info("A new UI has been initialized!"));

        event.addBootstrapListener(response -> {
            // BoostrapListener to change the bootstrap page
        });

        event.addDependencyFilter((dependencies, filterContext) -> {
            // DependencyFilter to add/remove/change dependencies sent to
            // the client
            return dependencies;
        });

        event.addRequestHandler((session, request, response) -> {
            // RequestHandler to change how responses are handled
            if ("/login".equals(request.getPathInfo())) {
                WrappedSession servletSession = session.getSession();
                State state = new State();
                servletSession.setAttribute(OidcConfiguration.STATE_SESSION_ATTRIBUTE, state);
                Nonce nonce = new Nonce();
                servletSession.setAttribute(OidcConfiguration.NONCE_SESSION_ATTRIBUTE, nonce);

                Map<String, String> params = new HashMap<>();
                params.put("response_type", "code");
                params.put("client_id", SecurityConfigFactory.GOOGLE_CLIENT_ID);
                params.put("scope", "openid profile email");
                params.put("redirect_uri", SecurityConfigFactory.getCallbackUrl(SecurityConfigFactory.getWebContext(), SecurityConfigFactory.GOOGLE_CLIENT_NAME));
                params.put("state", state.getValue());

                HttpUrl.Builder builder = Objects.requireNonNull(HttpUrl.parse(GOOGLE_AUTHORIZATION_ENDPOINT)).newBuilder();
                params.forEach(builder::addQueryParameter);
                String location = builder.build().toString();
                response.setStatus(307); // Temporary Redirect
                response.setHeader("Location", location);
                return true;
            }
            return false;
        });
    }

}
