package no.shhsoft.kafka.auth;

import org.apache.kafka.common.Endpoint;
import org.apache.kafka.common.acl.AclBinding;
import org.apache.kafka.common.acl.AclBindingFilter;
import org.apache.kafka.server.authorizer.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletionStage;

/**
 * @author <a href="mailto:shh@thathost.com">Sverre H. Huseby</a>
 */
public final class AlternativeAclAuthorizer
implements Authorizer {

    private static final Logger LOG = LoggerFactory.getLogger(AlternativeAclAuthorizer.class);

    @Override
    public Map<Endpoint, ? extends CompletionStage<Void>> start(final AuthorizerServerInfo authorizerServerInfo) {
        LOG.info("*** start");
        return Collections.emptyMap();
    }

    @Override
    public List<AuthorizationResult> authorize(final AuthorizableRequestContext authorizableRequestContext, final List<Action> list) {
        LOG.info("*** authorize principal: " + authorizableRequestContext.principal());
        final List<AuthorizationResult> results = new ArrayList<>(list.size());
        for (final Action action : list) {
            results.add(authorize(authorizableRequestContext, action));
        }
        return results;
    }

    private AuthorizationResult authorize(final AuthorizableRequestContext authorizableRequestContext, final Action action) {
        LOG.info("  *** authorize resource: " + action.resourcePattern() + ", operation: " + action.operation());
        return AuthorizationResult.DENIED;
    }

    @Override
    public List<? extends CompletionStage<AclCreateResult>> createAcls(final AuthorizableRequestContext authorizableRequestContext, final List<AclBinding> list) {
        LOG.info("*** createAcls");
        return Collections.emptyList();
    }

    @Override
    public List<? extends CompletionStage<AclDeleteResult>> deleteAcls(final AuthorizableRequestContext authorizableRequestContext, final List<AclBindingFilter> list) {
        LOG.info("*** deleteAcls");
        return Collections.emptyList();
    }

    @Override
    public Iterable<AclBinding> acls(final AclBindingFilter aclBindingFilter) {
        LOG.info("*** acls");
        return null;
    }

    @Override
    public void close()
    throws IOException {
        LOG.info("*** close");
    }

    @Override
    public void configure(final Map<String, ?> map) {
        LOG.info("*** configure");
    }

}
