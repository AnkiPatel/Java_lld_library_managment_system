package com.pravisolutions.services;

import com.pravisolutions.repository.IBookRepository;
import com.pravisolutions.models.*;
import com.pravisolutions.services.searchbook.IBookSearchStrategy;
import com.pravisolutions.services.searchbook.SearchStrategyFactory;

import java.util.List;

/**
 * Handles all Book and BookItem CRUD operations.
 *
 * RESPONSIBILITY:
 * BookService is the only place in the system that creates or modifies
 * Book and BookItem objects. Services like LendingService and
 * ReservationService READ books/items through this service — they do
 * not create or delete them.
 *
 * WHY SEPARATE FROM LendingService:
 * Adding a book and lending a book are completely different concerns.
 * Mixing them would violate Single Responsibility and make the class
 * harder to reason about during an interview.
 *
 * Librarian actions that map to this service:
 * - Add/Edit/Remove Book (R catalog management)
 * - Add/Edit/Remove BookItem
 */


public class BookService {

    private IBookRepository bookRepository;

    public BookService(IBookRepository bookRepository) {
        this.bookRepository = bookRepository;
    }

    // -----------------------------------------------------------------------
    // Book operations
    // -----------------------------------------------------------------------

    /**
     * Add a new book to the catalog.
     * bookId must be unique — caller is responsible for generating it.
     */
    public Book addBook(String bookId, String isbn, String title,
                        String author, String subject, String publicationDate) {

        if (bookRepository.findBookById(bookId) != null) {
            throw new IllegalArgumentException("Book with ID '" + bookId + "' already exists.");
        }
        Book nbook = new Book(bookId, isbn, title, author, subject,publicationDate);
        bookRepository.addBook(nbook);
        System.out.println("[BookService] Book added: " + nbook);
        return nbook;

    }

    /**
     * Update mutable metadata of an existing book.
     * bookId and isbn are immutable after creation.
     */
    public void editBook(String bookId, String title, String author,
                         String subject, String publicationDate) {
        Book book = getBook(bookId);  // throws if not found
        book.setTitle(title);
        book.setAuthor(author);
        book.setSubject(subject);
        book.setPublicationDate(publicationDate);
        System.out.println("[BookService] Book updated: " + book);
    }

    /**
     * Remove a book and all its physical copies from the catalog.
     * In a real system, we would first check that no copies are currently borrowed.
     */
    public void removeBook(String bookId) {
        getBook(bookId); // validate existence
        bookRepository.removeBook(bookId);
        System.out.println("[BookService] Book removed: " + bookId);
    }

    // -----------------------------------------------------------------------
    // BookItem operations
    // -----------------------------------------------------------------------

    /**
     * Add a new physical copy of an existing book.
     * The parent book must already exist in the catalog.
     */
    public BookItem addBookItem(String bookId, String bookItemId, String rackLocation) {
        getBook(bookId); // validate parent book exists
        if (bookRepository.findBookItemById(bookItemId) != null) {
            throw new IllegalArgumentException(
                    "BookItem with ID '" + bookItemId + "' already exists.");
        }
        BookItem item = new BookItem(bookItemId, bookId, rackLocation);
        bookRepository.addBookItem(item);
        System.out.println("[BookService] BookItem added: " + item);
        return item;
    }

    /**
     * Update the rack location of a physical copy.
     * This is the only mutable field on BookItem that librarians can change.
     */
    public void editBookItem(String bookItemId, String rackLocation) {
        BookItem item = getBookItem(bookItemId); // throws if not found
        item.setRackLocation(rackLocation);
        System.out.println("[BookService] BookItem updated: " + item);
    }

    /**
     * Remove a physical copy from inventory.
     * In a real system, we would check the item is not currently borrowed.
     */
    public void removeBookItem(String bookItemId) {
        getBookItem(bookItemId); // validate existence
        bookRepository.removeBookItem(bookItemId);
        System.out.println("[BookService] BookItem removed: " + bookItemId);
    }

    // -----------------------------------------------------------------------
    // Query methods — used by other services
    // -----------------------------------------------------------------------

    /**
     * Find a Book by ID. Throws if not found.
     * Centralising the null check here means other services never need to
     * write "if book == null throw..." — they just call getBook().
     */
    public Book getBook(String bookId) {
        Book book = bookRepository.findBookById(bookId);
        if (book == null) {
            throw new IllegalArgumentException("Book not found: " + bookId);
        }
        return book;
    }

    /**
     * Find a BookItem by ID. Throws if not found.
     */
    public BookItem getBookItem(String bookItemId) {
        BookItem item = bookRepository.findBookItemById(bookItemId);
        if (item == null) {
            throw new IllegalArgumentException("BookItem not found: " + bookItemId);
        }
        return item;
    }

    public List<BookItem> findBookItemsByBookId(String bookId) {
        return bookRepository.findBookItemsByBookId(bookId);
    }

    public List<Book> getAllBooks() {
        return bookRepository.getAllBooks();
    }

    // ── Search (Strategy Pattern) ─────────────────────────────────────────────

    /**
     * Search book catalog using a specific search type and query string.
     *
     * Usage:
     *   catalogService.search(SearchType.TITLE, "Clean Code");
     *   catalogService.search(SearchType.AUTHOR, "Martin");
     *   catalogService.search(SearchType.PUBLICATION_DATE, "2008");
     */

    public List<Book> serachBook(SearchStrategyFactory.SearchType type, String query) {
        IBookSearchStrategy searchStrategy = SearchStrategyFactory.getStrategy(type);
        return searchStrategy.search(this.getAllBooks(), query);
    }

    /**
     * Search using an externally provided strategy (for extensibility).
     */
    /*public List<Book> search(IBookSearchStrategy customStrategy, String query) {
        return customStrategy.search(this.getAllBooks(), query);
    }*/


}
