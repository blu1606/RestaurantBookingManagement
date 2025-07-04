/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package restaurantbookingmanagement.utils;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Scanner;

/**
 *
 * @author Blue
 */
public class InputHandler {
    
    private static final Scanner scanner = new Scanner(System.in); 
    
    public String getString(String message) {
        System.out.print(message);
        return scanner.nextLine().trim();
    }

    public int getInt(String message) {
        System.out.println(message);
        while (true) {
            try {
                return Integer.parseInt(scanner.nextLine().trim());
            } catch (NumberFormatException e) {
                System.err.println("Invalid input. Please enter a valid integer.");
            }
        }
    }

    public double getDouble(String message) {
        System.out.println(message);
        while (true) {
            try {
                return Double.parseDouble(scanner.nextLine().trim());
            } catch (NumberFormatException e) {
                System.err.println("Invalid input. Please enter a valid double.");
            }
        }
    }

    public boolean getBoolean(String message) {
        System.out.println(message + " (true/false)");
        while (true) {
            String input = scanner.nextLine().trim().toLowerCase();
            if ("true".equals(input)) {
                return true;
            } else if ("false".equals(input)) {
                return false;
            } else {
                System.err.println("Invalid input. Please enter 'true' or 'false'.");
            }
        }
    }

    public LocalDate getDate(String message) {
        System.out.println(message + " (format: dd/MM/yyyy)");
        while (true) {
            String input = scanner.nextLine().trim();
            try {
                return LocalDate.parse(input, DateTimeFormatter.ofPattern("dd/MM/yyyy"));
            } catch (Exception e) {
                System.err.println("Invalid date format. Please use dd/MM/yyyy.");
            }
        }
    }

    /**
     * Kiểm tra input có phải là lệnh hủy thao tác không
     */
    public boolean checkCancel(String input) {
        if (input == null) return false;
        String trimmed = input.trim().toLowerCase();
        return trimmed.equals("cancel") || trimmed.equals("thoat") || trimmed.equals("thoát") || trimmed.equals("exit") || trimmed.equals("back");
    }

    /**
     * Nhập chuỗi, nếu nhập lệnh hủy thì trả về null
     */
    public String getStringWithCancel(String message) {
        System.out.println(message + " (gõ 'cancel' hoặc 'thoát' để hủy)");
        String input = scanner.nextLine().trim();
        if (checkCancel(input)) return null;
        return input;
    }
}

