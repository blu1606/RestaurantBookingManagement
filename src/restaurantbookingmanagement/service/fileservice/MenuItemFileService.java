package restaurantbookingmanagement.service.fileservice;

import restaurantbookingmanagement.model.MenuItem;
import java.util.List;
import java.util.ArrayList;
import java.io.*;
import java.nio.charset.StandardCharsets;
import com.google.gson.reflect.TypeToken;

public class MenuItemFileService extends FileServiceBase {
    private static final String MENU_ITEMS_FILE = "menu_items.json";
    public synchronized List<MenuItem> readMenuItemsFromFile() {
        try {
            File file = new File(DATA_DIR, MENU_ITEMS_FILE);
            if (!file.exists()) {
                return new ArrayList<>();
            }
            try (Reader reader = new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8)) {
                java.lang.reflect.Type listType = new TypeToken<List<MenuItem>>(){}.getType();
                List<MenuItem> menuItems = gson.fromJson(reader, listType);
                return menuItems != null ? menuItems : new ArrayList<>();
            }
        } catch (Exception e) {
            System.err.println("Error reading menu items from file: " + e.getMessage());
            return new ArrayList<>();
        }
    }
    public synchronized void writeMenuItemsToFile(List<MenuItem> menuItems) {
        try {
            File file = new File(DATA_DIR, MENU_ITEMS_FILE);
            try (Writer writer = new OutputStreamWriter(new FileOutputStream(file), StandardCharsets.UTF_8)) {
                gson.toJson(menuItems, writer);
            }
            notifyAIAgentOfChange();
        } catch (Exception e) {
            System.err.println("Error writing menu items to file: " + e.getMessage());
        }
    }
} 