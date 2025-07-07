package restaurantbookingmanagement.controller;

import restaurantbookingmanagement.ai.AIAgentConnector;
import restaurantbookingmanagement.ai.AIResponse;
import restaurantbookingmanagement.model.Customer;

public class AiController {
    private final AIAgentConnector aiAgentConnector;

    public AiController() {
        this.aiAgentConnector = new AIAgentConnector();
    }

    /**
     * Gửi yêu cầu đến AI Agent và nhận phản hồi
     */
    public AIResponse chatWithAI(String userInput, String role, String sessionId) {
        // Có thể mở rộng để truyền sessionId nếu cần
        return aiAgentConnector.processUserInput(userInput, role);
    }
    
    /**
     * Gửi yêu cầu đến AI Agent và nhận phản hồi với session ID
     */
    public AIResponse chatWithAI(String userInput, String role) {
        return aiAgentConnector.processUserInput(userInput, role);
    }

    /**
     * Kiểm tra AI Agent có sẵn sàng không
     */
    public boolean isAIAgentAvailable() {
        return aiAgentConnector.isAIAgentAvailable();
    }

    /**
     * Thông báo AI Agent refresh knowledge
     */
    public void notifyKnowledgeRefresh() {
        aiAgentConnector.notifyKnowledgeRefresh();
    }

    public AIResponse chatWithAI(String userInput, String role, Customer currentCustomer) {
        return aiAgentConnector.processUserInput(userInput, role, currentCustomer);
    }
} 