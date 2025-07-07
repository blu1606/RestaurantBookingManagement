package restaurantbookingmanagement.service.fileservice;

import restaurantbookingmanagement.model.Booking;
import java.util.List;
import java.util.ArrayList;
import java.io.*;
import java.nio.charset.StandardCharsets;
import com.google.gson.reflect.TypeToken;

public class BookingFileService extends FileServiceBase {
    private static final String BOOKINGS_FILE = "bookings.json";
    public synchronized List<Booking> readBookingsFromFile() {
        try {
            File file = new File(DATA_DIR, BOOKINGS_FILE);
            if (!file.exists()) {
                return new ArrayList<>();
            }
            try (Reader reader = new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8)) {
                java.lang.reflect.Type listType = new TypeToken<List<Booking>>(){}.getType();
                List<Booking> bookings = gson.fromJson(reader, listType);
                return bookings != null ? bookings : new ArrayList<>();
            }
        } catch (Exception e) {
            System.err.println("Error reading bookings from file: " + e.getMessage());
            return new ArrayList<>();
        }
    }
    public synchronized void writeBookingsToFile(List<Booking> bookings) {
        try {
            File file = new File(DATA_DIR, BOOKINGS_FILE);
            try (Writer writer = new OutputStreamWriter(new FileOutputStream(file), StandardCharsets.UTF_8)) {
                gson.toJson(bookings, writer);
            }
            notifyAIAgentOfChange();
        } catch (Exception e) {
            System.err.println("Error writing bookings to file: " + e.getMessage());
        }
    }
} 