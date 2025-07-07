package restaurantbookingmanagement.service.fileservice;

import restaurantbookingmanagement.model.Customer;
import java.util.List;
import java.util.ArrayList;
import java.io.*;
import java.nio.charset.StandardCharsets;
import com.google.gson.reflect.TypeToken;

public class CustomerFileService extends FileServiceBase {
    private static final String CUSTOMERS_FILE = "customers.json";
    public synchronized List<Customer> readCustomersFromFile() {
        try {
            File file = new File(DATA_DIR, CUSTOMERS_FILE);
            if (!file.exists()) {
                return new ArrayList<>();
            }
            try (Reader reader = new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8)) {
                java.lang.reflect.Type listType = new TypeToken<List<Customer>>(){}.getType();
                List<Customer> customers = gson.fromJson(reader, listType);
                return customers != null ? customers : new ArrayList<>();
            }
        } catch (Exception e) {
            System.err.println("Error reading customers from file: " + e.getMessage());
            return new ArrayList<>();
        }
    }
    public synchronized void writeCustomersToFile(List<Customer> customers) {
        try {
            File file = new File(DATA_DIR, CUSTOMERS_FILE);
            try (Writer writer = new OutputStreamWriter(new FileOutputStream(file), StandardCharsets.UTF_8)) {
                gson.toJson(customers, writer);
            }
            notifyAIAgentOfChange();
        } catch (Exception e) {
            System.err.println("Error writing customers to file: " + e.getMessage());
        }
    }
} 