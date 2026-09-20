package net.patrykdobrowolski.bookshelf.adapter.exporter.impl;

import jakarta.inject.Named;
import lombok.RequiredArgsConstructor;
import net.patrykdobrowolski.bookshelf.adapter.exporter.ExportData;
import net.patrykdobrowolski.bookshelf.adapter.exporter.ExportDataExtractor;
import net.patrykdobrowolski.bookshelf.domain.exception.CatalogingSessionNotFoundException;
import net.patrykdobrowolski.bookshelf.domain.exception.ExportException;
import net.patrykdobrowolski.bookshelf.domain.model.cataloging.CatalogingSession;
import net.patrykdobrowolski.bookshelf.domain.model.cataloging.DraftBook;
import net.patrykdobrowolski.bookshelf.domain.model.value.BookDetails;
import net.patrykdobrowolski.bookshelf.domain.model.value.ExportType;
import net.patrykdobrowolski.bookshelf.domain.model.value.Year;
import net.patrykdobrowolski.bookshelf.domain.port.CatalogingSessionServicePort;

import java.util.Collections;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;

@Named
@RequiredArgsConstructor
public class CatalogingSessionDataExtractor implements ExportDataExtractor {

    private final CatalogingSessionServicePort sessionService;

    @Override
    public boolean supports(ExportType exportType) {
        return exportType == ExportType.CATALOGING_SESSION;
    }

    @Override
    public ExportData extract(UUID correlationKey) throws ExportException.ExtractingExportDataException {
        try {
            CatalogingSession session = sessionService.findById(correlationKey);
            ExportData.ExportDataBuilder builder = ExportData.builder()
                    .title("Cataloging session")
                    .headers(Stream.of("ISBN", "Title", "Authors", "Publication year", "Publisher", "Publication place", "Language", "Genres", "Added date").map(ExportData.Header::new).toList());
            for (DraftBook draftBook : session.getDraftBooks()) {
                builder.row(new ExportData.Row(
                        draftBook.getIsbn().value(),
                        Optional.ofNullable(draftBook.getBookDetails()).map(BookDetails::title).orElse(""),
                        String.join(", ", Optional.ofNullable(draftBook.getBookDetails()).map(BookDetails::authors).orElseGet(Collections::emptyList)),
                        Optional.ofNullable(draftBook.getBookDetails()).map(BookDetails::publicationYear).map(Year::value).orElse(""),
                        Optional.ofNullable(draftBook.getBookDetails()).map(BookDetails::publisher).orElse(""),
                        Optional.ofNullable(draftBook.getBookDetails()).map(BookDetails::publicationPlace).orElse(""),
                        Optional.ofNullable(draftBook.getBookDetails()).map(BookDetails::language).orElse(""),
                        String.join(", ", Optional.ofNullable(draftBook.getBookDetails()).map(BookDetails::genres).orElseGet(Collections::emptyList)),
                        Optional.ofNullable(draftBook.getCreatedAt()).map(Object::toString).orElse("")));
            }
            return builder.build();
        } catch (CatalogingSessionNotFoundException e) {
            throw new ExportException.ExtractingExportDataException();
        }
    }
}
