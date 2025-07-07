package restaurantbookingmanagement.service;

import restaurantbookingmanagement.ai.AIResponse;
import restaurantbookingmanagement.view.ConsoleView;
import restaurantbookingmanagement.ai.handlers.AIActionHandlerRegistry;

public class AiService {
    private final AIActionHandlerRegistry handlerRegistry = new AIActionHandlerRegistry();
    /**
     * Xử lý tất cả các actions từ AI Agent một cách thống nhất
     */
    public void processAIResponse(AIResponse aiResponse, OrderService orderService, 
                                BookingService bookingService, CustomerService customerService, 
                                ConsoleView view) {
        try {
            handlerRegistry.get(aiResponse.getAction())
                .handle(aiResponse, orderService, bookingService, customerService, view);
        } catch (Exception e) {
            view.displayError("❌ Lỗi xử lý AI response: " + e.getMessage());
            System.err.println("🔥 Error processing AI response: " + e.getMessage());
        }
    }
} 