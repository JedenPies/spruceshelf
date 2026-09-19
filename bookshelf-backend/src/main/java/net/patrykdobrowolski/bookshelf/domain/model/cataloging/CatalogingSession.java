package net.patrykdobrowolski.bookshelf.domain.model.cataloging;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import net.patrykdobrowolski.bookshelf.domain.AggregateRoot;
import net.patrykdobrowolski.bookshelf.domain.exception.DraftBookNotFoundException;
import net.patrykdobrowolski.bookshelf.domain.model.event.DraftBookCreatedEvent;
import net.patrykdobrowolski.bookshelf.domain.model.event.DraftBookUpdatedEvent;
import net.patrykdobrowolski.bookshelf.domain.model.event.DraftBooksDeletedEvent;
import net.patrykdobrowolski.bookshelf.domain.model.value.BookDetails;
import net.patrykdobrowolski.bookshelf.domain.model.value.ISBN;
import net.patrykdobrowolski.bookshelf.domain.model.value.Modifier;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

@Builder @AllArgsConstructor(access = lombok.AccessLevel.PRIVATE)
@Getter
public class CatalogingSession extends AggregateRoot {

    private UUID id;
    private Instant createdAt;
    private Instant lastUse;
    private List<DraftBook> draftBooks;
    private UUID exportId;

    public static CatalogingSession createNew() {
        Instant now = Instant.now();
        return CatalogingSession.builder()
                .draftBooks(new ArrayList<>())
                .id(UUID.randomUUID())
                .createdAt(now)
                .lastUse(now)
                .build();
    }

    public DraftBook findDraftBookById(UUID draftBookId) throws DraftBookNotFoundException {
        return draftBooks.stream().filter(draftBook -> Objects.equals(draftBook.getId(), draftBookId)).findFirst().orElseThrow(DraftBookNotFoundException::new);
    }

    public DraftBook createNewDraftBook(ISBN isbn) {
        touch();
        DraftBook newDraftBook = DraftBook.createNew(isbn, this.id);
        DraftBook foundDraftBook = findOldestDraftBookByIsbn(isbn);
        Optional.ofNullable(foundDraftBook).ifPresent(newDraftBook::copyDetails);
        draftBooks.add(newDraftBook);
        registerEvent(DraftBookCreatedEvent.of(this, newDraftBook));
        return newDraftBook;
    }

    public void removeDraftBooks(List<UUID> draftBooksIds) {
        touch();
        List<DraftBook> draftBooks = this.draftBooks.stream().filter(draftBook -> draftBooksIds.contains(draftBook.getId())).toList();
        draftBooks.forEach(this.draftBooks::remove);
        registerEvent(DraftBooksDeletedEvent.of(this, draftBooks));
    }

    public DraftBook updateDraftBook(UUID draftBookId, BookDetails newDetails) throws DraftBookNotFoundException {
        touch();
        DraftBook draftBook = findDraftBookById(draftBookId);
        draftBook.setBookDetails(newDetails.withSources(draftBook.getBookDetails().sources()), Modifier.USER);
        registerEvent(DraftBookUpdatedEvent.of(this, draftBook));
        return draftBook;
    }

    public void markDraftBookFetching(UUID draftBookId) throws DraftBookNotFoundException {
        touch();
        DraftBook draftBook = findDraftBookById(draftBookId);
        draftBook.markFetching();
        registerEvent(DraftBookUpdatedEvent.of(this, draftBook));
    }

    public void markDraftBookFailed(UUID draftBookId) throws DraftBookNotFoundException {
        touch();
        findDraftBookById(draftBookId).markFailed();
        registerEvent(DraftBookUpdatedEvent.of(this, findDraftBookById(draftBookId)));
    }

    public void markDraftBookNotFound(UUID draftBookId) throws DraftBookNotFoundException {
        touch();
        findDraftBookById(draftBookId).markNotFound();
        registerEvent(DraftBookUpdatedEvent.of(this, findDraftBookById(draftBookId)));
    }

    public void setDraftBookBookDetails(UUID draftBookId, BookDetails details, Modifier modifier) throws DraftBookNotFoundException {
        touch();
        findDraftBookById(draftBookId).setBookDetails(details, modifier);
        registerEvent(DraftBookUpdatedEvent.of(this, findDraftBookById(draftBookId)));
    }

    private DraftBook findOldestDraftBookByIsbn(ISBN isbn) {
        return draftBooks.stream()
                .filter(draftBook -> Objects.equals(draftBook.getIsbn(), isbn))
                .min(Comparator.comparing(DraftBook::getCreatedAt)).orElse(null);
    }

    private void touch() {
        this.lastUse = Instant.now();
    }
}
