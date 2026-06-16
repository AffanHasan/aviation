package aviation.bdd.support;

import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;

import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.kafka.KafkaContainer;
import org.testcontainers.utility.DockerImageName;

public final class TestcontainersSupport {

    private static final String KAFKA_IMAGE = "apache/kafka-native:3.8.1";
    private static final String KAFKA_IMAGE_COMPATIBLE = "apache/kafka";
    private static final String POSTGRES_IMAGE = "postgres:16";
    private static final String DB_SCHEMA_PATH = "/schema.sql";
    private static final String DB_NAME = "aviation";
    private static final String DB_USER = "aviation";
    private static final String DB_PASSWORD = "aviation";

    private KafkaContainer kafka;
    private PostgreSQLContainer<?> postgres;

    public void start() throws Exception {
        kafka = new KafkaContainer(DockerImageName.parse(KAFKA_IMAGE).asCompatibleSubstituteFor(KAFKA_IMAGE_COMPATIBLE))
                .withEnv("KAFKA_AUTO_CREATE_TOPICS_ENABLE", "true");
        kafka.start();

        postgres = new PostgreSQLContainer<>(POSTGRES_IMAGE)
                .withDatabaseName(DB_NAME)
                .withUsername(DB_USER)
                .withPassword(DB_PASSWORD);
        postgres.start();

        applySchema();
        publishSystemProperties();
    }

    public void stop() {
        if (postgres != null) {
            postgres.stop();
        }
        if (kafka != null) {
            kafka.stop();
        }
    }

    public String kafkaBootstrapServers() {
        return kafka.getBootstrapServers();
    }

    public String jdbcUrl() {
        return postgres.getJdbcUrl();
    }

    public DatabaseCredentials databaseCredentials() {
        return new DatabaseCredentials(postgres.getUsername(), postgres.getPassword());
    }

    public boolean isRunning() {
        return kafka.isRunning() && postgres.isRunning();
    }

    private void applySchema() throws Exception {
        final String sql = new String(TestcontainersSupport.class.getResourceAsStream(DB_SCHEMA_PATH).readAllBytes(),
                StandardCharsets.UTF_8);
        try (Connection conn = DriverManager.getConnection(jdbcUrl(), databaseCredentials().user(), databaseCredentials().password());
             Statement stmt = conn.createStatement()) {
            stmt.execute(sql);
        }
    }

    private void publishSystemProperties() {
        System.setProperty("jdbc-journal.slick.db.url", jdbcUrl());
        System.setProperty("jdbc-journal.slick.db.user", databaseCredentials().user());
        System.setProperty("jdbc-journal.slick.db.password", databaseCredentials().password());
        System.setProperty("kafka.bootstrap-servers", kafkaBootstrapServers());
    }

    public record DatabaseCredentials(String user, String password) {
    }
}
