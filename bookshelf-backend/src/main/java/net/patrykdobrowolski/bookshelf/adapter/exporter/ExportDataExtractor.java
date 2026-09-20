package net.patrykdobrowolski.bookshelf.adapter.exporter;

import net.patrykdobrowolski.bookshelf.domain.exception.ExportException;
import net.patrykdobrowolski.bookshelf.domain.model.value.ExportType;

import java.util.UUID;

public interface ExportDataExtractor {

    boolean supports(ExportType exportType);
    ExportData extract(UUID correlationKey) throws ExportException.ExtractingExportDataException;
}
