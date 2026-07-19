package com.nexuswms.config;

import com.nexuswms.dashboard.websocket.DashboardWebSocketHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.data.redis.listener.adapter.MessageListenerAdapter;

@Configuration
public class RedisSubscriberConfig {

    /* ----- Topics ------------------------------------------------------- */

    @Bean
    public ChannelTopic warehouseEventsTopic() {
        return new ChannelTopic("warehouse.events");
    }

    @Bean
    public ChannelTopic alertsTopic() {
        return new ChannelTopic("alerts");
    }

    /* ----- Listener Adapter --------------------------------------------- */

    /**
     * Wraps DashboardWebSocketHandler as a Redis message listener.
     * Delegates incoming messages to the handleMessage() method.
     */
    @Bean
    public MessageListenerAdapter messageListenerAdapter(DashboardWebSocketHandler handler) {
        return new MessageListenerAdapter(handler, "handleMessage");
    }

    /* ----- Container ---------------------------------------------------- */

    /**
     * Registers the listener on both warehouse.events and alerts channels.
     * Spring manages the subscriber thread lifecycle automatically.
     */
    @Bean
    public RedisMessageListenerContainer redisMessageListenerContainer(
            RedisConnectionFactory connectionFactory,
            MessageListenerAdapter messageListenerAdapter) {

        RedisMessageListenerContainer container = new RedisMessageListenerContainer();
        container.setConnectionFactory(connectionFactory);
        container.addMessageListener(messageListenerAdapter, warehouseEventsTopic());
        container.addMessageListener(messageListenerAdapter, alertsTopic());
        return container;
    }
}