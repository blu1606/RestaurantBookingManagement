package restaurantbookingmanagement.service.fileservice;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import restaurantbookingmanagement.ai.AIAgentConnector;

public abstract class FileServiceBase {
    protected static final String DATA_DIR = "data";
    protected final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    private final AIAgentConnector aiAgentConnector = new AIAgentConnector();

    protected void notifyAIAgentOfChange() {
        try {
            aiAgentConnector.notifyKnowledgeRefresh();
        } catch (Exception e) {
            System.err.println("Warning: Could not notify AI Agent of data change: " + e.getMessage());
        }
    }
} 