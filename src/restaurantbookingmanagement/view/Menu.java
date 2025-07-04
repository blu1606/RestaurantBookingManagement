/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package restaurantbookingmanagement.view;


import restaurantbookingmanagement.utils.InputHandler;

/**
 *
 * @author Blue
 */
public abstract class Menu {
    protected String title; 
    protected String[] options;
    protected InputHandler inputHandler = new InputHandler();
    
    public Menu(){}
    
    public Menu(String title, String[] options) {
        this.title = title; 
        this.options = options; 
    }
    
    public void display() {
        System.out.println(title);
        System.out.println("--------------------------------");
        for (int i = 0; i < options.length; i++) {
            System.out.println("[" + (i + 1) + "] " + options[i]);
        }
        System.out.println("--------------------------------");
    }
    
    public int getSelection() {
        display();
        return inputHandler.getInt("Enter your selection: ");
    }
    
    public abstract void execute(int n);
    
    public void run() {
        while (true) {
            int n = getSelection();
            if (n > options.length) break; 
            execute(n);
        }
    }
}
