package no.shhsoft.kafka.auth;

import org.apache.kafka.common.security.auth.AuthenticationContext;
import org.apache.kafka.common.security.auth.KafkaPrincipal;
import org.apache.kafka.common.security.auth.SaslAuthenticationContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class LdapGroupsPrincipalBuilder
extends AbstractPrincipalBuilder {

    private static final Logger LOG = LoggerFactory.getLogger(LdapGroupsPrincipalBuilder.class);

    @Override
    public KafkaPrincipal build(final AuthenticationContext context) {
        LOG.info("*** Build. context is " + context.getClass().getName());
        if (context instanceof SaslAuthenticationContext) {
            final SaslAuthenticationContext saslContext = (SaslAuthenticationContext) context;
        }
        return delegateToDefaultBuilder(context);
    }

}
