package com.github.blakelai.components;

import com.github.blakelai.SecurityConfigFactory;
import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.HasComponents;
import com.vaadin.flow.component.Tag;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Anchor;
import org.pac4j.core.context.WebContext;
import org.pac4j.core.profile.AnonymousProfile;
import org.pac4j.core.profile.CommonProfile;
import org.pac4j.core.profile.ProfileManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;

@Tag("google-sign-in")
public class GoogleSignInComponent extends Component implements HasComponents {
    private static final Logger logger = LoggerFactory.getLogger(GoogleSignInComponent.class);

    protected void onAttach(AttachEvent attachEvent) {
        getUI().ifPresent(ui -> {
            WebContext context = SecurityConfigFactory.getWebContext();
            ProfileManager<CommonProfile> manager = new ProfileManager<>(context);
            Optional<CommonProfile> profile = manager.get(true);
            if (profile.isPresent() && !AnonymousProfile.INSTANCE.equals(profile.get())) {
                logger.info("{} is logged in", profile.get().getFirstName());
                Button logoutButton = new Button("Logout");
                Anchor logoutAnchor = new Anchor(SecurityConfigFactory.LOGOUT_URL, logoutButton);
                add(logoutAnchor);
                return;
            }

            Button loginButton = new Button("Login with Google");
            Anchor loginAnchor = new Anchor("/login", loginButton);
            add(loginAnchor);
        });

    }
}
