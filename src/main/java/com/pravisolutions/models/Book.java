package com.pravisolutions.models;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents the logical/metadata entry for a book in the catalog (R2, R3).
 *
 * KEY DISTINCTION — Book vs BookItem:
 * - Book  = the CONCEPT  (e.g., "Clean Code" by Robert Martin, ISBN: 978-0132350884)
 * - BookItem = a PHYSICAL COPY of that book (e.g., the copy on Rack A-3 with barcode BK-001)
 *
 * One Book → Many BookItems (R4).
 *
 * WHY Book holds List<BookItem>:
 * - getAvailableItems() is a natural operation on Book ("do we have a free copy?")
 * - Avoids an extra repository lookup in the common "search then check availability" flow
 * - BookItem also stores bookId as a back-reference for reverse lookups
 *
 * publicationDate is stored as String (not Date) because requirements only say
 * "publication date" without format — String keeps it simple and portable.
 */

public class Book {
    private String bookId;
    private String isbn;
    private String title;
    private String author;
    private String subject;
    private String publicationDate;

    private List<BookItem> bookItems;

    public Book(String bookId, String isbn, String title,
                String author, String subject, String publicationDate) {
        this.bookId          = bookId;
        this.isbn            = isbn;
        this.title           = title;
        this.author          = author;
        this.subject         = subject;
        this.publicationDate = publicationDate;
        this.bookItems       = new ArrayList<>();
    }

    // -----------------------------------------------------------------------
    // BookItem management — Book owns its copies
    // -----------------------------------------------------------------------

    public void addBookItem(BookItem bitem) {
        if(bitem == null)
            throw new IllegalArgumentException("Bookitem cannot be null");
        this.bookItems.add(bitem);
    }

    public BookItem removeBookItem(String bookItemId) {
        BookItem itemRemoved = null;
        for(BookItem item : bookItems) {
             if(item.getBookItemId().equals(bookItemId)) {
                 itemRemoved = item;
                 bookItems.remove(item);
             }
         }
        return itemRemoved;
    }

    public String getBookId() {
        return bookId;
    }

    public void setBookId(String bookId) {
        this.bookId = bookId;
    }

    public String getIsbn() {
        return isbn;
    }

    public void setIsbn(String isbn) {
        this.isbn = isbn;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getAuthor() {
        return author;
    }

    public void setAuthor(String author) {
        this.author = author;
    }

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public String getPublicationDate() {
        return publicationDate;
    }

    public void setPublicationDate(String publicationDate) {
        this.publicationDate = publicationDate;
    }

    public List<BookItem> getBookItems() {
        return bookItems;
    }

    @Override
    public String toString() {
        return "Book{" +
                "bookId='" + bookId + '\'' +
                ", isbn='" + isbn + '\'' +
                ", title='" + title + '\'' +
                ", author='" + author + '\'' +
                ", subject='" + subject + '\'' +
                ", publicationDate='" + publicationDate + '\'' +
                '}';
    }
}
