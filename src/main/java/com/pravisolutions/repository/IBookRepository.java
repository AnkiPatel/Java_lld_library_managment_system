package com.pravisolutions.repository;

import com.pravisolutions.models.BookItem;
import com.pravisolutions.models.Book;

import java.util.List;

/**
 * Repository contract for Book and BookItem storage.
 *
 * WHY REPOSITORY PATTERN:
 * Services should not care WHERE data is stored — in-memory, a database,
 * or a file. By coding to this interface, we can swap InMemoryBookRepository
 * for a DatabaseBookRepository without touching any service class.
 *
 * Note: Book and BookItem are kept in the same repository because
 * BookItem is always accessed in the context of its parent Book.
 * Splitting them would force unnecessary cross-repository joins.
 */
public interface IBookRepository {
    // -----------------------------------------------------------------------
    // Book operations
    // -----------------------------------------------------------------------

    /** Add a new Book to the catalog. */
    void addBook(Book book);

    /** Remove a Book from the catalog by its unique bookId. */
    void removeBook(String bookId);

    /**
     * Find a Book by its unique bookId.
     * Returns null if not found.
     */
    Book findBookById(String bookId);

    /** Return all books in the catalog. */
    List<Book> getAllBooks();

    // -----------------------------------------------------------------------
    // BookItem operations
    // -----------------------------------------------------------------------

    /** Add a physical copy (BookItem) to the inventory. */
    void addBookItem(BookItem bookItem);

    /** Remove a physical copy from the inventory by its unique bookItemId. */
    void removeBookItem(String bookItemId);

    /**
     * Find a BookItem by its unique bookItemId.
     * Returns null if not found.
     */
    BookItem findBookItemById(String bookItemId);

    /**
     * Find all physical copies belonging to a specific Book.
     * Returns empty list if no items found.
     */
    List<BookItem> findBookItemsByBookId(String bookId);
}
