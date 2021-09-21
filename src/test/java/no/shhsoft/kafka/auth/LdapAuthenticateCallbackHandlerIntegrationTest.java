package no.shhsoft.kafka.auth;

import no.shhsoft.kafka.auth.container.ContainerTestUtils;
import no.shhsoft.kafka.auth.container.LdapContainer;
import no.shhsoft.kafka.auth.container.TestKafkaContainer;
import no.shhsoft.ldap.LdapConnectionSpec;
import no.shhsoft.ldap.LdapUsernamePasswordAuthenticator;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.errors.TopicAuthorizationException;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.concurrent.ExecutionException;

public class LdapAuthenticateCallbackHandlerIntegrationTest {

    private static final String USERNAME_TO_DN_FORMAT = "cn=%s,ou=People,dc=example,dc=com";
    public static final String TOPIC_WITH_USER_PRINCIPAL = "topic_with_user_principal";
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
        container.withEnv("KAFKA_AUTHN_LDAP_USERNAME_TO_DN_FORMAT", USERNAME_TO_DN_FORMAT);
        container.withEnv("KAFKA_LISTENER_NAME_SASL__PLAINTEXT_PLAIN_SASL_SERVER_CALLBACK_HANDLER_CLASS", LdapAuthenticateCallbackHandler.class.getName());
        container.start();
        setupTestTopicsAndAcls();
    }

    private static void setupTestTopicsAndAcls() {
        assertLdapAuthenticationWorks();
        container.addTopic(TOPIC_WITH_USER_PRINCIPAL);
        container.addProducer(TOPIC_WITH_USER_PRINCIPAL, "User:" + LdapContainer.PRODUCER1_USER_PASS);
    }

    private static void assertLdapAuthenticationWorks() {
        final LdapConnectionSpec ldapConnectionSpec = new LdapConnectionSpec(ldapContainer.getLdapHost(), ldapContainer.getLdapPort(), false, ldapContainer.getLdapBaseDn());
        final LdapUsernamePasswordAuthenticator ldapUsernamePasswordAuthenticator = new LdapUsernamePasswordAuthenticator(ldapConnectionSpec, USERNAME_TO_DN_FORMAT, null);
        for (final String userPass : Arrays.asList("kafka", LdapContainer.PRODUCER1_USER_PASS, LdapContainer.NON_PRODUCER_USER_PASS)) {
            Assert.assertTrue("Failed for " + userPass, ldapUsernamePasswordAuthenticator.authenticate(userPass, userPass.toCharArray()));
        }
    }

    @Test(expected = TopicAuthorizationException.class)
    public void shouldNotProduceWhenNotProducer() {
        try (final Producer<String, String> producer = container.getProducer(LdapContainer.NON_PRODUCER_USER_PASS, LdapContainer.NON_PRODUCER_USER_PASS)) {
            produce(producer, TOPIC_WITH_USER_PRINCIPAL, "foo");
        }
    }

    @Test
    public void shouldProduceWhenProducer() {
        try (final Producer<String, String> producer = container.getProducer(LdapContainer.PRODUCER1_USER_PASS, LdapContainer.PRODUCER1_USER_PASS)) {
            produce(producer, TOPIC_WITH_USER_PRINCIPAL, "foo");
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
