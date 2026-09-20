package net.patrykdobrowolski.bookshelf.adapter.rest;

import lombok.RequiredArgsConstructor;
import net.patrykdobrowolski.bookshelf.adapter.rest.dto.ExportDto;
import net.patrykdobrowolski.bookshelf.adapter.rest.mapper.ExportDtoMapper;
import net.patrykdobrowolski.bookshelf.domain.exception.ExportException;
import net.patrykdobrowolski.bookshelf.domain.model.export.Export;
import net.patrykdobrowolski.bookshelf.service.ExportService;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/exports")
@RequiredArgsConstructor
public class ExportsResource {

    private final ExportService exportService;
    private final ExportDtoMapper exportDtoMapper;

    @GetMapping("{exportId}")
    public ExportDto getExport(@PathVariable UUID exportId) throws ExportException.ExportNotFoundException {
        Export export = exportService.findExport(exportId);
        return exportDtoMapper.map(export);
    }

    @GetMapping("{exportId}/data")
    public ResponseEntity<Resource> downloadExport(@PathVariable UUID exportId) throws ExportException.ExportNotFoundException {
        Export export = exportService.findExport(exportId);
        ByteArrayResource resource = new ByteArrayResource(export.getData());
        return ResponseEntity.ok().header(
                        HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=export-" + export.getId() + "." + export.getFormat().name().toLowerCase())
                .contentType(MediaType.parseMediaType("text/csv"))
                .body(resource);
    }
}
