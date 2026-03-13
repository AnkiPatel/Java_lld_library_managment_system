package com.pravisolutions.services.searchbook;


import com.pravisolutions.models.Book;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

class TitleSearchStrategy implements IBookSearchStrategy {

    @Override
    public List<Book> search(List<Book> allBooks, String query) {
        String lq = query.toLowerCase();
        List<Book> result = new ArrayList<>();
        for (Book book : allBooks) {
            if (book.getTitle().toLowerCase().contains(lq)) {
                result.add(book);
            }
        }
        return result;
    }
}


// ─────────────────────────────────────────────────────────────────────────────
//  Concrete Strategy 2: Search by Author name (case-insensitive partial match)
// ─────────────────────────────────────────────────────────────────────────────
class AuthorSearchStrategy implements IBookSearchStrategy {
    @Override
    public List<Book> search(List<Book> allBooks, String query) {
        String lq = query.toLowerCase();
        List<Book> result = new ArrayList<>();
        for (Book book : allBooks) {
            if (book.getAuthor().toLowerCase().contains(lq)) {
                result.add(book);
            }
        }
        return result;
    }
}


// ─────────────────────────────────────────────────────────────────────────────
//  Concrete Strategy 3: Search by Subject (case-insensitive partial match)
// ─────────────────────────────────────────────────────────────────────────────
class SubjectSearchStrategy implements IBookSearchStrategy {
    //NOTE: Usage of java stream.. just for learning
    @Override
    public List<Book> search(List<Book> allBooks, String query) {
        String lq = query.toLowerCase();

        return allBooks.stream().filter(b ->b.getSubject().toLowerCase().equals(lq))
                .toList();
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  Concrete Strategy 4: Search by Publication Date (exact year match)
// ─────────────────────────────────────────────────────────────────────────────
class PublicationDateSearchStrategy implements IBookSearchStrategy {
    @Override
    public List<Book> search(List<Book> allBooks, String query) {
        String lq = query.toLowerCase();
        List<Book> result = new ArrayList<>();
        // query is expected as "YYYY" e.g. "2020"

        int year = Integer.parseInt(query.trim());
        for(Book b: allBooks) {
            if(b.getPublicationDate() != null && b.getPublicationDate().equals(lq)) {
                result.add(b);
            }
        }
        return result;
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  Concrete Strategy 5: Search by ISBN (exact match)
// ─────────────────────────────────────────────────────────────────────────────
class IsbnSearchStrategy implements IBookSearchStrategy {
    @Override
    public List<Book> search(List<Book> allBooks, String query) {
        return allBooks.stream()
                .filter(b -> b.getIsbn().equals(query.trim()))
                .collect(Collectors.toList());
    }
}


// ─────────────────────────────────────────────────────────────────────────────
//  SearchStrategy Factory — maps SearchType enum to concrete strategy
// ─────────────────────────────────────────────────────────────────────────────

/**
 * ── FACTORY PATTERN ───────────────────────────────────────────────────────────
 *
 * Creates the correct SearchStrategy based on SearchType.
 * Catalog uses this factory so it doesn't need to know about concrete strategies.
 *
 * Adding a new search type: create a new Strategy class + add enum constant.
 * No changes to Catalog required (Open/Closed Principle).
 */
public class SearchStrategyFactory {

    public enum SearchType {
        TITLE, AUTHOR, SUBJECT, PUBLICATION_DATE, ISBN
    }

    public static IBookSearchStrategy getStrategy(SearchType type) {
        return switch (type) {
            case TITLE            -> new TitleSearchStrategy();
            case AUTHOR           -> new AuthorSearchStrategy();
            case SUBJECT          -> new SubjectSearchStrategy();
            case PUBLICATION_DATE -> new PublicationDateSearchStrategy();
            case ISBN             -> new IsbnSearchStrategy();
        };
    }

    private SearchStrategyFactory() {} // Utility class — no instantiation
}
