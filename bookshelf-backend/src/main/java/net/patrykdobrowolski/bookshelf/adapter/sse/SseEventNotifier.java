package net.patrykdobrowolski.bookshelf.adapter.sse;

import jakarta.inject.Named;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.patrykdobrowolski.bookshelf.adapter.sse.mapper.EventsMapper;
import net.patrykdobrowolski.bookshelf.domain.model.event.DraftBookCreatedEvent;
import net.patrykdobrowolski.bookshelf.domain.model.event.DraftBookUpdatedEvent;
import net.patrykdobrowolski.bookshelf.domain.model.event.DraftBooksDeletedEvent;
import net.patrykdobrowolski.bookshelf.domain.model.event.ExportCompleteEvent;
import net.patrykdobrowolski.bookshelf.domain.model.value.ExportType;
import org.springframework.transaction.event.TransactionalEventListener;

@Named
@RequiredArgsConstructor
@Slf4j
public class SseEventNotifier {

    private final SseCatalogingSessionService sseCatalogingSessionService;
    private final EventsMapper eventsMapper;

    @TransactionalEventListener
    public void handleDraftBookRequestedEvent(DraftBookCreatedEvent event) {
        log.debug("Sending DRAFT_BOOK_CREATED event to all connected clients");
        sseCatalogingSessionService.broadcastToSession(event.getCatalogingSession().getId(), "DRAFT_BOOK_CREATED", eventsMapper.toSseEvent(event));
    }

    @TransactionalEventListener
    public void handleDraftBookUpdatedEvent(DraftBookUpdatedEvent event) {
        log.debug("Sending DRAFT_BOOK_UPDATED event");
        sseCatalogingSessionService.broadcastToSession(event.getCatalogingSession().getId(), "DRAFT_BOOK_UPDATED", eventsMapper.toSseEvent(event));
    }

    @TransactionalEventListener
    public void handleDraftBooksDeletedEvent(DraftBooksDeletedEvent event) {
        log.debug("Sending DRAFT_BOOKS_DELETED event");
        sseCatalogingSessionService.broadcastToSession(event.getCatalogingSession().getId(), "DRAFT_BOOKS_DELETED", eventsMapper.toSseEvent(event));
    }

    @TransactionalEventListener
    public void handleExportCompleteEvent(ExportCompleteEvent event) {
        if (event.getExport().getType() == ExportType.CATALOGING_SESSION) {
            log.debug("Sending EXPORT_COMPLETE event");
            sseCatalogingSessionService.broadcastToSession(event.getExport().getCorrelationKey(), "EXPORT_COMPLETE", eventsMapper.toSseEvent(event));
        }
    }
}
