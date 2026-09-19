package net.patrykdobrowolski.bookshelf.domain.port;

import net.patrykdobrowolski.bookshelf.domain.exception.ExportException;
import net.patrykdobrowolski.bookshelf.domain.exception.CatalogingSessionNotFoundException;
import net.patrykdobrowolski.bookshelf.domain.model.command.ExportCommand;
import net.patrykdobrowolski.bookshelf.domain.model.export.Export;

import java.util.UUID;

public interface ExportServicePort {

    Export requestExport(ExportCommand command) throws CatalogingSessionNotFoundException, ExportException.ExportAlreadyRequestedException, ExportException.ExportNotFoundException;
    Export beginExport(UUID exportId) throws CatalogingSessionNotFoundException, ExportException.ExportNotRequestedException, ExportException.ExportNotFoundException;
    Export findExport(UUID exportId) throws ExportException.ExportNotFoundException;
    Export findForCatalogingSession(UUID sessionId) throws ExportException.ExportNotFoundException;
    Export save(Export export);
}
