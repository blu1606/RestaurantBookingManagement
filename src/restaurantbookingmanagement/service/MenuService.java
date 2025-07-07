package restaurantbookingmanagement.service;

import restaurantbookingmanagement.model.MenuItem;
import java.util.List;
import java.util.ArrayList;
import restaurantbookingmanagement.service.fileservice.MenuItemFileService;

public class MenuService {
    private final MenuItemFileService menuItemFileService;

    public MenuService() {
        this.menuItemFileService = new MenuItemFileService();
    }

    public List<MenuItem> getAllMenuItems() {
        return new ArrayList<>(menuItemFileService.readMenuItemsFromFile());
    }

    public MenuItem findMenuItemByName(String name) {
        return menuItemFileService.readMenuItemsFromFile().stream()
                .filter(item -> item.getName().toLowerCase().contains(name.toLowerCase()))
                .findFirst()
                .orElse(null);
    }

    public MenuItem findMenuItemById(int itemId) {
        return menuItemFileService.readMenuItemsFromFile().stream()
                .filter(item -> item.getItemId() == itemId)
                .findFirst()
                .orElse(null);
    }

    public MenuItem addMenuItem(String name, double price, String description) {
        List<MenuItem> menuItems = menuItemFileService.readMenuItemsFromFile();
        int nextItemId = menuItems.stream()
                .mapToInt(MenuItem::getItemId)
                .max()
                .orElse(0) + 1;
        MenuItem newItem = new MenuItem(nextItemId, name, price, description);
        menuItems.add(newItem);
        menuItemFileService.writeMenuItemsToFile(menuItems);
        return newItem;
    }

    public boolean deleteMenuItem(int itemId) {
        List<MenuItem> menuItems = menuItemFileService.readMenuItemsFromFile();
        MenuItem itemToDelete = menuItems.stream()
                .filter(item -> item.getItemId() == itemId)
                .findFirst()
                .orElse(null);
        if (itemToDelete != null) {
            menuItems.remove(itemToDelete);
            menuItemFileService.writeMenuItemsToFile(menuItems);
            return true;
        }
        return false;
    }

    public boolean updateMenuItem(int id, String newName, String priceStr, String newDesc) {
        List<MenuItem> items = menuItemFileService.readMenuItemsFromFile();
        MenuItem item = null;
        for (MenuItem mi : items) {
            if (mi.getItemId() == id) {
                item = mi;
                break;
            }
        }
        if (item == null) return false;
        if (newName != null && !newName.isEmpty()) item.setName(newName);
        if (priceStr != null && !priceStr.isEmpty()) {
            try {
                item.setPrice(Double.parseDouble(priceStr));
            } catch (Exception e) {
                return false;
            }
        }
        if (newDesc != null && !newDesc.isEmpty()) item.setDescription(newDesc);
        menuItemFileService.writeMenuItemsToFile(items);
        return true;
    }
} 