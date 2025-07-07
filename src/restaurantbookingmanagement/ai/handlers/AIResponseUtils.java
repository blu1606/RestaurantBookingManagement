package restaurantbookingmanagement.ai.handlers;

import java.util.Map;
import java.util.List;

public class AIResponseUtils {
    public static String getString(Map<String, Object> params, String key) {
        if (params != null && params.containsKey(key)) {
            Object val = params.get(key);
            if (val instanceof String) return (String) val;
            if (val != null) return val.toString();
        }
        return null;
    }
    public static Integer getInt(Map<String, Object> params, String key) {
        if (params != null && params.containsKey(key)) {
            Object val = params.get(key);
            if (val instanceof Integer) return (Integer) val;
            if (val instanceof String) {
                try { return Integer.parseInt((String) val); } catch (Exception e) { return null; }
            }
        }
        return null;
    }
    public static Double getDouble(Map<String, Object> params, String key) {
        if (params != null && params.containsKey(key)) {
            Object val = params.get(key);
            if (val instanceof Double) return (Double) val;
            if (val instanceof Integer) return ((Integer) val).doubleValue();
            if (val instanceof String) {
                try { return Double.parseDouble((String) val); } catch (Exception e) { return null; }
            }
        }
        return null;
    }
    @SuppressWarnings("unchecked")
    public static List<Map<String, Object>> getList(Map<String, Object> params, String key) {
        if (params != null && params.containsKey(key)) {
            Object val = params.get(key);
            if (val instanceof List) return (List<Map<String, Object>>) val;
        }
        return null;
    }
    public static boolean getBoolean(Map<String, Object> params, String key) {
        if (params != null && params.containsKey(key)) {
            Object val = params.get(key);
            if (val instanceof Boolean) return (Boolean) val;
            if (val instanceof String) return Boolean.parseBoolean((String) val);
        }
        return false;
    }
} 