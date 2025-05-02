package com.growith.tailo.chat.room.component;

import com.growith.tailo.chat.message.component.ChatConsumer;

import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.AmqpAdmin;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class QueueManager {

    private final AmqpAdmin amqpAdmin;
    private final TopicExchange chatExchange;
    private final RabbitTemplate rabbitTemplate;
    private final ChatConsumer chatConsumer;

    @Autowired
    public QueueManager(AmqpAdmin amqpAdmin, @Qualifier("chatExchange") TopicExchange chatExchange, RabbitTemplate rabbitTemplate, ChatConsumer chatConsumer) {
        this.amqpAdmin = amqpAdmin;
        this.chatExchange = chatExchange;
        this.rabbitTemplate = rabbitTemplate;
        this.chatConsumer = chatConsumer;
    }

    @Value("${spring.rabbitmq.chat.routing-key}")
    private String routingKey;

    @Value("${spring.rabbitmq.chat.queue}")
    private String queueName;

    @Autowired
    public QueueManager(AmqpAdmin amqpAdmin,
                        @Qualifier("chatExchange") TopicExchange chatExchange,
                        RabbitTemplate rabbitTemplate,
                        ChatConsumer chatConsumer) {
        this.amqpAdmin = amqpAdmin;
        this.chatExchange = chatExchange;
        this.rabbitTemplate = rabbitTemplate;
        this.chatConsumer = chatConsumer;
    }

    // 큐 생성 메서드
    public void createChatQueue(Long roomId) {
        String dynamicQueueName = queueName + roomId;
        log.info("{} 큐 존재 여부 : {}", dynamicQueueName, !existQueue(dynamicQueueName));



        // 큐가 이미 존재하는지 확인
        if (!existQueue(dynamicQueueName)) {
            // 큐 이름을 동적으로 생성
            Queue queue = new Queue(dynamicQueueName, true, false, true);  // 큐 이름은 roomId에 맞게 동적으로 생성

            // 바인딩 생성: 고정된 Exchange와 동적 큐, 라우팅 키를 사용
            Binding binding = BindingBuilder.bind(queue)
                    .to(chatExchange)  // 고정된 Exchange 사용
                    .with(routingKey + roomId);  // 동적인 라우팅 키 사용

            // 큐와 바인딩을 선언
            amqpAdmin.declareQueue(queue);
            amqpAdmin.declareBinding(binding);

            chatConsumer.startListening(dynamicQueueName);

        }
    }


    public void deleteQueue(String queueName){
        amqpAdmin.deleteQueue(queueName);
    }


    private boolean existQueue(String queueName) {
        try {
            // RabbitTemplate을 사용하여 큐를 직접 조회
            rabbitTemplate.execute(channel -> channel.queueDeclarePassive(queueName));
            return true;
        } catch (Exception e) {
            return false;  // 큐가 존재하지 않으면 예외가 발생하고 false를 반환
        }
    }
}