package lab.rates;

import com.rabbitmq.client.Channel;

import java.io.IOException;
import java.util.Map;

/**
 * RabbitMQ objects used by the lab:
 *   exchange "rates" (direct) --routing key "rate.snapshot"--> queue "rates.snapshots"
 *   rejected messages go to the dead-letter exchange "rates.dlx" --> queue "rates.snapshots.dlq"
 */
public final class Topology {

    public static final String EXCHANGE = "rates";
    public static final String ROUTING_KEY = "rate.snapshot";
    public static final String QUEUE = "rates.snapshots";
    public static final String DLX = "rates.dlx";
    public static final String DLQ = "rates.snapshots.dlq";

    private Topology() {
    }

    public static void declare(Channel channel) throws IOException {
        channel.exchangeDeclare(DLX, "fanout", true);
        channel.queueDeclare(DLQ, true, false, false, null);
        channel.queueBind(DLQ, DLX, "");

        channel.exchangeDeclare(EXCHANGE, "direct", true);
        channel.queueDeclare(QUEUE, true, false, false, Map.of("x-dead-letter-exchange", DLX));
        channel.queueBind(QUEUE, EXCHANGE, ROUTING_KEY);
    }
}
