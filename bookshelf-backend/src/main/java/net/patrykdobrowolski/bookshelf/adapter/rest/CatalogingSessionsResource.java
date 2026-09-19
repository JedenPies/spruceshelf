package net.patrykdobrowolski.bookshelf.adapter.rest;

import lombok.RequiredArgsConstructor;
import net.patrykdobrowolski.bookshelf.adapter.rest.dto.CatalogingSessionDto;
import net.patrykdobrowolski.bookshelf.adapter.rest.dto.ExportDto;
import net.patrykdobrowolski.bookshelf.adapter.rest.dto.ExportRequestDto;
import net.patrykdobrowolski.bookshelf.adapter.rest.mapper.CatalogingSessionDtoMapper;
import net.patrykdobrowolski.bookshelf.adapter.rest.mapper.ExportDtoMapper;
import net.patrykdobrowolski.bookshelf.adapter.sse.SseCatalogingSessionService;
import net.patrykdobrowolski.bookshelf.domain.exception.CatalogingSessionNotFoundException;
import net.patrykdobrowolski.bookshelf.domain.exception.ExportException;
import net.patrykdobrowolski.bookshelf.domain.model.cataloging.CatalogingSession;
import net.patrykdobrowolski.bookshelf.domain.model.export.Export;
import net.patrykdobrowolski.bookshelf.domain.model.value.ExportType;
import net.patrykdobrowolski.bookshelf.domain.port.CatalogingSessionServicePort;
import net.patrykdobrowolski.bookshelf.domain.port.ExportServicePort;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.UUID;

@RestController
@RequestMapping("/api/cataloging-sessions")
@RequiredArgsConstructor
public class CatalogingSessionsResource {

    private final CatalogingSessionServicePort sessionService;
    private final SseCatalogingSessionService sseCatalogingSessionService;
    private final CatalogingSessionDtoMapper catalogingSessionDtoMapper;
    private final ExportServicePort exportService;
    private final ExportDtoMapper exportDtoMapper;


    @PostMapping
    public CatalogingSessionDto createSession() {
        CatalogingSession catalogingSession = sessionService.createSession();
        return catalogingSessionDtoMapper.toDto(catalogingSession);
    }

    @GetMapping("/{sessionId}/export")
    public ExportDto getSession(@PathVariable UUID sessionId) throws CatalogingSessionNotFoundException, ExportException.ExportNotFoundException {
        sessionService.ensureSessionExists(sessionId);
        Export found = exportService.findForCatalogingSession(sessionId);
        return exportDtoMapper.map(found);
    }

    @GetMapping("/{sessionId}/events-stream")
    public SseEmitter getEventsStream(@PathVariable UUID sessionId) throws CatalogingSessionNotFoundException {
        return sseCatalogingSessionService.createConnection(sessionId);
    }

    @PutMapping("/{sessionId}/export-request")
    @ResponseStatus(HttpStatus.CREATED)
    public ExportDto createNewExportRequest(
            @PathVariable UUID sessionId, @RequestBody ExportRequestDto exportDto) throws ExportException.ExportAlreadyRequestedException, CatalogingSessionNotFoundException, ExportException.ExportNotFoundException {
        sessionService.ensureSessionExists(sessionId);
        Export export = exportService.requestExport(
                exportDtoMapper.map(exportDto).withType(ExportType.CATALOGING_SESSION).withCorrelationKey(sessionId));
        return exportDtoMapper.map(export);
    }

}
