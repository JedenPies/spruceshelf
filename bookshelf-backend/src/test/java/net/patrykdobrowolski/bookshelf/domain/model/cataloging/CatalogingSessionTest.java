package net.patrykdobrowolski.bookshelf.domain.model.cataloging;

import net.patrykdobrowolski.bookshelf.domain.exception.DraftBookNotFoundException;
import net.patrykdobrowolski.bookshelf.domain.model.event.BusinessEvent;
import net.patrykdobrowolski.bookshelf.domain.model.event.DraftBookCreatedEvent;
import net.patrykdobrowolski.bookshelf.domain.model.event.DraftBookUpdatedEvent;
import net.patrykdobrowolski.bookshelf.domain.model.event.DraftBooksDeletedEvent;
import net.patrykdobrowolski.bookshelf.domain.model.value.BookDetails;
import net.patrykdobrowolski.bookshelf.domain.model.value.DraftBookStatus;
import net.patrykdobrowolski.bookshelf.domain.model.value.ISBN;
import net.patrykdobrowolski.bookshelf.domain.model.value.Modifier;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CatalogingSessionTest {

    private final ISBN validIsbn = new ISBN("9788328302341");
    private final ISBN otherIsbn = new ISBN("9788328302342");

    /**
     * Pomocnicza metoda konsumująca zdarzenia za pomocą AggregateRoot#publishEvents.
     * Metoda publishEvents czyści listę wewnętrzną events po wywołaniu.
     */
    private List<BusinessEvent> drainEvents(CatalogingSession session) {
        List<BusinessEvent> published = new ArrayList<>();
        session.publishEvents(published::add);
        return published;
    }

    @Test
    @DisplayName("createNew() powinno poprawnie zainicjalizować nową sesję katalogowania")
    void shouldCreateNewCatalogingSession() {
        // when
        CatalogingSession session = CatalogingSession.createNew();

        // then
        assertThat(session.getId()).isNotNull();
        assertThat(session.getCreatedAt()).isNotNull();
        assertThat(session.getLastUse()).isEqualTo(session.getCreatedAt());
        assertThat(session.getDraftBooks()).isNotNull().isEmpty();
        assertThat(session.getExportId()).isNull();
        assertThat(drainEvents(session)).isEmpty();
    }

    @Nested
    @DisplayName("Tworzenie i dodawanie DraftBook")
    class CreateDraftBookTests {

        @Test
        @DisplayName("powinno dodać nową książkę ze statusem PENDING i zarejestrować zdarzenie DraftBookCreatedEvent")
        void shouldCreateNewDraftBookAndEmitEvent() {
            // given
            CatalogingSession session = CatalogingSession.createNew();

            // when
            DraftBook draftBook = session.createNewDraftBook(validIsbn);

            // then
            assertThat(session.getDraftBooks()).hasSize(1);
            assertThat(draftBook.getId()).isNotNull();
            assertThat(draftBook.getSessionId()).isEqualTo(session.getId());
            assertThat(draftBook.getIsbn()).isEqualTo(validIsbn);
            assertThat(draftBook.getStatus()).isEqualTo(DraftBookStatus.PENDING);

            List<BusinessEvent> events = drainEvents(session);
            assertThat(events)
                    .hasSize(1)
                    .first()
                    .isInstanceOf(DraftBookCreatedEvent.class);

            DraftBookCreatedEvent event = (DraftBookCreatedEvent) events.getFirst();
            assertThat(event.getDraftBook()).isEqualTo(draftBook);

            // Sprawdzenie czyszczenia kolejki po publishEvents
            assertThat(drainEvents(session)).isEmpty();
        }

        @Test
        @DisplayName("powinno skopiować szczegóły z najstarszego wpisu i nadać status DUPLICATE, gdy dodawany jest ten sam ISBN")
        void shouldCopyDetailsAndMarkAsDuplicateWhenSameIsbnIsAdded() throws DraftBookNotFoundException {
            // given
            CatalogingSession session = CatalogingSession.createNew();
            DraftBook firstDraft = session.createNewDraftBook(validIsbn);

            BookDetails initialDetails = BookDetails.builder()
                    .title("Czysty Kod")
                    .authors(List.of("Robert C. Martin"))
                    .sources(Set.of("GOOGLE"))
                    .build();

            session.setDraftBookBookDetails(firstDraft.getId(), initialDetails, Modifier.SYSTEM);
            drainEvents(session); // czyścimy dotychczasowe zdarzenia

            // when - ponowne dodanie książki z tym samym ISBN
            DraftBook duplicateDraft = session.createNewDraftBook(validIsbn);

            // then
            assertThat(session.getDraftBooks()).hasSize(2);
            assertThat(duplicateDraft.getId()).isNotEqualTo(firstDraft.getId());
            assertThat(duplicateDraft.getStatus()).isEqualTo(DraftBookStatus.DUPLICATE);
            assertThat(duplicateDraft.getBookDetails()).isEqualTo(initialDetails);

            List<BusinessEvent> events = drainEvents(session);
            assertThat(events)
                    .hasSize(1)
                    .first()
                    .isInstanceOf(DraftBookCreatedEvent.class);
        }
    }

    @Nested
    @DisplayName("Wyszukiwanie DraftBook")
    class FindDraftBookTests {

        @Test
        @DisplayName("powinno odnaleźć książkę po ID jeśli istnieje w sesji")
        void shouldFindExistingDraftBookById() throws DraftBookNotFoundException {
            // given
            CatalogingSession session = CatalogingSession.createNew();
            DraftBook createdDraft = session.createNewDraftBook(validIsbn);

            // when
            DraftBook found = session.findDraftBookById(createdDraft.getId());

            // then
            assertThat(found).isNotNull().isEqualTo(createdDraft);
        }

        @Test
        @DisplayName("powinno rzucić DraftBookNotFoundException, gdy szukane ID nie istnieje")
        void shouldThrowExceptionWhenDraftBookNotFound() {
            // given
            CatalogingSession session = CatalogingSession.createNew();

            // when & then
            assertThatThrownBy(() -> session.findDraftBookById(UUID.randomUUID()))
                    .isInstanceOf(DraftBookNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("Aktualizacje i modyfikacje stanu DraftBook")
    class UpdateDraftBookTests {

        @Test
        @DisplayName("updateDraftBook() powinno zaktualizować szczegóły, zachować oryginalne źródła i oznaczyć Modifier.USER")
        void shouldUpdateDraftBookDetailsPreservingSources() throws DraftBookNotFoundException {
            // given
            CatalogingSession session = CatalogingSession.createNew();
            DraftBook draftBook = session.createNewDraftBook(validIsbn);

            BookDetails originalDetails = BookDetails.builder()
                    .title("Stary tytuł")
                    .sources(Set.of("GOOGLE", "BN"))
                    .build();
            session.setDraftBookBookDetails(draftBook.getId(), originalDetails, Modifier.SYSTEM);
            drainEvents(session);

            BookDetails userDetails = BookDetails.builder()
                    .title("Nowy poprawiony tytuł")
                    .authors(List.of("Jan Kowalski"))
                    .sources(Set.of("INNE_ZRODLO")) // powinno zostać zignorowane
                    .build();

            // when
            DraftBook updated = session.updateDraftBook(draftBook.getId(), userDetails);

            // then
            assertThat(updated.getBookDetails().title()).isEqualTo("Nowy poprawiony tytuł");
            assertThat(updated.getBookDetails().authors()).containsExactly("Jan Kowalski");
            assertThat(updated.getBookDetails().sources()).containsExactlyInAnyOrder("GOOGLE", "BN");
            assertThat(updated.getModifiedBy()).isEqualTo(Modifier.USER);
            assertThat(updated.getStatus()).isEqualTo(DraftBookStatus.FOUND);

            List<BusinessEvent> events = drainEvents(session);
            assertThat(events).hasSize(1).first().isInstanceOf(DraftBookUpdatedEvent.class);
        }

        @Test
        @DisplayName("markDraftBookFetching() powinno zmienić status na FETCHING i wyemitować zdarzenie")
        void shouldMarkDraftBookAsFetching() throws DraftBookNotFoundException {
            // given
            CatalogingSession session = CatalogingSession.createNew();
            DraftBook draftBook = session.createNewDraftBook(validIsbn);
            drainEvents(session);

            // when
            session.markDraftBookFetching(draftBook.getId());

            // then
            assertThat(draftBook.getStatus()).isEqualTo(DraftBookStatus.FETCHING);
            assertThat(drainEvents(session)).hasSize(1).first().isInstanceOf(DraftBookUpdatedEvent.class);
        }

        @Test
        @DisplayName("markDraftBookFailed() powinno zmienić status na FAILED i wyemitować zdarzenie")
        void shouldMarkDraftBookAsFailed() throws DraftBookNotFoundException {
            // given
            CatalogingSession session = CatalogingSession.createNew();
            DraftBook draftBook = session.createNewDraftBook(validIsbn);
            drainEvents(session);

            // when
            session.markDraftBookFailed(draftBook.getId());

            // then
            assertThat(draftBook.getStatus()).isEqualTo(DraftBookStatus.FAILED);
            assertThat(drainEvents(session)).hasSize(1).first().isInstanceOf(DraftBookUpdatedEvent.class);
        }

        @Test
        @DisplayName("markDraftBookNotFound() powinno zmienić status na NOT_FOUND i wyemitować zdarzenie")
        void shouldMarkDraftBookAsNotFound() throws DraftBookNotFoundException {
            // given
            CatalogingSession session = CatalogingSession.createNew();
            DraftBook draftBook = session.createNewDraftBook(validIsbn);
            drainEvents(session);

            // when
            session.markDraftBookNotFound(draftBook.getId());

            // then
            assertThat(draftBook.getStatus()).isEqualTo(DraftBookStatus.NOT_FOUND);
            assertThat(drainEvents(session)).hasSize(1).first().isInstanceOf(DraftBookUpdatedEvent.class);
        }
    }

    @Nested
    @DisplayName("Usuwanie pozycji z sesji")
    class RemoveDraftBooksTests {

        @Test
        @DisplayName("powinno usunąć tylko wskazane pozycje i zarejestrować zdarzenie DraftBooksDeletedEvent")
        void shouldRemoveSelectedDraftBooksAndEmitEvent() {
            // given
            CatalogingSession session = CatalogingSession.createNew();
            DraftBook book1 = session.createNewDraftBook(validIsbn);
            DraftBook book2 = session.createNewDraftBook(otherIsbn);
            drainEvents(session);

            // when
            session.removeDraftBooks(List.of(book1.getId()));

            // then
            assertThat(session.getDraftBooks()).containsExactly(book2);

            List<BusinessEvent> events = drainEvents(session);
            assertThat(events).hasSize(1).first().isInstanceOf(DraftBooksDeletedEvent.class);

            DraftBooksDeletedEvent event = (DraftBooksDeletedEvent) events.getFirst();
            assertThat(event.getDraftBooks()).containsExactly(book1);
        }

        @Test
        @DisplayName("nie powinno modyfikować listy, jeśli przekazano nieistniejące ID")
        void shouldNotRemoveAnythingIfIdsDoNotMatch() {
            // given
            CatalogingSession session = CatalogingSession.createNew();
            DraftBook book1 = session.createNewDraftBook(validIsbn);
            drainEvents(session);

            // when
            session.removeDraftBooks(List.of(UUID.randomUUID()));

            // then
            assertThat(session.getDraftBooks()).containsExactly(book1);

            List<BusinessEvent> events = drainEvents(session);
            assertThat(events).hasSize(1);
            DraftBooksDeletedEvent event = (DraftBooksDeletedEvent) events.getFirst();
            assertThat(event.getDraftBooks()).isEmpty();
        }
    }

    @Test
    @DisplayName("Wywołanie operacji biznesowej powinno odświeżyć czas lastUse (touch)")
    void shouldUpdateLastUseOnModifications() throws InterruptedException {
        // given
        CatalogingSession session = CatalogingSession.createNew();
        Instant initialLastUse = session.getLastUse();

        // drobne opóźnienie, aby sprawdzić zmianę znacznika czasu
        Thread.sleep(5);

        // when
        session.createNewDraftBook(validIsbn);

        // then
        assertThat(session.getLastUse()).isAfter(initialLastUse);
    }
}