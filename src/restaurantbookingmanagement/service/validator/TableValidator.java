package restaurantbookingmanagement.service.validator;

import restaurantbookingmanagement.model.Table;

public class TableValidator {
    public static boolean isValid(Table table) {
        return table != null &&
               table.getTableId() > 0 &&
               table.getCapacity() > 0 &&
               table.getStatus() != null;
    }
    public static String validate(Table table) {
        if (table == null) return "Table is null";
        if (table.getTableId() <= 0) return "Table ID must be > 0";
        if (table.getCapacity() <= 0) return "Capacity must be > 0";
        if (table.getStatus() == null) return "Status is required";
        return null;
    }
} 