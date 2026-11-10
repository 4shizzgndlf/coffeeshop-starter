package ek.osnb.demo.coffeeshop.ai;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
class AssistantConfiguration {

    @Bean
    ChatClient chatClient(ChatClient.Builder builder) {
        ChatMemory memory = MessageWindowChatMemory.builder()
                .maxMessages(20)
                .build();

        return builder
                .defaultSystem("""
                        You are the Coffee Shop assistant. Use a tool before stating facts about products,
                        prices, stock, or orders. Never invent products, prices, stock levels, order details,
                        or delivery details. If a tool has no matching data, say so plainly. You may recommend
                        products based only on tool results. You cannot change data, place orders, cancel orders,
                        mark orders shipped, or modify carts. Never claim that you performed an action.
                        """)
                .defaultAdvisors(MessageChatMemoryAdvisor.builder(memory).build())
                .build();
    }
}
