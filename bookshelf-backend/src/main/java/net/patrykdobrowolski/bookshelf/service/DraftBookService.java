package net.patrykdobrowolski.bookshelf.service;

import jakarta.inject.Named;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import net.patrykdobrowolski.bookshelf.domain.exception.CatalogingSessionNotFoundException;
import net.patrykdobrowolski.bookshelf.domain.exception.DraftBookNotFoundException;
import net.patrykdobrowolski.bookshelf.domain.model.cataloging.CatalogingSession;
import net.patrykdobrowolski.bookshelf.domain.model.cataloging.DraftBook;
import net.patrykdobrowolski.bookshelf.domain.model.value.BookDetails;
import net.patrykdobrowolski.bookshelf.domain.model.value.ISBN;
import net.patrykdobrowolski.bookshelf.domain.port.BookDetailsAsyncFetcherPort;
import net.patrykdobrowolski.bookshelf.domain.port.DraftBookServicePort;

import java.util.List;
import java.util.UUID;

@Named
@RequiredArgsConstructor
public class DraftBookService implements DraftBookServicePort {

    private final CatalogingSessionService sessionService;
    private final BookDetailsAsyncFetcherPort bookDetailsFetcher;

    @Transactional
    @Override
    public void retryDraftBookFetch(UUID sessionId, UUID draftBookId) throws DraftBookNotFoundException, CatalogingSessionNotFoundException {
        CatalogingSession catalogingSession = sessionService.findById(sessionId);
        DraftBook draftBook = catalogingSession.findDraftBookById(draftBookId);
        bookDetailsFetcher.fetchBookDetails(catalogingSession, draftBook);
    }

    @Transactional
    @Override
    public List<DraftBook> getDraftBooks(UUID sessionId) throws CatalogingSessionNotFoundException {
        CatalogingSession catalogingSession = sessionService.findById(sessionId);
        return catalogingSession.getDraftBooks();
    }

    @Transactional
    @Override
    public DraftBook createDraftBook(UUID sessionId, String isbn) throws CatalogingSessionNotFoundException {
        CatalogingSession catalogingSession = sessionService.findById(sessionId);
        DraftBook newDraftBook = catalogingSession.createNewDraftBook(new ISBN(isbn));
        sessionService.save(catalogingSession);
        return newDraftBook;

    }

    @Transactional
    @Override
    public DraftBook updateDraftBook(UUID sessionId, UUID draftBookId, BookDetails newDetails) throws DraftBookNotFoundException, CatalogingSessionNotFoundException {
        CatalogingSession catalogingSession = sessionService.findById(sessionId);
        DraftBook result = catalogingSession.updateDraftBook(draftBookId, newDetails);
        sessionService.save(catalogingSession);
        return result;
    }

    @Override
    public void deleteDraftBooks(UUID sessionId, List<UUID> draftBookIds) throws CatalogingSessionNotFoundException {
        CatalogingSession catalogingSession = sessionService.findById(sessionId);
        catalogingSession.removeDraftBooks(draftBookIds);
        sessionService.save(catalogingSession);
    }
}
