package net.patrykdobrowolski.bookshelf.domain.model.export;

import net.patrykdobrowolski.bookshelf.domain.exception.ExportException;
import net.patrykdobrowolski.bookshelf.domain.model.command.ExportCommand;
import net.patrykdobrowolski.bookshelf.domain.model.event.BusinessEvent;
import net.patrykdobrowolski.bookshelf.domain.model.event.ExportCompleteEvent;
import net.patrykdobrowolski.bookshelf.domain.model.event.ExportRequestedEvent;
import net.patrykdobrowolski.bookshelf.domain.model.value.ExportFormat;
import net.patrykdobrowolski.bookshelf.domain.model.value.ExportStatus;
import net.patrykdobrowolski.bookshelf.domain.model.value.ExportType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ExportTest {

    private final UUID correlationKey = UUID.randomUUID();

    private ExportCommand createCommand(ExportFormat format, ExportType type) {
        return ExportCommand.builder()
                .format(format)
                .type(type)
                .correlationKey(correlationKey)
                .build();
    }

    private List<BusinessEvent> drainEvents(Export export) {
        List<BusinessEvent> events = new ArrayList<>();
        export.publishEvents(events::add);
        return events;
    }

    @Test
    @DisplayName("createNew() powinno poprawnie zainicjalizować obiekt ze statusem REQUESTED na podstawie komendy")
    void shouldCreateNewExportFromCommand() {
        // given
        ExportCommand command = createCommand(ExportFormat.CSV, ExportType.CATALOGING_SESSION);

        // when
        Export export = Export.createNew(command);

        // then
        assertThat(export.getId()).isNotNull();
        assertThat(export.getFormat()).isEqualTo(ExportFormat.CSV);
        assertThat(export.getType()).isEqualTo(ExportType.CATALOGING_SESSION);
        assertThat(export.getCorrelationKey()).isEqualTo(correlationKey);
        assertThat(export.getStatus()).isEqualTo(ExportStatus.REQUESTED);
        assertThat(export.getCreatedAt()).isNotNull();
        assertThat(export.getModifiedAt()).isNull();
        assertThat(export.getData()).isNull();
        assertThat(drainEvents(export)).isEmpty();
    }

    @Nested
    @DisplayName("Metoda request()")
    class RequestTests {

        @Test
        @DisplayName("powinno rzucić ExportAlreadyRequestedException, gdy status to REQUESTED")
        void shouldThrowExceptionWhenExportAlreadyRequested() {
            // given
            Export export = Export.createNew(createCommand(ExportFormat.CSV, ExportType.CATALOGING_SESSION));
            ExportCommand newCommand = createCommand(ExportFormat.XLSX, ExportType.CATALOGING_SESSION);

            // when & then
            assertThatThrownBy(() -> export.request(newCommand))
                    .isInstanceOf(ExportException.ExportAlreadyRequestedException.class);
        }

        @Test
        @DisplayName("powinno zaktualizować dane i wyemitować ExportRequestedEvent, gdy poprzedni eksport się zakończył")
        void shouldUpdateRequestWhenNotInRequestedStatus() throws Exception {
            // given
            Export export = Export.createNew(createCommand(ExportFormat.CSV, ExportType.CATALOGING_SESSION));
            export.begin();
            export.failed();
            drainEvents(export); // oczyszczenie kolejki po poprzednich akcjach

            UUID newCorrelationKey = UUID.randomUUID();
            ExportCommand newCommand = ExportCommand.builder()
                    .format(ExportFormat.XLSX)
                    .type(ExportType.CATALOGING_SESSION)
                    .correlationKey(newCorrelationKey)
                    .build();

            // when
            export.request(newCommand);

            // then
            assertThat(export.getStatus()).isEqualTo(ExportStatus.REQUESTED);
            assertThat(export.getFormat()).isEqualTo(ExportFormat.XLSX);
            assertThat(export.getType()).isEqualTo(ExportType.CATALOGING_SESSION);
            assertThat(export.getCorrelationKey()).isEqualTo(newCorrelationKey);

            List<BusinessEvent> events = drainEvents(export);
            assertThat(events)
                    .hasSize(1)
                    .first()
                    .isInstanceOf(ExportRequestedEvent.class);

            ExportRequestedEvent event = (ExportRequestedEvent) events.getFirst();
            assertThat(event.getExport()).isEqualTo(export);
        }
    }

    @Nested
    @DisplayName("Metoda isComplete()")
    class IsCompleteTests {

        @Test
        @DisplayName("powinno zwrócić false dla stanu REQUESTED i PROCESSING")
        void shouldReturnFalseForIncompleteStatuses() {
            Export export = Export.createNew(createCommand(ExportFormat.CSV, ExportType.CATALOGING_SESSION));
            assertThat(export.isComplete()).isFalse();

            export.begin();
            assertThat(export.isComplete()).isFalse();
        }

        @Test
        @DisplayName("powinno zwrócić true dla stanu SUCCEED i FAILED")
        void shouldReturnTrueForCompleteStatuses() {
            Export succeedExport = Export.createNew(createCommand(ExportFormat.CSV, ExportType.CATALOGING_SESSION));
            succeedExport.begin();
            succeedExport.exported(new byte[]{1, 2, 3});
            assertThat(succeedExport.isComplete()).isTrue();

            Export failedExport = Export.createNew(createCommand(ExportFormat.CSV, ExportType.CATALOGING_SESSION));
            failedExport.begin();
            failedExport.failed();
            assertThat(failedExport.isComplete()).isTrue();
        }
    }

    @Test
    @DisplayName("begin() powinno ustawić status PROCESSING oraz zaktualizować modifiedAt")
    void shouldBeginProcessing() {
        // given
        Export export = Export.createNew(createCommand(ExportFormat.CSV, ExportType.CATALOGING_SESSION));

        // when
        export.begin();

        // then
        assertThat(export.getStatus()).isEqualTo(ExportStatus.PROCESSING);
        assertThat(export.getModifiedAt()).isNotNull();
        assertThat(drainEvents(export)).isEmpty();
    }

    @Test
    @DisplayName("exported() powinno zapisać dane binarne, ustawić status SUCCEED oraz zarejestrować ExportCompleteEvent")
    void shouldCompleteExportSuccessfully() {
        // given
        Export export = Export.createNew(createCommand(ExportFormat.CSV, ExportType.CATALOGING_SESSION));
        export.begin();
        byte[] exportData = "id,isbn,title".getBytes();

        // when
        export.exported(exportData);

        // then
        assertThat(export.getStatus()).isEqualTo(ExportStatus.SUCCEED);
        assertThat(export.getData()).isEqualTo(exportData);
        assertThat(export.getModifiedAt()).isNotNull();

        List<BusinessEvent> events = drainEvents(export);
        assertThat(events)
                .hasSize(1)
                .first()
                .isInstanceOf(ExportCompleteEvent.class);

        ExportCompleteEvent event = (ExportCompleteEvent) events.getFirst();
        // Jeśli ExportCompleteEvent używa rekordu lub tradycyjnego gettera z Lomboka:
        assertThat(event.getExport()).isEqualTo(export);
    }

    @Test
    @DisplayName("failed() powinno ustawić status FAILED oraz zarejestrować ExportCompleteEvent")
    void shouldMarkExportAsFailed() {
        // given
        Export export = Export.createNew(createCommand(ExportFormat.CSV, ExportType.CATALOGING_SESSION));
        export.begin();

        // when
        export.failed();

        // then
        assertThat(export.getStatus()).isEqualTo(ExportStatus.FAILED);
        assertThat(export.getModifiedAt()).isNotNull();

        List<BusinessEvent> events = drainEvents(export);
        assertThat(events)
                .hasSize(1)
                .first()
                .isInstanceOf(ExportCompleteEvent.class);

        ExportCompleteEvent event = (ExportCompleteEvent) events.getFirst();
        assertThat(event.getExport()).isEqualTo(export);
    }
}