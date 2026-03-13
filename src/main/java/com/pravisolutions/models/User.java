package com.pravisolutions.models;

/**
 * Abstract base class for all users in the system (R5).
 *
 * WHY ABSTRACT CLASS (not interface):
 * Member and Librarian share actual STATE — name, email, phone, password,
 * libraryCard. An interface cannot hold state. Using an abstract class here
 * avoids duplicating these fields in every subclass (DRY principle).
 *
 * getRole() is abstract because:
 * - It has no shared implementation — each subclass knows its own role.
 * - It forces every subclass to declare its identity explicitly.
 *
 * WHY NOT store password as plain text in a real system:
 * In production, passwords would be hashed (e.g., bcrypt).
 * For this LLD, we store as String to keep it framework-free and portable.
 */

public abstract class User {

    protected String name;
    protected String email;
    protected String phone;
    protected String password;
    protected LibraryCard libraryCard;

    public User(String name, String email, String phone,
                String password, LibraryCard libraryCard) {
        this.name        = name;
        this.email       = email;
        this.phone       = phone;
        this.password    = password;
        this.libraryCard = libraryCard;
    }

    // -----------------------------------------------------------------------
    // Abstract method — subclasses must identify their role
    // -----------------------------------------------------------------------

    public abstract String getRole();

    // -----------------------------------------------------------------------
    // Getters
    // -----------------------------------------------------------------------

    public String getName() {
        return name;
    }

    public String getEmail() {
        return email;
    }

    public String getPhone() {
        return phone;
    }

    public LibraryCard getLibraryCard() {
        return libraryCard;
    }

    // -----------------------------------------------------------------------
    // Setters — only mutable fields exposed (cardNumber is immutable)
    // -----------------------------------------------------------------------

    public void setName(String name) {
        this.name = name;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    @Override
    public String toString() {
        return getRole() + "{name='" + name + "', card=" + libraryCard.getCardNumber() + "}";
    }

}
