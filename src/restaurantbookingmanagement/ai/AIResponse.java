package restaurantbookingmanagement.ai;

import java.util.Map;

/**
 * Class đại diện cho phản hồi từ AI Agent
 */
public class AIResponse {
    private String action;
    private Map<String, Object> parameters;
    private String naturalResponse;
    
    public AIResponse() {
    }
    
    public AIResponse(String action, Map<String, Object> parameters, String naturalResponse) {
        this.action = action;
        this.parameters = parameters;
        this.naturalResponse = naturalResponse;
    }
    
    // Getters
    public String getAction() {
        return action;
    }
    
    public Map<String, Object> getParameters() {
        return parameters;
    }
    
    public String getNaturalResponse() {
        return naturalResponse;
    }
    
    // Setters
    public void setAction(String action) {
        this.action = action;
    }
    
    public void setParameters(Map<String, Object> parameters) {
        this.parameters = parameters;
    }
    
    public void setNaturalResponse(String naturalResponse) {
        this.naturalResponse = naturalResponse;
    }
    
    @Override
    public String toString() {
        return "AIResponse{" +
                "action='" + action + '\'' +
                ", parameters=" + parameters +
                ", naturalResponse='" + naturalResponse + '\'' +
                '}';
    }
} 