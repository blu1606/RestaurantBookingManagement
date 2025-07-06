package restaurantbookingmanagement.ai;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import restaurantbookingmanagement.utils.DebugUtil;

/**
 * Helper class để giao tiếp với Python AI Agent
 */
public class AIAgentConnector {
    private static final String AI_API_URL = "http://localhost:5000/process";
    private static final int TIMEOUT_SECONDS = 30;
    
    private final HttpClient httpClient;
    private final Gson gson;
    private String currentSessionId;
    
    public AIAgentConnector() {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(TIMEOUT_SECONDS))
                .build();
        this.gson = new Gson();
        this.currentSessionId = generateSessionId();
    }
    
    private String generateSessionId() {
        return "session_" + System.currentTimeMillis() + "_" + (int)(Math.random() * 1000);
    }
    
    /**
     * Gửi yêu cầu đến AI Agent và nhận về phản hồi
     */
    public AIResponse processUserInput(String userInput) {
        return processUserInput(userInput, "USER");
    }
    
    /**
     * Gửi yêu cầu đến AI Agent và nhận về phản hồi với role
     */
    public AIResponse processUserInput(String userInput, String role) {
        try {
            // Tạo JSON request với session ID và role
            Map<String, Object> requestData = new HashMap<>();
            requestData.put("userInput", userInput);
            requestData.put("sessionId", currentSessionId);
            requestData.put("role", role);
            
            String jsonRequest = gson.toJson(requestData);
            
            // Tạo HTTP request
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(AI_API_URL))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(jsonRequest))
                    .timeout(Duration.ofSeconds(TIMEOUT_SECONDS))
                    .build();
            
            // Gửi request và nhận response
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            
            if (response.statusCode() == 200) {
                // Parse JSON response
                return parseJsonResponse(response.body());
            } else {
                System.err.println("AI API returned status code: " + response.statusCode());
                return createErrorResponse("Lỗi kết nối với AI Agent");
            }
            
        } catch (Exception e) {
            System.err.println("Error communicating with AI Agent: " + e.getMessage());
            return createErrorResponse("Không thể kết nối với AI Agent. Vui lòng thử lại sau.");
        }
    }
    
    /**
     * Parse JSON response từ AI Agent sử dụng Gson
     */
    private AIResponse parseJsonResponse(String jsonStr) {
        try {
            DebugUtil.debugPrint("🔍 DEBUG - Raw JSON from AI Agent: " + jsonStr);
            
            Map<String, Object> parameters = new HashMap<>();
            String action = "";
            String naturalResponse = "";
            
            // Parse JSON sử dụng Gson
            JsonObject jsonObject = gson.fromJson(jsonStr, JsonObject.class);
            
            // Parse action
            if (jsonObject.has("action")) {
                action = jsonObject.get("action").getAsString();
            }
            
            // Parse naturalResponse
            if (jsonObject.has("naturalResponse")) {
                naturalResponse = jsonObject.get("naturalResponse").getAsString();
            }
            
            // Parse parameters
            if (jsonObject.has("parameters")) {
                JsonObject paramsObj = jsonObject.getAsJsonObject("parameters");
                
                // Parse guests
                if (paramsObj.has("guests") && !paramsObj.get("guests").isJsonNull()) {
                    JsonElement guestsElement = paramsObj.get("guests");
                    if (guestsElement.isJsonPrimitive()) {
                        try {
                            parameters.put("guests", guestsElement.getAsInt());
                        } catch (Exception e) {
                            // If not an integer, try as string
                            parameters.put("guests", guestsElement.getAsString());
                        }
                    }
                }
                
                // Parse time
                if (paramsObj.has("time") && !paramsObj.get("time").isJsonNull()) {
                    parameters.put("time", paramsObj.get("time").getAsString());
                }
                
                // Parse customerName
                if (paramsObj.has("customerName") && !paramsObj.get("customerName").isJsonNull()) {
                    parameters.put("customerName", paramsObj.get("customerName").getAsString());
                }
                
                // Parse customerPhone
                if (paramsObj.has("customerPhone") && !paramsObj.get("customerPhone").isJsonNull()) {
                    parameters.put("customerPhone", paramsObj.get("customerPhone").getAsString());
                }
                
                // Parse bookingId
                if (paramsObj.has("bookingId") && !paramsObj.get("bookingId").isJsonNull()) {
                    try {
                        parameters.put("bookingId", paramsObj.get("bookingId").getAsInt());
                    } catch (Exception e) {
                        // If not an integer, try as string
                        parameters.put("bookingId", paramsObj.get("bookingId").getAsString());
                    }
                }
                
                // Parse dishes/items
                if (paramsObj.has("dishes")) {
                    JsonArray dishesArray = paramsObj.getAsJsonArray("dishes");
                    List<Map<String, Object>> dishes = new ArrayList<>();
                    
                    for (JsonElement dishElement : dishesArray) {
                        JsonObject dishObj = dishElement.getAsJsonObject();
                        Map<String, Object> dish = new HashMap<>();
                        
                        if (dishObj.has("name")) {
                            dish.put("name", dishObj.get("name").getAsString());
                        }
                        if (dishObj.has("quantity")) {
                            dish.put("quantity", dishObj.get("quantity").getAsInt());
                        }
                        
                        dishes.add(dish);
                    }
                    
                    parameters.put("dishes", dishes);
                }
                
                // Parse items (new format from AI)
                if (paramsObj.has("items")) {
                    JsonArray itemsArray = paramsObj.getAsJsonArray("items");
                    List<Map<String, Object>> items = new ArrayList<>();
                    for (JsonElement itemElement : itemsArray) {
                        JsonObject itemObj = itemElement.getAsJsonObject();
                        Map<String, Object> item = new HashMap<>();
                        if (itemObj.has("name")) {
                            item.put("name", itemObj.get("name").getAsString());
                        }
                        if (itemObj.has("item")) {
                            item.put("name", itemObj.get("item").getAsString());
                        }
                        if (itemObj.has("quantity")) {
                            item.put("quantity", itemObj.get("quantity").getAsInt());
                        }
                        items.add(item);
                    }
                    parameters.put("dishes", items); // Map to dishes for compatibility
                }
                
                // Parse single item + quantity (fallback for AI)
                if (paramsObj.has("item") && paramsObj.has("quantity")) {
                    List<Map<String, Object>> singleDish = new ArrayList<>();
                    Map<String, Object> dish = new HashMap<>();
                    dish.put("name", paramsObj.get("item").getAsString());
                    dish.put("quantity", paramsObj.get("quantity").getAsInt());
                    singleDish.add(dish);
                    parameters.put("dishes", singleDish);
                }
                
                // Parse single name + quantity (fallback for AI)
                if (paramsObj.has("name") && paramsObj.has("quantity")) {
                    List<Map<String, Object>> singleDish = new ArrayList<>();
                    Map<String, Object> dish = new HashMap<>();
                    dish.put("name", paramsObj.get("name").getAsString());
                    dish.put("quantity", paramsObj.get("quantity").getAsInt());
                    singleDish.add(dish);
                    parameters.put("dishes", singleDish);
                }
                
                // Parse menu item parameters (for add_menu action)
                if (paramsObj.has("name") && !paramsObj.get("name").isJsonNull()) {
                    parameters.put("name", paramsObj.get("name").getAsString());
                }
                
                if (paramsObj.has("price") && !paramsObj.get("price").isJsonNull()) {
                    try {
                        parameters.put("price", paramsObj.get("price").getAsDouble());
                    } catch (Exception e) {
                        // If not a double, try as string
                        parameters.put("price", paramsObj.get("price").getAsString());
                    }
                }
                
                if (paramsObj.has("description") && !paramsObj.get("description").isJsonNull()) {
                    parameters.put("description", paramsObj.get("description").getAsString());
                }
                
                // Parse menu item ID (for delete_menu action)
                if (paramsObj.has("itemId") && !paramsObj.get("itemId").isJsonNull()) {
                    try {
                        parameters.put("itemId", paramsObj.get("itemId").getAsInt());
                    } catch (Exception e) {
                        // If not an integer, try as string
                        parameters.put("itemId", paramsObj.get("itemId").getAsString());
                    }
                }
                
                // Parse table capacity (for add_table action)
                if (paramsObj.has("capacity") && !paramsObj.get("capacity").isJsonNull()) {
                    try {
                        parameters.put("capacity", paramsObj.get("capacity").getAsInt());
                    } catch (Exception e) {
                        // If not an integer, try as string
                        parameters.put("capacity", paramsObj.get("capacity").getAsString());
                    }
                }
                
                // Parse search term (for customer_search action)
                if (paramsObj.has("searchTerm") && !paramsObj.get("searchTerm").isJsonNull()) {
                    parameters.put("searchTerm", paramsObj.get("searchTerm").getAsString());
                }
                
                // Parse date (for booking actions)
                if (paramsObj.has("date") && !paramsObj.get("date").isJsonNull()) {
                    parameters.put("date", paramsObj.get("date").getAsString());
                }
                
                // Parse requiresJavaService flag
                if (jsonObject.has("requiresJavaService")) {
                    parameters.put("requiresJavaService", jsonObject.get("requiresJavaService").getAsBoolean());
                }
                
                // Parse javaServiceType
                if (jsonObject.has("javaServiceType")) {
                    parameters.put("javaServiceType", jsonObject.get("javaServiceType").getAsString());
                }
            }
            
            DebugUtil.debugPrint("🔍 DEBUG - Parsed AI Response:");
            DebugUtil.debugPrint("   - Action: " + action);
            DebugUtil.debugPrint("   - NaturalResponse: " + naturalResponse);
            DebugUtil.debugPrint("   - Parameters size: " + parameters.size());
            DebugUtil.debugPrint("   - CustomerName: " + parameters.get("customerName"));
            DebugUtil.debugPrint("   - CustomerPhone: " + parameters.get("customerPhone"));
            DebugUtil.debugPrint("   - Guests: " + parameters.get("guests"));
            DebugUtil.debugPrint("   - Time: " + parameters.get("time"));
            DebugUtil.debugPrint("   - MenuItemName: " + parameters.get("name"));
            DebugUtil.debugPrint("   - MenuItemPrice: " + parameters.get("price"));
            DebugUtil.debugPrint("   - MenuItemDescription: " + parameters.get("description"));
            DebugUtil.debugPrint("   - MenuItemId: " + parameters.get("itemId"));
            DebugUtil.debugPrint("   - TableCapacity: " + parameters.get("capacity"));
            DebugUtil.debugPrint("   - SearchTerm: " + parameters.get("searchTerm"));
            DebugUtil.debugPrint("   - BookingId: " + parameters.get("bookingId"));
            DebugUtil.debugPrint("   - RequiresJavaService: " + parameters.get("requiresJavaService"));
            DebugUtil.debugPrint("   - JavaServiceType: " + parameters.get("javaServiceType"));
            
            if ("order_food".equals(action)) {
                boolean needCustomerInfo = false;
                if (!parameters.containsKey("customerName") || parameters.get("customerName") == null || ((String)parameters.get("customerName")).isEmpty()) {
                    needCustomerInfo = true;
                }
                if (!parameters.containsKey("customerPhone") || parameters.get("customerPhone") == null || ((String)parameters.get("customerPhone")).isEmpty()) {
                    needCustomerInfo = true;
                }
                if (needCustomerInfo) {
                    return new AIResponse("collect_customer_info", parameters, "Để gọi món, vui lòng cung cấp tên và số điện thoại khách hàng.");
                }
            }
            
            return new AIResponse(action, parameters, naturalResponse);
            
        } catch (Exception e) {
            System.err.println("Error parsing JSON response: " + e.getMessage());
            System.err.println("Raw JSON: " + jsonStr);
            return createErrorResponse("Lỗi xử lý phản hồi từ AI Agent");
        }
    }
    
    /**
     * Escape JSON string
     */
    private String escapeJson(String input) {
        return input.replace("\\", "\\\\")
                   .replace("\"", "\\\"")
                   .replace("\n", "\\n")
                   .replace("\r", "\\r")
                   .replace("\t", "\\t");
    }
    
    /**
     * Tạo phản hồi lỗi
     */
    private AIResponse createErrorResponse(String errorMessage) {
        Map<String, Object> params = new HashMap<>();
        return new AIResponse("error", params, errorMessage);
    }
    
    /**
     * Kiểm tra xem AI Agent có đang hoạt động không
     */
    public boolean isAIAgentAvailable() {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(AI_API_URL.replace("/process", "/health")))
                    .GET()
                    .timeout(Duration.ofSeconds(15))
                    .build();
            
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            return response.statusCode() == 200;
        } catch (Exception e) {
            return false;
        }
    }
    
    /**
     * Thông báo cho AI Agent rằng dữ liệu đã thay đổi và cần nạp lại kiến thức
     */
    public void notifyKnowledgeRefresh() {
        try {
            String refreshUrl = AI_API_URL.replace("/process", "/refresh-knowledge");
            
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(refreshUrl))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString("{}"))
                    .timeout(Duration.ofSeconds(30))
                    .build();
            
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            
            if (response.statusCode() == 200) {
                System.out.println("✅ AI Agent knowledge refreshed successfully.");
            } else {
                System.err.println("⚠️ AI Agent knowledge refresh returned status: " + response.statusCode());
            }
            
        } catch (Exception e) {
            System.err.println("⚠️ Could not notify AI Agent of knowledge refresh: " + e.getMessage());
        }
    }
    
    /**
     * Reset session để bắt đầu cuộc hội thoại mới
     */
    public void resetSession() {
        this.currentSessionId = generateSessionId();
        System.out.println("🔄 Session reset: " + currentSessionId);
    }
    
    /**
     * Lấy session ID hiện tại
     */
    public String getCurrentSessionId() {
        return currentSessionId;
    }
} 