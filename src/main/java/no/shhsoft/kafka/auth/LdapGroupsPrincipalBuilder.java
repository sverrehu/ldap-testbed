package no.shhsoft.kafka.auth;

import org.apache.kafka.common.security.auth.AuthenticationContext;
import org.apache.kafka.common.security.auth.KafkaPrincipal;

public final class LdapGroupsPrincipalBuilder
extends AbstractPrincipalBuilder {

    @Override
    public KafkaPrincipal build(final AuthenticationContext context) {
        return null;
    }

}
