package restaurantbookingmanagement.utils;

public class DebugUtil {
    private static boolean debug = false;

    public static boolean isDebug() {
        return debug;
    }

    public static void setDebug(boolean value) {
        debug = value;
    }

    public static void toggleDebug() {
        debug = !debug;
    }

    public static void debugPrint(String msg) {
        if (debug) {
            System.out.println(msg);
        }
    }

    public static void disableDebug() {
        debug = false;
    }
} 