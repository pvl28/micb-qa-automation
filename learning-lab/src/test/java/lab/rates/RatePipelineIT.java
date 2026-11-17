package lab.rates;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.ConnectionFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.RabbitMQContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
import java.util.function.BooleanSupplier;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests of the learning pipeline in real, disposable containers.
 * Without Docker the whole class is SKIPPED (disabledWithoutDocker), never faked.
 * All data is synthetic.
 */
@Testcontainers(disabledWithoutDocker = true)
class RatePipelineIT {

    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");

    @Container
    static final RabbitMQContainer RABBIT = new RabbitMQContainer("rabbitmq:3.13-management-alpine");

    private static Connection db;
    private static com.rabbitmq.client.Connection mq;
    private static Channel channel;
    private static RatePublisher publisher;

    @BeforeAll
    static void setUp() throws Exception {
        db = DriverManager.getConnection(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword());
        try (Statement st = db.createStatement()) {
            st.execute(Files.readString(Path.of("src/main/resources/schema.sql")));
        }
        ConnectionFactory factory = new ConnectionFactory();
        factory.setUri(RABBIT.getAmqpUrl());
        mq = factory.newConnection();
        channel = mq.createChannel();
        Topology.declare(channel);
        new RateConsumer(mq.createChannel(), db).start();
        publisher = new RatePublisher(channel);
    }

    @AfterAll
    static void tearDown() throws Exception {
        if (mq != null) mq.close();
        if (db != null) db.close();
    }

    @BeforeEach
    void cleanState() throws Exception {
        try (Statement st = db.createStatement()) {
            st.execute("TRUNCATE rate_snapshot");
        }
        channel.queuePurge(Topology.DLQ);
    }

    @Test
    @DisplayName("Valid message is stored in PostgreSQL exactly as sent")
    void validMessageIsStored() throws Exception {
        RateSnapshot s = snapshot("EUR", "19.9500", "20.4500");
        publisher.publish(s);

        await(() -> count("message_id = '" + s.messageId() + "'") == 1);
        try (Statement st = db.createStatement();
             ResultSet rs = st.executeQuery("SELECT currency, buy, sell FROM rate_snapshot WHERE message_id = '" + s.messageId() + "'")) {
            assertThat(rs.next()).isTrue();
            assertThat(rs.getString("currency")).isEqualTo("EUR");
            assertThat(rs.getBigDecimal("buy")).isEqualByComparingTo("19.95");
            assertThat(rs.getBigDecimal("sell")).isEqualByComparingTo("20.45");
        }
    }

    @Test
    @DisplayName("The same message delivered twice creates one row (idempotent consumer)")
    void duplicateMessageIsStoredOnce() throws Exception {
        RateSnapshot s = snapshot("USD", "17.8000", "18.1000");
        publisher.publish(s);
        publisher.publish(s);
        RateSnapshot marker = snapshot("GBP", "23.0000", "24.0000");
        publisher.publish(marker);  // processed after both duplicates (prefetch 1, single consumer)

        await(() -> count("message_id = '" + marker.messageId() + "'") == 1);
        assertThat(count("message_id = '" + s.messageId() + "'")).isEqualTo(1);
    }

    @Test
    @DisplayName("Message with buy > sell is rejected to the dead-letter queue and not stored")
    void invalidMessageGoesToDlq() throws Exception {
        RateSnapshot bad = snapshot("CHF", "22.0000", "21.0000");
        publisher.publish(bad);

        await(() -> dlqDepth() == 1);
        assertThat(count("message_id = '" + bad.messageId() + "'")).isZero();
    }

    @Test
    @DisplayName("Malformed JSON is rejected to the dead-letter queue")
    void malformedJsonGoesToDlq() throws Exception {
        publisher.publishRaw("{not json".getBytes(StandardCharsets.UTF_8), UUID.randomUUID().toString());
        await(() -> dlqDepth() == 1);
        assertThat(count("true")).isZero();
    }

    @Test
    @DisplayName("Database constraint blocks buy > sell even if application validation is bypassed")
    void databaseConstraintProtectsData() throws Exception {
        try (Statement st = db.createStatement()) {
            org.assertj.core.api.Assertions.assertThatThrownBy(() -> st.executeUpdate(
                            "INSERT INTO rate_snapshot (message_id, rate_date, currency, buy, sell) "
                                    + "VALUES (gen_random_uuid(), CURRENT_DATE, 'RON', 4.0, 3.0)"))
                    .hasMessageContaining("buy_not_above_sell");
        }
    }

    // ---------- helpers ----------

    private static RateSnapshot snapshot(String currency, String buy, String sell) {
        return new RateSnapshot(UUID.randomUUID(), LocalDate.of(2026, 1, 15), currency,
                new BigDecimal(buy), new BigDecimal(sell));
    }

    private static int count(String where) {
        try (Statement st = db.createStatement();
             ResultSet rs = st.executeQuery("SELECT count(*) FROM rate_snapshot WHERE " + where)) {
            rs.next();
            return rs.getInt(1);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    private static long dlqDepth() {
        try {
            return channel.messageCount(Topology.DLQ);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    /** Explicit polling wait with a deadline (asynchronous consumer), no fixed sleep in the test logic. */
    private static void await(BooleanSupplier condition) throws InterruptedException {
        Instant deadline = Instant.now().plus(Duration.ofSeconds(10));
        while (Instant.now().isBefore(deadline)) {
            if (condition.getAsBoolean()) {
                return;
            }
            Thread.sleep(100); // poll interval
        }
        throw new AssertionError("Condition not met within 10 s");
    }
}
