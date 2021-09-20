package no.shhsoft.kafka.auth;

import kafka.security.authorizer.AclAuthorizer;
import org.apache.kafka.server.authorizer.Action;
import org.apache.kafka.server.authorizer.AuthorizableRequestContext;
import org.apache.kafka.server.authorizer.AuthorizationResult;

import java.util.List;

public class LdapGroupAclAuthorizer
extends AclAuthorizer {

    @Override
    public List<AuthorizationResult> authorize(final AuthorizableRequestContext requestContext, final List<Action> actions) {
        return super.authorize(requestContext, actions);
    }

}
