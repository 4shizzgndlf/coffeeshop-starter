package ek.osnb.demo.coffeeshop.ai;

import jakarta.validation.Valid;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import ek.osnb.demo.coffeeshop.customer.CustomerApi;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/assistant")
class ChatController {
    private final ChatClient chatClient;
    private final CatalogTools catalogTools;
    private final OrderTools orderTools;
    private final StaffCatalogTools staffCatalogTools;
    private final CustomerApi customers;

    ChatController(
            ChatClient chatClient,
            CatalogTools catalogTools,
            OrderTools orderTools,
            StaffCatalogTools staffCatalogTools,
            CustomerApi customers) {
        this.chatClient = chatClient;
        this.catalogTools = catalogTools;
        this.orderTools = orderTools;
        this.staffCatalogTools = staffCatalogTools;
        this.customers = customers;
    }

    @PostMapping("/chat")
    ChatResponse chat(Authentication authentication, @Valid @RequestBody ChatRequest request) {
        String reply;
        if (isStaff(authentication)) {
            reply = chatClient.prompt()
                    .user(request.message())
                    .tools(catalogTools, staffCatalogTools)
                    .advisors(advisor -> advisor.param(ChatMemory.CONVERSATION_ID, authentication.getName()))
                    .call()
                    .content();
        } else {
            reply = chatForCustomer(authentication, request.message());
        }
        return new ChatResponse(reply);
    }

    private String chatForCustomer(Authentication authentication, String message) {
        var customer = customers.findCustomerByEmail(authentication.getName());
        if (customer.isEmpty()) {
            return chatClient.prompt()
                    .user(message)
                    .tools(catalogTools)
                    .advisors(advisor -> advisor.param(ChatMemory.CONVERSATION_ID, authentication.getName()))
                    .call()
                    .content();
        }

        return chatClient.prompt()
                .user(message)
                .tools(catalogTools, orderTools)
                .toolContext(Map.of("customerId", customer.get().id()))
                .advisors(advisor -> advisor.param(ChatMemory.CONVERSATION_ID, authentication.getName()))
                .call()
                .content();
    }

    private boolean isStaff(Authentication authentication) {
        return authentication.getAuthorities().stream()
                .anyMatch(authority -> authority.getAuthority().equals("ROLE_STAFF"));
    }
}
