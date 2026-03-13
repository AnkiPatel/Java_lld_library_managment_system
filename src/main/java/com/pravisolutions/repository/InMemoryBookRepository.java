package com.pravisolutions.repository;

import com.pravisolutions.models.Book;
import com.pravisolutions.models.BookItem;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * In-memory implementation of BookRepository.
 *
 * WHY TWO SEPARATE MAPS (books + bookItems):
 *
 * bookMap (bookId → Book):
 *   Fast O(1) lookup when we know the bookId.
 *   Used by: getBook(), removeBook(), search (iterates values).
 *
 * bookItemMap (bookItemId → BookItem):
 *   Fast O(1) lookup when we know the bookItemId.
 *   Critical for the borrow/return/reserve flows — all of which
 *   start with a bookItemId. Without this map, we'd scan all books
 *   and all their items to find one copy — O(n*m) vs O(1).
 *
 * The BookItem is also stored inside Book.bookItems for the
 * "getAvailableItems()" query. This is intentional duplication —
 * two access patterns require two data structures.
 *
 * In a production system, this would be replaced by a database
 * repository with the same interface — zero changes to service layer.
 */

public class InMemoryBookRepository implements IBookRepository{

    private Map<String, Book> bookRepo;
    private Map<String, BookItem> bookItemRepo;

    public InMemoryBookRepository() {
        bookRepo = new HashMap<>();
        bookItemRepo = new HashMap<>();
    }

    @Override
    public void addBook(Book book) {
        bookRepo.put(book.getBookId(), book);
    }

    @Override
    public void removeBook(String bookId) {
        Book book = bookRepo.remove(bookId);
        // Also remove all BookItems belonging to this book from the flat map
        if (book != null) {
            for (BookItem item : book.getBookItems()) {
                bookItemRepo.remove(item.getBookItemId());
            }
        }
    }

    @Override
    public Book findBookById(String bookId) {
        return bookRepo.get(bookId);
    }

    @Override
    public List<Book> getAllBooks() {
        return new ArrayList<>(bookRepo.values());
    }

    @Override
    public void addBookItem(BookItem bookItem) {
        bookItemRepo.put(bookItem.getBookItemId(), bookItem);
        // Also add to parent Book's internal list
        Book parentBook = bookRepo.get(bookItem.getActualBookId());
        if (parentBook != null) {
            parentBook.addBookItem(bookItem);
        }
    }

    @Override
    public void removeBookItem(String bookItemId) {
        BookItem item = bookItemRepo.remove(bookItemId);
        // Also remove from parent Book's internal list
        if (item != null) {
            Book parentBook = bookRepo.get(item.getActualBookId());
            if (parentBook != null) {
                parentBook.removeBookItem(bookItemId);
            }
        }
    }

    @Override
    public BookItem findBookItemById(String bookItemId) {
        return bookItemRepo.get(bookItemId);
    }

    @Override
    public List<BookItem> findBookItemsByBookId(String bookId) {
        Book book = bookRepo.get(bookId);
        if (book == null) {
            return new ArrayList<>();
        }
        return new ArrayList<>(book.getBookItems());
    }
}
