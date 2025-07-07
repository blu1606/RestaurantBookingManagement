package restaurantbookingmanagement.ai.handlers;

import restaurantbookingmanagement.ai.AIResponse;
import restaurantbookingmanagement.view.ConsoleView;

public interface AIActionHandler {
    void handle(AIResponse response, ServiceContext context, ConsoleView view);
} 