package no.shhsoft.kafka.auth;

import no.shhsoft.kafka.auth.container.LdapContainer;
import no.shhsoft.kafka.auth.container.TestKafkaContainer;
import no.shhsoft.ldap.LdapConnectionSpec;
import no.shhsoft.ldap.LdapUsernamePasswordAuthenticator;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.errors.TopicAuthorizationException;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.Arrays;
import java.util.concurrent.ExecutionException;

public class LdapAuthenticateCallbackHandlerIntegrationTest {

    public static final String TOPIC_WITH_USER_PRINCIPAL = "topic_with_user_principal";
    public static final String TOPIC_WITH_GROUP_PRINCIPAL = "topic_with_group_principal";
    private static TestKafkaContainer container;
    private static LdapContainer ldapContainer;

    @BeforeClass
    public static void beforeClass() {
        ldapContainer = new LdapContainer();
        ldapContainer.start();
        container = new TestKafkaContainer();
        container.withEnv("KAFKA_AUTHN_LDAP_BASE_DN", ldapContainer.getLdapBaseDn());
        container.withEnv("KAFKA_AUTHN_LDAP_HOST", ldapContainer.getLdapHost());
        container.withEnv("KAFKA_AUTHN_LDAP_PORT", String.valueOf(ldapContainer.getLdapPort()));
        container.withEnv("KAFKA_AUTHN_LDAP_USERNAME_TO_DN_FORMAT", LdapContainer.USERNAME_TO_DN_FORMAT);
        container.withEnv("KAFKA_AUTHN_LDAP_USERNAME_TO_UNIQUE_SEARCH_FORMAT", LdapContainer.USERNAME_TO_UNIQUE_SEARCH_FORMAT);
        container.withEnv("KAFKA_LISTENER_NAME_SASL__PLAINTEXT_PLAIN_SASL_SERVER_CALLBACK_HANDLER_CLASS", LdapAuthenticateCallbackHandler.class.getName());
//        container.withEnv("KAFKA_PRINCIPAL_BUILDER_CLASS", SpiffeKafkaPrincipalBuilder.class.getName());
        container.withEnv("KAFKA_AUTHORIZER_CLASS_NAME", LdapGroupAclAuthorizer.class.getName());
        container.start();
        setupTestTopicsAndAcls();
    }

    @Before
    public void before() {
        UserToGroupsCache.getInstance().clear();
    }

    private static void setupTestTopicsAndAcls() {
        assertLdapAuthenticationWorks();
        container.addTopic(TOPIC_WITH_USER_PRINCIPAL);
        container.addProducer(TOPIC_WITH_USER_PRINCIPAL, "User:" + LdapContainer.PRODUCER1_USER_PASS);
        container.addTopic(TOPIC_WITH_GROUP_PRINCIPAL);
        container.addProducer(TOPIC_WITH_GROUP_PRINCIPAL, "Group:" + LdapContainer.PRODUCER_GROUP);
    }

    private static void assertLdapAuthenticationWorks() {
        final LdapConnectionSpec ldapConnectionSpec = new LdapConnectionSpec(ldapContainer.getLdapHost(), ldapContainer.getLdapPort(), false, ldapContainer.getLdapBaseDn());
        final LdapUsernamePasswordAuthenticator ldapUsernamePasswordAuthenticator = new LdapUsernamePasswordAuthenticator(ldapConnectionSpec, LdapContainer.USERNAME_TO_DN_FORMAT, null);
        for (final String userPass : Arrays.asList("kafka", LdapContainer.PRODUCER1_USER_PASS, LdapContainer.PRODUCER2_USER_PASS, LdapContainer.NON_PRODUCER_USER_PASS)) {
            Assert.assertTrue("Failed for " + userPass, ldapUsernamePasswordAuthenticator.authenticate(userPass, userPass.toCharArray()));
        }
    }

    @Test(expected = TopicAuthorizationException.class)
    public void shouldNotProduceWhenNotProducerByUser() {
        try (final Producer<String, String> producer = container.getProducer(LdapContainer.NON_PRODUCER_USER_PASS, LdapContainer.NON_PRODUCER_USER_PASS)) {
            produce(producer, TOPIC_WITH_USER_PRINCIPAL, "foo");
        }
    }

    @Test
    public void shouldProduceWhenProducerByUser() {
        try (final Producer<String, String> producer = container.getProducer(LdapContainer.PRODUCER1_USER_PASS, LdapContainer.PRODUCER1_USER_PASS)) {
            produce(producer, TOPIC_WITH_USER_PRINCIPAL, "foo");
        }
    }

    @Test(expected = TopicAuthorizationException.class)
    public void shouldNotProduceWhenNotProducerByGroup() {
        try (final Producer<String, String> producer = container.getProducer(LdapContainer.NON_PRODUCER_USER_PASS, LdapContainer.NON_PRODUCER_USER_PASS)) {
            produce(producer, TOPIC_WITH_GROUP_PRINCIPAL, "foo");
        }
    }

    @Test
    public void shouldProduceWhenProducerByGroup() {
        try (final Producer<String, String> producer = container.getProducer(LdapContainer.PRODUCER2_USER_PASS, LdapContainer.PRODUCER2_USER_PASS)) {
            produce(producer, TOPIC_WITH_GROUP_PRINCIPAL, "foo");
        }
    }

    public void produce(final Producer<String, String> producer, final String topicName, final String recordValue) {
        final ProducerRecord<String, String> record = new ProducerRecord<>(topicName, null, recordValue);
        try {
            producer.send(record, (metadata, exception) -> {
                if (exception != null) {
                    throw (exception instanceof RuntimeException) ? (RuntimeException) exception : new RuntimeException(exception);
                }
            }).get(); // Make call synchronous, to be able to get exceptions in time.
        } catch (final InterruptedException | ExecutionException e) {
            final Throwable cause = e.getCause();
            throw (cause instanceof RuntimeException) ? (RuntimeException) cause : new RuntimeException(e);
        }
        producer.flush();
    }

}
