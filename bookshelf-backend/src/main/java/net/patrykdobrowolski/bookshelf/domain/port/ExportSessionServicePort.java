package net.patrykdobrowolski.bookshelf.domain.port;

import net.patrykdobrowolski.bookshelf.domain.exception.ExportException;
import net.patrykdobrowolski.bookshelf.domain.exception.CatalogingSessionNotFoundException;

import java.util.UUID;

public interface ExportSessionServicePort {

    void doExport(UUID sessionId) throws CatalogingSessionNotFoundException, ExportException.ExportNotRequestedException, ExportException.ExportNotFoundException;
}
