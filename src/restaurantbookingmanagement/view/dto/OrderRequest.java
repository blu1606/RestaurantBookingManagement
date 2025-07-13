package restaurantbookingmanagement.view.dto;

import restaurantbookingmanagement.model.Booking;

public class OrderRequest {
    private final Booking booking;
    private final String itemInput; // tên hoặc ID món
    private final int quantity;

    public OrderRequest(Booking booking, String itemInput, int quantity) {
        this.booking = booking;
        this.itemInput = itemInput;
        this.quantity = quantity;
    }

    public Booking getBooking() { return booking; }
    public String getItemInput() { return itemInput; }
    public int getQuantity() { return quantity; }
} 