package restaurantbookingmanagement.service.fileservice;

import restaurantbookingmanagement.model.Table;
import restaurantbookingmanagement.model.TableStatus;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.io.*;
import java.nio.charset.StandardCharsets;
import com.google.gson.reflect.TypeToken;

public class TableFileService extends FileServiceBase {
    private static final String TABLES_FILE = "tables.json";
    public synchronized List<Table> readTablesFromFile() {
        try {
            File file = new File(DATA_DIR, TABLES_FILE);
            if (!file.exists()) {
                return new ArrayList<>();
            }
            try (Reader reader = new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8)) {
                java.lang.reflect.Type listType = new TypeToken<List<Map<String, Object>>>(){}.getType();
                List<Map<String, Object>> rawTables = gson.fromJson(reader, listType);
                if (rawTables == null) return new ArrayList<>();
                List<Table> tables = new ArrayList<>();
                for (Map<String, Object> rawTable : rawTables) {
                    Table table = new Table();
                    if (rawTable.containsKey("tableId")) {
                        table.setTableId(((Number) rawTable.get("tableId")).intValue());
                    }
                    if (rawTable.containsKey("capacity")) {
                        table.setCapacity(((Number) rawTable.get("capacity")).intValue());
                    }
                    if (rawTable.containsKey("status")) {
                        table.setStatus(TableStatus.valueOf((String) rawTable.get("status")));
                    }
                    if (rawTable.containsKey("orderIds")) {
                        List<?> orderIdsRaw = (List<?>) rawTable.get("orderIds");
                        List<Integer> orderIds = new ArrayList<>();
                        for (Object d : orderIdsRaw) {
                            if (d instanceof Number) {
                                orderIds.add(((Number) d).intValue());
                            }
                        }
                        table.setOrderIds(orderIds);
                    }
                    tables.add(table);
                }
                return tables;
            }
        } catch (Exception e) {
            System.err.println("Error reading tables from file: " + e.getMessage());
            return new ArrayList<>();
        }
    }
    public synchronized void writeTablesToFile(List<Table> tables) {
        try {
            File file = new File(DATA_DIR, TABLES_FILE);
            List<Map<String, Object>> jsonTables = new ArrayList<>();
            for (Table table : tables) {
                Map<String, Object> jsonTable = new java.util.HashMap<>();
                jsonTable.put("tableId", table.getTableId());
                jsonTable.put("capacity", table.getCapacity());
                jsonTable.put("status", table.getStatus().name());
                jsonTable.put("orderIds", table.getOrderIds());
                jsonTables.add(jsonTable);
            }
            try (Writer writer = new OutputStreamWriter(new FileOutputStream(file), StandardCharsets.UTF_8)) {
                gson.toJson(jsonTables, writer);
            }
            notifyAIAgentOfChange();
        } catch (Exception e) {
            System.err.println("Error writing tables to file: " + e.getMessage());
        }
    }
} 