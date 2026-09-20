package net.patrykdobrowolski.bookshelf.service;

import jakarta.inject.Named;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.patrykdobrowolski.bookshelf.adapter.exporter.ExportData;
import net.patrykdobrowolski.bookshelf.adapter.exporter.ExportDataExtractor;
import net.patrykdobrowolski.bookshelf.adapter.exporter.ExportResult;
import net.patrykdobrowolski.bookshelf.adapter.exporter.Exporter;
import net.patrykdobrowolski.bookshelf.domain.exception.CatalogingSessionNotFoundException;
import net.patrykdobrowolski.bookshelf.domain.exception.ExportException;
import net.patrykdobrowolski.bookshelf.domain.model.export.Export;
import net.patrykdobrowolski.bookshelf.domain.model.value.ExportFormat;
import net.patrykdobrowolski.bookshelf.domain.model.value.ExportType;
import net.patrykdobrowolski.bookshelf.domain.port.ExportServicePort;
import net.patrykdobrowolski.bookshelf.domain.port.ExportSessionServicePort;

import java.util.List;
import java.util.UUID;

@Named
@RequiredArgsConstructor
@Slf4j
public class ExportCatalogingSessionService implements ExportSessionServicePort {

    private final ExportServicePort exportService;
    private final List<ExportDataExtractor> extractors;
    private final List<Exporter> exporters;

    @Override
    public void doExport(UUID exportId) throws CatalogingSessionNotFoundException, ExportException.ExportNotRequestedException, ExportException.ExportNotFoundException {
        Export export = exportService.beginExport(exportId);
        try {
            tryToExport(export);
        } catch (ExportException.ExportFormatNotSupportedException | ExportException.ExportFailedException |
                 ExportException.ExtractingExportDataException e) {
            log.error("Session export failed", e);
            export.failed();
        }
        exportService.save(export);
    }

    private void tryToExport(Export export) throws ExportException.ExportFormatNotSupportedException, ExportException.ExportFailedException, ExportException.ExtractingExportDataException {
        ExportDataExtractor extractor = findExtractor(export.getType());
        Exporter exporter = findExporter(export.getFormat());
        ExportData data = extractor.extract(export.getCorrelationKey());
        ExportResult result = exporter.export(data);
        export.exported(result.getData());
    }

    private ExportDataExtractor findExtractor(ExportType exportType) {
        return extractors.stream()
                .filter(extractor -> extractor.supports(exportType))
                .findFirst().orElseThrow(() -> new RuntimeException("No extractor found for export type: " + exportType));
    }

    private Exporter findExporter(ExportFormat format) throws ExportException.ExportFormatNotSupportedException {
        return exporters.stream()
                .filter(exporter -> exporter.supports(format))
                .findFirst().orElseThrow(() -> new ExportException.ExportFormatNotSupportedException(format));
    }
}
