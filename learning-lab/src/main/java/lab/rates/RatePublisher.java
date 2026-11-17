package lab.rates;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;

import java.io.IOException;

/** Publishes rate snapshots as persistent JSON messages. */
public class RatePublisher {

    // findAndRegisterModules() picks up jackson-datatype-jsr310 for LocalDate
    static final ObjectMapper JSON = new ObjectMapper().findAndRegisterModules();

    private final Channel channel;

    public RatePublisher(Channel channel) {
        this.channel = channel;
    }

    public void publish(RateSnapshot snapshot) throws IOException {
        publishRaw(JSON.writeValueAsBytes(snapshot), snapshot.messageId().toString());
    }

    /** Lets tests send malformed payloads too. */
    public void publishRaw(byte[] body, String messageId) throws IOException {
        AMQP.BasicProperties props = new AMQP.BasicProperties.Builder()
                .contentType("application/json")
                .deliveryMode(2)            // persistent: survives a broker restart
                .messageId(messageId)
                .build();
        channel.basicPublish(Topology.EXCHANGE, Topology.ROUTING_KEY, props, body);
    }
}
