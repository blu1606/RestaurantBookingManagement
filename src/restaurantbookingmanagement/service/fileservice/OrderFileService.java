package restaurantbookingmanagement.service.fileservice;

import restaurantbookingmanagement.model.Order;
import java.util.List;
import java.util.ArrayList;
import java.io.*;
import java.nio.charset.StandardCharsets;
import com.google.gson.reflect.TypeToken;

public class OrderFileService extends FileServiceBase {
    private static final String ORDERS_FILE = "orders.json";
    public synchronized List<Order> readOrdersFromFile() {
        try {
            File file = new File(DATA_DIR, ORDERS_FILE);
            if (!file.exists()) {
                return new ArrayList<>();
            }
            try (Reader reader = new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8)) {
                java.lang.reflect.Type listType = new TypeToken<List<Order>>(){}.getType();
                List<Order> orders = gson.fromJson(reader, listType);
                return orders != null ? orders : new ArrayList<>();
            }
        } catch (Exception e) {
            System.err.println("Error reading orders from file: " + e.getMessage());
            return new ArrayList<>();
        }
    }
    public synchronized void writeOrdersToFile(List<Order> orders) {
        try {
            File file = new File(DATA_DIR, ORDERS_FILE);
            try (Writer writer = new OutputStreamWriter(new FileOutputStream(file), StandardCharsets.UTF_8)) {
                gson.toJson(orders, writer);
            }
            notifyAIAgentOfChange();
        } catch (Exception e) {
            System.err.println("Error writing orders to file: " + e.getMessage());
        }
    }
} 