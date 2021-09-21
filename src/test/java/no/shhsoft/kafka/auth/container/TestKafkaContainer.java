package no.shhsoft.kafka.auth.container;

import com.github.dockerjava.api.command.InspectContainerResponse;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.common.acl.AccessControlEntry;
import org.apache.kafka.common.acl.AclBinding;
import org.apache.kafka.common.acl.AclOperation;
import org.apache.kafka.common.acl.AclPermissionType;
import org.apache.kafka.common.resource.PatternType;
import org.apache.kafka.common.resource.ResourcePattern;
import org.apache.kafka.common.resource.ResourceType;
import org.testcontainers.images.builder.Transferable;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;

public class TestKafkaContainer
extends SaslPlaintextKafkaContainer {

    private static final String COMBO_JAR_ARTIFACT_NAME = "ldap-testbed";
    private final String pathToComboJar;

    public TestKafkaContainer() {
        pathToComboJar = findPathToComboJar(new File("../"));
    }

    @Override
    protected void containerIsStarting(final InspectContainerResponse containerInfo, final boolean reused) {
        copyComboJar();
        super.containerIsStarting(containerInfo, reused);
    }

    private void copyComboJar() {
        try {
            final byte[] comboJarBytes = Files.readAllBytes(Path.of(pathToComboJar));
            System.out.println("*** Copying our jar to the container");
            copyFileToContainer(Transferable.of(comboJarBytes), "/usr/share/java/kafka/our-test.jar");
        } catch (final IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static String findPathToComboJar(final File dir) {
        final File[] files = dir.listFiles();
        if (files == null) {
            throw new RuntimeException("Nothing found? This should not happen.");
        }
        for (final File file : files) {
            if (file.isDirectory()) {
                final String path = findPathToComboJar(file);
                if (path != null) {
                    return path;
                }
            } else {
                if (file.getName().matches(COMBO_JAR_ARTIFACT_NAME + "-.*\\.jar")) {
                    return file.getPath();
                }
            }
        }
        return null;
    }

    public void addTopic(final String topicName) {
        final NewTopic newTopic = new NewTopic(topicName, 1, (short) 1);
        getSuperAdminClient().createTopics(Collections.singleton(newTopic));
    }

    public void addProducer(final String topicName, final String principal) {
        final AclBinding describeAclBinding = createBinding(topicName, principal, AclOperation.DESCRIBE);
        final AclBinding writeAclBinding = createBinding(topicName, principal, AclOperation.WRITE);
        final Collection<AclBinding> aclBindings = Arrays.asList(describeAclBinding, writeAclBinding);
        getSuperAdminClient().createAcls(aclBindings);
    }

    private AclBinding createBinding(final String topicName, final String principal, final AclOperation operation) {
        final ResourcePattern resourcePattern = new ResourcePattern(ResourceType.TOPIC, topicName, PatternType.LITERAL);
        final AccessControlEntry accessControlEntry = new AccessControlEntry(principal, "*", operation, AclPermissionType.ALLOW);
        return new AclBinding(resourcePattern, accessControlEntry);
    }

}
