package restaurantbookingmanagement.service;

import restaurantbookingmanagement.model.Table;
import restaurantbookingmanagement.model.TableStatus;
import java.util.List;
import java.util.ArrayList;
import java.util.stream.Collectors;
import restaurantbookingmanagement.service.fileservice.TableFileService;

// Design Pattern: Dependency Injection, State
// Purpose: Inject TableFileService; manage Table status using State pattern (TableStatus).

public class TableService {
    private final TableFileService tableFileService;

    public TableService() {
        this.tableFileService = new TableFileService();
    }

    public List<Table> getAllTables() {
        List<Table> tables = new ArrayList<>(tableFileService.readTablesFromFile());
        for (Table table : tables) {
            table.syncStateWithStatus();
        }
        return tables;
    }

    public List<Table> getAvailableTables() {
        return tableFileService.readTablesFromFile().stream()
                .filter(table -> table.getStatus() == TableStatus.AVAILABLE)
                .collect(Collectors.toList());
    }

    public List<Table> getAvailableTablesForCapacity(int capacity) {
        return tableFileService.readTablesFromFile().stream()
                .filter(table -> table.getStatus() == TableStatus.AVAILABLE && table.getCapacity() >= capacity)
                .collect(Collectors.toList());
    }

    public Table findAvailableTable(int capacity) {
        return tableFileService.readTablesFromFile().stream()
                .filter(table -> table.getStatus() == TableStatus.AVAILABLE && table.getCapacity() >= capacity)
                .findFirst()
                .orElse(null);
    }

    public Table addTable(int capacity) {
        List<Table> tables = tableFileService.readTablesFromFile();
        int nextTableId = tables.stream()
                .mapToInt(Table::getTableId)
                .max()
                .orElse(0) + 1;
        Table newTable = new Table(nextTableId, capacity);
        tables.add(newTable);
        tableFileService.writeTablesToFile(tables);
        return newTable;
    }

    public boolean updateTable(int id, String newCapacity, String newStatus) {
        List<Table> tables = tableFileService.readTablesFromFile();
        Table table = null;
        for (Table t : tables) if (t.getTableId() == id) table = t;
        if (table == null) return false;
        if (newCapacity != null && !newCapacity.isEmpty()) {
            try { table.setCapacity(Integer.parseInt(newCapacity)); } catch (Exception e) { return false; }
        }
        if (newStatus != null && !newStatus.isEmpty()) {
            try {
                TableStatus statusEnum = TableStatus.valueOf(newStatus);
                table.transitionTo(Table.TableStateFactory.fromStatus(statusEnum));
            } catch (Exception e) { return false; }
        }
        for (int i = 0; i < tables.size(); i++) if (tables.get(i).getTableId() == id) tables.set(i, table);
        tableFileService.writeTablesToFile(tables);
        return true;
    }

    public boolean deleteTable(int id) {
        List<Table> tables = tableFileService.readTablesFromFile();
        boolean found = false;
        for (int i = 0; i < tables.size(); i++) {
            if (tables.get(i).getTableId() == id) {
                tables.remove(i);
                found = true;
                break;
            }
        }
        if (found) tableFileService.writeTablesToFile(tables);
        return found;
    }

    public List<Table> searchTables(String keyword) {
        List<Table> tables = tableFileService.readTablesFromFile();
        List<Table> result = new ArrayList<>();
        for (Table t : tables) {
            if (String.valueOf(t.getTableId()).equals(keyword) || String.valueOf(t.getCapacity()).equals(keyword) || t.getStatus().name().equalsIgnoreCase(keyword)) {
                result.add(t);
            }
        }
        return result;
    }

    public void writeTablesToFile(List<Table> tables) {
        tableFileService.writeTablesToFile(tables);
    }
} 