package restaurantbookingmanagement.service;

import restaurantbookingmanagement.ai.AIResponse;
import restaurantbookingmanagement.view.ConsoleView;
import restaurantbookingmanagement.ai.handlers.AIActionHandlerRegistry;
import restaurantbookingmanagement.ai.handlers.ServiceContext;

public class AiService {
    private final AIActionHandlerRegistry handlerRegistry = new AIActionHandlerRegistry();
    private final MenuService menuService = new MenuService();
    private final TableService tableService = new TableService();
    /**
     * Xử lý tất cả các actions từ AI Agent một cách thống nhất
     */
    public void processAIResponse(AIResponse aiResponse, OrderService orderService, 
                                BookingService bookingService, CustomerService customerService, 
                                ConsoleView view) {
        try {
            // Tạo ServiceContext
            ServiceContext context = new ServiceContext(orderService, bookingService, customerService, menuService, tableService);
            handlerRegistry.get(aiResponse.getAction())
                .handle(aiResponse, context, view);
        } catch (Exception e) {
            view.displayError("❌ Lỗi xử lý AI response: " + e.getMessage());
            System.err.println("🔥 Error processing AI response: " + e.getMessage());
        }
    }
} 