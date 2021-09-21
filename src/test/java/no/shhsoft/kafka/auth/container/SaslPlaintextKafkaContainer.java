package no.shhsoft.kafka.auth.container;

import com.github.dockerjava.api.command.ExecCreateCmdResponse;
import com.github.dockerjava.api.command.InspectContainerResponse;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.images.builder.Transferable;
import org.testcontainers.utility.DockerImageName;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Based on KafkaContainer from the testcontainers project version 1.15.0.
 *
 * @author <a href="mailto:shh@thathost.com">Sverre H. Huseby</a>
 */
public class SaslPlaintextKafkaContainer
extends GenericContainer<SaslPlaintextKafkaContainer> {

    private static final DockerImageName DEFAULT_IMAGE = DockerImageName.parse("confluentinc/cp-kafka").withTag("6.2.0");
    public static final String DEFAULT_SUPER_USERNAME = "kafka";
    public static final String DEFAULT_SUPER_PASSWORD = "kafka";
    private static final String STARTER_SCRIPT = "/testcontainers_start.sh";
    private static final String JAAS_CONFIG_FILE = "/tmp/broker_jaas.conf";
    public static final String INTERNAL_LISTENER_NAME = "BROKER";
    public static final int KAFKA_PORT = 9092;
    public static final int KAFKA_INTERNAL_PORT = 9093;
    public static final int ZOOKEEPER_PORT = 2181;
    private static final int PORT_NOT_ASSIGNED = -1;
    /* Note difference between 0.0.0.0 and localhost: The former will be replaced by the container IP. */
    private static final String LISTENERS = "SASL_PLAINTEXT://0.0.0.0:" + KAFKA_PORT + "," + INTERNAL_LISTENER_NAME + "://127.0.0.1:" + KAFKA_INTERNAL_PORT;
    private int port = PORT_NOT_ASSIGNED;
    private String superUsername = DEFAULT_SUPER_USERNAME;
    private String superPassword = DEFAULT_SUPER_PASSWORD;
    private final Map<String, String> usernamesAndPasswords = new HashMap<>();

    public SaslPlaintextKafkaContainer() {
        this(DEFAULT_IMAGE);
    }

    public SaslPlaintextKafkaContainer(final DockerImageName dockerImageName) {
        super(dockerImageName);
        withExposedPorts(KAFKA_PORT, ZOOKEEPER_PORT);
        withEnv("KAFKA_BROKER_ID", "1");
        withEnv("KAFKA_OFFSETS_TOPIC_REPLICATION_FACTOR", "1");
        withEnv("KAFKA_OFFSETS_TOPIC_NUM_PARTITIONS", "1");
        withEnv("KAFKA_LOG_FLUSH_INTERVAL_MESSAGES", Long.MAX_VALUE + "");
        withEnv("KAFKA_GROUP_INITIAL_REBALANCE_DELAY_MS", "0");
        withEnv("KAFKA_LISTENERS", LISTENERS);
        withEnv("KAFKA_LISTENER_SECURITY_PROTOCOL_MAP", "SASL_PLAINTEXT:SASL_PLAINTEXT," + INTERNAL_LISTENER_NAME + ":SASL_PLAINTEXT");
        withEnv("KAFKA_INTER_BROKER_LISTENER_NAME", INTERNAL_LISTENER_NAME);
        withEnv("KAFKA_SASL_MECHANISM_INTER_BROKER_PROTOCOL", "PLAIN");
        withEnv("KAFKA_SASL_ENABLED_MECHANISMS", "PLAIN");
        withEnv("KAFKA_AUTHORIZER_CLASS_NAME", dockerImageName.getVersionPart().compareTo("6") >= 0 ? "kafka.security.authorizer.AclAuthorizer" : "kafka.security.auth.SimpleAclAuthorizer");
        withEnv("KAFKA_SUPER_USERS", "User:kafka");
        withEnv("KAFKA_OPTS", "-Djava.security.auth.login.config=" + JAAS_CONFIG_FILE);
        withSuperUser(superUsername, superPassword);
        withUser("alice", "alice-secret");
        withUser("bob", "bob-secret");
    }

    public String getBootstrapServers() {
        if (port == PORT_NOT_ASSIGNED) {
            throw new IllegalStateException("You should start Kafka container first");
        }
        return String.format("%s:%s", getHost(), port);
    }

    public SaslPlaintextKafkaContainer withSuperUser(final String username, final String password) {
        this.superUsername = assertValidUsernameAndPassword(username);
        this.superPassword = assertValidUsernameAndPassword(password);
        withEnv("KAFKA_SUPER_USERS", "User:" + username);
        return this;
    }

    public SaslPlaintextKafkaContainer withUser(final String usernameAndPassword) {
        return withUser(usernameAndPassword, usernameAndPassword);
    }

    public SaslPlaintextKafkaContainer withUser(final String username, final String password) {
        usernamesAndPasswords.put(
        assertValidUsernameAndPassword(username), assertValidUsernameAndPassword(password));
        return this;
    }

    @Override
    protected void doStart() {
        withCommand("sh", "-c", "while [ ! -f " + STARTER_SCRIPT + " ]; do sleep 0.1; done; " + STARTER_SCRIPT);
        super.doStart();
    }

    @Override
    protected void containerIsStarting(final InspectContainerResponse containerInfo, final boolean reused) {
        super.containerIsStarting(containerInfo, reused);
        followOutput(new TtyConsumer(getContainerId().substring(0, 12)));
        port = getMappedPort(KAFKA_PORT);
        if (reused) {
            return;
        }
        withEnv("KAFKA_LISTENER_NAME_SASL_PLAINTEXT_PLAIN_SASL_JAAS_CONFIG", createJaasLoginLine());
        uploadJaasConfig();
        createStartupScript(startZookeeper());
    }

    private void createStartupScript(final String zookeeperConnect) {
        final String listeners = getEnvMap().get("KAFKA_LISTENERS");
        if (listeners == null) {
            throw new RuntimeException("Need environment variable KAFKA_LISTENERS");
        }
        final String advertisedListeners = listeners.replaceAll(":" + KAFKA_PORT, ":" + getMappedPort(KAFKA_PORT))
                                                    .replaceAll("0\\.0\\.0\\.0", getContainerIpAddress());
        final String starterScript = "#!/bin/bash\n"
                                     + "export KAFKA_ZOOKEEPER_CONNECT='" + zookeeperConnect + "'\n"
                                     + "export KAFKA_ADVERTISED_LISTENERS='" + advertisedListeners + "'\n"
                                     + ". /etc/confluent/docker/bash-config\n"
                                     + "/etc/confluent/docker/configure\n"
                                     + "/etc/confluent/docker/launch\n";
        copyFileToContainer(Transferable.of(starterScript.getBytes(StandardCharsets.UTF_8), 0755), STARTER_SCRIPT);
    }

    private String startZookeeper() {
        final ExecCreateCmdResponse execCreateCmdResponse = dockerClient.execCreateCmd(getContainerId())
            .withCmd("sh", "-c", "echo '*** Starting Zookeeper'\n"
                                 + "printf 'clientPort=" + ZOOKEEPER_PORT + "\n"
                                 + "dataDir=/var/lib/zookeeper/data\ndataLogDir=/var/lib/zookeeper/log' > zookeeper.properties\n"
                                 + "zookeeper-server-start zookeeper.properties\n")
            .withAttachStderr(true)
            .withAttachStdout(true)
            .exec();
        try {
            dockerClient.execStartCmd(execCreateCmdResponse.getId()).start().awaitStarted(10, TimeUnit.SECONDS);
        } catch (final InterruptedException e) {
            throw new RuntimeException(e);
        }
        return "127.0.0.1:" + ZOOKEEPER_PORT;
    }

    private void uploadJaasConfig() {
        final String jaas = "KafkaServer { " + createJaasLoginLine() + " };\n";
        copyFileToContainer(Transferable.of(jaas.getBytes(StandardCharsets.UTF_8), 0644), JAAS_CONFIG_FILE);
    }

    private String createJaasLoginLine() {
        /* Precondition: No usernames or passwords contain characters that need special handling for JAAS config. */
        final StringBuilder sb = new StringBuilder();
        sb.append("org.apache.kafka.common.security.plain.PlainLoginModule required username=\"");
        sb.append(superUsername);
        sb.append("\" password=\"");
        sb.append(superPassword);
        sb.append("\" user_");
        sb.append(superUsername);
        sb.append("=\"");
        sb.append(superPassword);
        sb.append("\"");
        for (final Map.Entry<String, String> entry : usernamesAndPasswords.entrySet()) {
            sb.append(" user_");
            sb.append(entry.getKey());
            sb.append("=\"");
            sb.append(entry.getValue());
            sb.append("\"");
        }
        sb.append(";");
        return sb.toString();
    }

    private static String assertValidUsernameAndPassword(final String s) {
        /* Enforcing, in order to not have to deal with escaping for the JAAS config. */
        for (final char c : s.toCharArray()) {
            if (!(c >= 'a' && c <= 'z' || c >= 'A' && c <= 'Z' || c >= '0' && c <= '9' || c == '-')) {
                throw new RuntimeException(
                "Only letters, digits and hyphens allowed in usernames and passwords.");
            }
        }
        return s;
    }

}
