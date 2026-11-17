package lab.rates;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.DeliverCallback;

import java.io.IOException;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.SQLException;

/**
 * Consumes snapshots and stores them in PostgreSQL.
 * - valid message      -> INSERT ... ON CONFLICT DO NOTHING (idempotent), then ack
 * - invalid message    -> reject without requeue -> broker moves it to the DLQ
 * Manual acknowledgement means a message is removed only after it is safely stored.
 */
public class RateConsumer {

    private static final String INSERT = """
            INSERT INTO rate_snapshot (message_id, rate_date, currency, buy, sell)
            VALUES (?, ?, ?, ?, ?)
            ON CONFLICT (message_id) DO NOTHING
            """;

    private final Channel channel;
    private final Connection db;

    public RateConsumer(Channel channel, Connection db) {
        this.channel = channel;
        this.db = db;
    }

    public String start() throws IOException {
        channel.basicQos(1);
        DeliverCallback onMessage = (tag, delivery) -> {
            long deliveryTag = delivery.getEnvelope().getDeliveryTag();
            try {
                RateSnapshot s = RatePublisher.JSON.readValue(delivery.getBody(), RateSnapshot.class);
                s.validate();
                save(s);
                channel.basicAck(deliveryTag, false);
            } catch (IllegalArgumentException | IOException | SQLException e) {
                // Malformed JSON, failed validation or DB constraint -> dead-letter, do not retry forever
                channel.basicReject(deliveryTag, false);
            }
        };
        return channel.basicConsume(Topology.QUEUE, false, onMessage, tag -> { });
    }

    private void save(RateSnapshot s) throws SQLException {
        try (PreparedStatement ps = db.prepareStatement(INSERT)) {
            ps.setObject(1, s.messageId());
            ps.setDate(2, Date.valueOf(s.rateDate()));
            ps.setString(3, s.currency());
            ps.setBigDecimal(4, s.buy());
            ps.setBigDecimal(5, s.sell());
            ps.executeUpdate();
        }
    }
}
