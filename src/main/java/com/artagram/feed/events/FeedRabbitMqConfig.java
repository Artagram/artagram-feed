package com.artagram.feed.events;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class FeedRabbitMqConfig {

    public static final String EXCHANGE = "artagram.events";
    public static final String DLX_EXCHANGE = "artagram.dlx";

    public static final String QUEUE_USER_REGISTERED = "feed.user.registered";
    public static final String QUEUE_USER_DELETED = "feed.user.deleted";
    public static final String QUEUE_FOLLOW_CREATED = "feed.follow.created";
    public static final String QUEUE_FOLLOW_DELETED = "feed.follow.deleted";
    public static final String QUEUE_MEDIA_PROCESSED = "feed.media.processed";
    public static final String QUEUE_CANVAS_CLOSED = "feed.canvas.closed";
    public static final String QUEUE_AI_COMPLETED = "feed.ai.completed";

    @Bean
    public TopicExchange eventsExchange() {
        return new TopicExchange(EXCHANGE, true, false);
    }

    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public Queue feedUserRegisteredQueue() {
        return QueueBuilder.durable(QUEUE_USER_REGISTERED)
                .deadLetterExchange(DLX_EXCHANGE)
                .deadLetterRoutingKey(QUEUE_USER_REGISTERED + ".dlq")
                .build();
    }

    @Bean
    public Binding feedUserRegisteredBinding(Queue feedUserRegisteredQueue, TopicExchange eventsExchange) {
        return BindingBuilder.bind(feedUserRegisteredQueue).to(eventsExchange).with("user.registered");
    }

    @Bean
    public Queue feedUserDeletedQueue() {
        return QueueBuilder.durable(QUEUE_USER_DELETED)
                .deadLetterExchange(DLX_EXCHANGE)
                .deadLetterRoutingKey(QUEUE_USER_DELETED + ".dlq")
                .build();
    }

    @Bean
    public Binding feedUserDeletedBinding(Queue feedUserDeletedQueue, TopicExchange eventsExchange) {
        return BindingBuilder.bind(feedUserDeletedQueue).to(eventsExchange).with("user.deleted");
    }

    @Bean
    public Queue feedFollowCreatedQueue() {
        return QueueBuilder.durable(QUEUE_FOLLOW_CREATED)
                .deadLetterExchange(DLX_EXCHANGE)
                .deadLetterRoutingKey(QUEUE_FOLLOW_CREATED + ".dlq")
                .build();
    }

    @Bean
    public Binding feedFollowCreatedBinding(Queue feedFollowCreatedQueue, TopicExchange eventsExchange) {
        return BindingBuilder.bind(feedFollowCreatedQueue).to(eventsExchange).with("follow.created");
    }

    @Bean
    public Queue feedFollowDeletedQueue() {
        return QueueBuilder.durable(QUEUE_FOLLOW_DELETED)
                .deadLetterExchange(DLX_EXCHANGE)
                .deadLetterRoutingKey(QUEUE_FOLLOW_DELETED + ".dlq")
                .build();
    }

    @Bean
    public Binding feedFollowDeletedBinding(Queue feedFollowDeletedQueue, TopicExchange eventsExchange) {
        return BindingBuilder.bind(feedFollowDeletedQueue).to(eventsExchange).with("follow.deleted");
    }

    @Bean
    public Queue feedMediaProcessedQueue() {
        return QueueBuilder.durable(QUEUE_MEDIA_PROCESSED)
                .deadLetterExchange(DLX_EXCHANGE)
                .deadLetterRoutingKey(QUEUE_MEDIA_PROCESSED + ".dlq")
                .build();
    }

    @Bean
    public Binding feedMediaProcessedBinding(Queue feedMediaProcessedQueue, TopicExchange eventsExchange) {
        return BindingBuilder.bind(feedMediaProcessedQueue).to(eventsExchange).with("media.processed");
    }

    @Bean
    public Queue feedCanvasClosedQueue() {
        return QueueBuilder.durable(QUEUE_CANVAS_CLOSED)
                .deadLetterExchange(DLX_EXCHANGE)
                .deadLetterRoutingKey(QUEUE_CANVAS_CLOSED + ".dlq")
                .build();
    }

    @Bean
    public Binding feedCanvasClosedBinding(Queue feedCanvasClosedQueue, TopicExchange eventsExchange) {
        return BindingBuilder.bind(feedCanvasClosedQueue).to(eventsExchange).with("canvas.closed");
    }

    @Bean
    public Queue feedAiCompletedQueue() {
        return QueueBuilder.durable(QUEUE_AI_COMPLETED)
                .deadLetterExchange(DLX_EXCHANGE)
                .deadLetterRoutingKey(QUEUE_AI_COMPLETED + ".dlq")
                .build();
    }

    @Bean
    public Binding feedAiCompletedBinding(Queue feedAiCompletedQueue, TopicExchange eventsExchange) {
        return BindingBuilder.bind(feedAiCompletedQueue).to(eventsExchange).with("ai.completed");
    }
}
