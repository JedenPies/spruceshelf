package net.patrykdobrowolski.bookshelf.service;

import jakarta.inject.Named;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import net.patrykdobrowolski.bookshelf.domain.exception.ExportException;
import net.patrykdobrowolski.bookshelf.domain.model.command.ExportCommand;
import net.patrykdobrowolski.bookshelf.domain.model.export.Export;
import net.patrykdobrowolski.bookshelf.domain.model.value.ExportType;
import net.patrykdobrowolski.bookshelf.domain.port.ExportRepositoryPort;
import net.patrykdobrowolski.bookshelf.domain.port.ExportServicePort;
import org.springframework.context.ApplicationEventPublisher;

import java.util.Optional;
import java.util.UUID;

@Named
@RequiredArgsConstructor
public class ExportService implements ExportServicePort {

    private final ExportRepositoryPort exportRepository;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    @Override
    public Export requestExport(ExportCommand command) throws ExportException.ExportAlreadyRequestedException, ExportException.ExportNotFoundException {
        Optional<Export> existingExport = exportRepository.findByTypeAndCorrelationKey(command.getType(), command.getCorrelationKey());
        if (existingExport.isPresent()) {
            if (!existingExport.get().isComplete()) throw new ExportException.ExportAlreadyRequestedException();
        }
        Export export = existingExport.orElseGet(() -> Export.createNew(command));
        export.request(command);
        export.publishEvents(eventPublisher::publishEvent);
        return exportRepository.save(export);
    }

    @Transactional
    @Override
    public Export beginExport(UUID exportId) throws ExportException.ExportNotFoundException {
        Export export = exportRepository.findById(exportId);
        export.begin();
        export.publishEvents(eventPublisher::publishEvent);
        return exportRepository.save(export);
    }

    @Transactional
    @Override
    public Export findExport(UUID exportId) throws ExportException.ExportNotFoundException {
        return exportRepository.findById(exportId);
    }

    @Transactional
    @Override
    public Export findForCatalogingSession(UUID sessionId) throws ExportException.ExportNotFoundException {
        return exportRepository.findByTypeAndCorrelationKey(ExportType.CATALOGING_SESSION, sessionId).orElseThrow(ExportException.ExportNotFoundException::new);
    }

    @Transactional
    @Override
    public Export save(Export export) {
        export.publishEvents(eventPublisher::publishEvent);
        return exportRepository.save(export);
    }
}
