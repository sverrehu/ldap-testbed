package no.shhsoft.kafka.auth;

import org.apache.kafka.common.security.auth.AuthenticateCallbackHandler;

import javax.security.auth.callback.Callback;
import javax.security.auth.callback.UnsupportedCallbackException;
import javax.security.auth.login.AppConfigurationEntry;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

/**
 * @author <a href="mailto:shh@thathost.com">Sverre H. Huseby</a>
 */
public final class LdapAuthenticateCallbackHandler
implements AuthenticateCallbackHandler {

    private static final Logger LOG = Logger.getLogger(LdapAuthenticateCallbackHandler.class.getName());

    @Override
    public void configure(final Map<String, ?> configs, String saslMechanism, List<AppConfigurationEntry> jaasConfigEntries) {
        final StringBuilder sb = new StringBuilder();
        sb.append("config:\n");
        for (final Map.Entry<String, ?> entry : configs.entrySet()) {
            sb.append("  " + entry.getKey() + "=" + entry.getValue().toString() + "\n");
        }
        sb.append("saslMechanism=" + saslMechanism + "\n");
        sb.append("jaasConfigEntries\n");
        for (final AppConfigurationEntry entry : jaasConfigEntries) {
            sb.append("  " + entry.toString() + "\n");
        }
        LOG.info(sb.toString());
    }

    @Override
    public void close() {

    }

    @Override
    public void handle(final Callback[] callbacks)
    throws IOException, UnsupportedCallbackException {

    }

}
