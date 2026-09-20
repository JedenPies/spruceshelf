package net.patrykdobrowolski.bookshelf.domain.port;

import net.patrykdobrowolski.bookshelf.domain.exception.CatalogingSessionNotFoundException;
import net.patrykdobrowolski.bookshelf.domain.model.cataloging.CatalogingSession;

import java.util.UUID;

public interface CatalogingSessionServicePort {

    CatalogingSession findById(UUID sessionId) throws CatalogingSessionNotFoundException;
    CatalogingSession save(CatalogingSession catalogingSession);
    CatalogingSession createSession();
    void ensureSessionExists(UUID sessionId) throws CatalogingSessionNotFoundException;
}
