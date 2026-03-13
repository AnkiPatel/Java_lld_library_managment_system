package com.pravisolutions.services.searchbook;

import com.pravisolutions.models.Book;

import java.util.List;

/**
 * Contract for catalog search operations.
 *
 * specifies single-field search only: title, author, subject,
 * or publication date.
 *
 * Context:   Catalog.search(BookSearchCriteria)
 * Strategy:  SearchStrategyFactory (this interface)
 * Concrete:  TitleSearchStrategy, AuthorSearchStrategy,
 *            SubjectSearchStrategy, PublicationDateSearchStrategy
 *
 * Why Strategy here?
 *   - Different search axes have different matching logic
 *   - New search types (e.g. ISBN search) can be added without modifying Catalog
 *   - Follows Open/Closed Principle
 */
public interface IBookSearchStrategy {

    List<Book> search(List<Book> allBooks, String query);
}
