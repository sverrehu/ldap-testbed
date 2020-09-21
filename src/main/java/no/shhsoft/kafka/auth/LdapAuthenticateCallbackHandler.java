package no.shhsoft.kafka.auth;

import no.shhsoft.ldap.LdapAuthenticator;
import no.shhsoft.ldap.LdapConnector;
import org.apache.kafka.common.security.auth.AuthenticateCallbackHandler;
import org.apache.kafka.common.security.plain.PlainAuthenticateCallback;

import javax.security.auth.callback.Callback;
import javax.security.auth.callback.NameCallback;
import javax.security.auth.callback.UnsupportedCallbackException;
import javax.security.auth.login.AppConfigurationEntry;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

/**
 * @author <a href="mailto:shh@thathost.com">Sverre H. Huseby</a>
 */
public final class LdapAuthenticateCallbackHandler
implements AuthenticateCallbackHandler {

    private static final Logger LOG = Logger.getLogger(LdapAuthenticateCallbackHandler.class.getName());
    private static final String CONFIG_LDAP_HOST = "ldap.username.host";
    private static final String CONFIG_LDAP_PORT = "ldap.username.port";
    private static final String CONFIG_LDAP_USERNAME_TO_DN_FORMAT = "ldap.username.to.dn.format";
    private static final String CONFIG_LDAP_BASE_DN = "ldap.base.dn";
    private static final String SASL_PLAIN = "PLAIN";
    private LdapAuthenticator authenticator;

    @Override
    public void configure(final Map<String, ?> configs, String saslMechanism, List<AppConfigurationEntry> jaasConfigEntries) {
        if (!SASL_PLAIN.equals(saslMechanism)) {
            throw new IllegalArgumentException("Only SASL mechanism \"" + SASL_PLAIN + "\" is supported.");
        }
        configure(configs);
    }

    private void configure(final Map<String, ?> configs) {
        final String host = getRequiredStringProperty(configs, CONFIG_LDAP_HOST);
        final int port = getRequiredIntProperty(configs, CONFIG_LDAP_PORT);
        final String baseDn = getRequiredStringProperty(configs, CONFIG_LDAP_BASE_DN);
        final String userNameToDnFormat = getRequiredStringProperty(configs, CONFIG_LDAP_USERNAME_TO_DN_FORMAT);
        authenticator = new LdapAuthenticator(new LdapConnector(host, port, port == 636, baseDn), userNameToDnFormat);
        LOG.info("Configured.");
    }

    private int getRequiredIntProperty(final Map<String, ?> configs, final String name) {
        final String stringValue = getRequiredStringProperty(configs, name);
        try {
            return Integer.parseInt(stringValue);
        } catch (final NumberFormatException e) {
            throw new IllegalArgumentException("Value must be numeric in configuration property \"" + name + "\".");
        }
    }

    private String getRequiredStringProperty(final Map<String, ?> configs, final String name) {
        final Object value = configs.get(name);
        if (value == null) {
            throw new IllegalArgumentException("Missing required configuration property \"" + name + "\".");
        }
        return value.toString();
    }

    @Override
    public void close() {
        LOG.info("Closed.");
    }

    @Override
    public void handle(final Callback[] callbacks)
    throws UnsupportedCallbackException {
        if (authenticator == null) {
            throw new IllegalStateException("Handler not properly configured.");
        }
        String userName = null;
        PlainAuthenticateCallback plainAuthenticateCallback = null;
        for (final Callback callback : callbacks) {
            if (callback instanceof NameCallback) {
                userName = ((NameCallback) callback).getDefaultName();
            } else if (callback instanceof PlainAuthenticateCallback) {
                plainAuthenticateCallback = (PlainAuthenticateCallback) callback;
            } else {
                throw new UnsupportedCallbackException(callback);
            }
        }
        if (plainAuthenticateCallback == null) {
            throw new IllegalStateException("Expected PlainAuthenticationCallback was not found.");
        }
        final boolean authenticated = authenticator.authenticateByUserName(userName, plainAuthenticateCallback.password());
        if (authenticated) {
            LOG.info("User \"" + userName + "\" authenticated.");
        } else {
            LOG.warning("Authentication failed for user \"" + userName + "\".");
        }
        plainAuthenticateCallback.authenticated(authenticated);
    }

}
