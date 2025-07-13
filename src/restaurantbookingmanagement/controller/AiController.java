package restaurantbookingmanagement.controller;

import restaurantbookingmanagement.ai.AIAgentConnector;
import restaurantbookingmanagement.ai.AIResponse;
import restaurantbookingmanagement.model.Customer;
import java.util.HashMap;
import java.util.Map;
import java.util.List;

/**
 * Controller điều phối giao tiếp với AI Agent, không chứa logic nghiệp vụ.
 */
public class AiController {
    private final AIAgentConnector aiAgentConnector;
    // Lưu trạng thái pending action theo sessionId
    private final Map<String, PendingAction> pendingActions = new HashMap<>();

    public static class PendingAction {
        public String originalTool;
        public Map<String, Object> collectedParams;
        public List<String> missingParams;
    }

    public AiController() {
        this.aiAgentConnector = new AIAgentConnector();
    }

    /**
     * Gửi yêu cầu đến AI Agent và nhận phản hồi
     * Nếu truyền customer thì sẽ ưu tiên, nếu không thì chỉ dùng role.
     * Nếu có pending action, gửi kèm thông tin pending.
     */
    public AIResponse chatWithAI(String userInput, String role, Customer customer, String sessionId) {
        PendingAction pending = pendingActions.get(sessionId);
        AIResponse aiResponse;
        if (pending != null) {
            aiResponse = aiAgentConnector.processUserInput(
                userInput,
                role,
                customer,
                pending.originalTool,
                pending.collectedParams,
                pending.missingParams
            );
        } else {
            aiResponse = (customer != null)
                ? aiAgentConnector.processUserInput(userInput, role, customer)
                : aiAgentConnector.processUserInput(userInput, role);
        }
        // Nếu AI trả về ask_for_info, lưu lại pending action
        if ("ask_for_info".equals(aiResponse.getAction())) {
            PendingAction newPending = new PendingAction();
            Object origTool = aiResponse.getParameters().get("original_tool");
            newPending.originalTool = origTool != null ? origTool.toString() : null;
            Object collected = aiResponse.getParameters().get("collected_params");
            if (collected instanceof Map) {
                newPending.collectedParams = (Map<String, Object>) collected;
            } else {
                newPending.collectedParams = new HashMap<>();
            }
            Object missing = aiResponse.getParameters().get("missing_params");
            if (missing instanceof List) {
                newPending.missingParams = (List<String>) missing;
            } else {
                newPending.missingParams = new java.util.ArrayList<>();
            }
            pendingActions.put(sessionId, newPending);
        } else {
            // Nếu không còn pending, xóa trạng thái
            pendingActions.remove(sessionId);
        }
        return aiResponse;
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

    // Overload cho tương thích code cũ
    public AIResponse chatWithAI(String userInput, String role, Customer customer) {
        return chatWithAI(userInput, role, customer, "default");
    }
    public AIResponse chatWithAI(String userInput, String role) {
        return chatWithAI(userInput, role, null, "default");
    }
} 