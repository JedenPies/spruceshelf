package net.patrykdobrowolski.bookshelf.service;

import jakarta.inject.Named;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import net.patrykdobrowolski.bookshelf.domain.exception.CatalogingSessionNotFoundException;
import net.patrykdobrowolski.bookshelf.domain.model.cataloging.CatalogingSession;
import net.patrykdobrowolski.bookshelf.domain.port.CatalogingSessionRepositoryPort;
import net.patrykdobrowolski.bookshelf.domain.port.CatalogingSessionServicePort;
import org.springframework.context.ApplicationEventPublisher;

import java.util.UUID;

@Named
@RequiredArgsConstructor
public class CatalogingSessionService implements CatalogingSessionServicePort {

    private final CatalogingSessionRepositoryPort catalogingSessionRepository;
    private final ApplicationEventPublisher eventPublisher;

    @Override
    public CatalogingSession findById(UUID catalogingSessionId) throws CatalogingSessionNotFoundException {
        return catalogingSessionRepository.findById(catalogingSessionId);
    }

    @Override
    @Transactional
    public CatalogingSession save(CatalogingSession catalogingSession) {
        CatalogingSession saved = catalogingSessionRepository.save(catalogingSession);
        catalogingSession.publishEvents(eventPublisher::publishEvent);
        return saved;
    }

    @Transactional
    @Override
    public CatalogingSession createSession() {
        CatalogingSession newCatalogingSession = CatalogingSession.createNew();
        CatalogingSession saved = catalogingSessionRepository.save(newCatalogingSession);
        newCatalogingSession.publishEvents(eventPublisher::publishEvent);
        return saved;
    }

    @Override
    public void ensureSessionExists(UUID catalogingSessionId) throws CatalogingSessionNotFoundException {
        catalogingSessionRepository.findById(catalogingSessionId);
    }
}
