package net.patrykdobrowolski.bookshelf.domain.model.export;

import lombok.Builder;
import lombok.Getter;
import net.patrykdobrowolski.bookshelf.domain.AggregateRoot;
import net.patrykdobrowolski.bookshelf.domain.exception.ExportException;
import net.patrykdobrowolski.bookshelf.domain.model.command.ExportCommand;
import net.patrykdobrowolski.bookshelf.domain.model.event.ExportCompleteEvent;
import net.patrykdobrowolski.bookshelf.domain.model.event.ExportRequestedEvent;
import net.patrykdobrowolski.bookshelf.domain.model.value.ExportFormat;
import net.patrykdobrowolski.bookshelf.domain.model.value.ExportStatus;
import net.patrykdobrowolski.bookshelf.domain.model.value.ExportType;

import java.time.Instant;
import java.util.UUID;

@Builder
@Getter
public class Export extends AggregateRoot {

    private UUID id;
    private ExportFormat format;
    private ExportStatus status;
    private ExportType type;
    private UUID correlationKey;
    private byte[] data;
    private Instant createdAt;
    private Instant modifiedAt;

    private Export(UUID id, ExportFormat format, ExportStatus status, ExportType type, UUID correlationKey, byte[] data, Instant createdAt, Instant modifiedAt) {
        this.id = id;
        this.format = format;
        this.status = status;
        this.type = type;
        this.correlationKey = correlationKey;
        this.data = data;
        this.createdAt = createdAt;
        this.modifiedAt = modifiedAt;
    }

    public static Export createNew(ExportCommand command) {
        return Export.builder()
                .id(UUID.randomUUID())
                .format(command.getFormat())
                .type(command.getType())
                .correlationKey(command.getCorrelationKey())
                .createdAt(Instant.now())
                .status(ExportStatus.REQUESTED)
                .build();
    }

    public void request(ExportCommand command) throws ExportException.ExportAlreadyRequestedException {
        if (ExportStatus.REQUESTED.equals(status)) throw new ExportException.ExportAlreadyRequestedException();
        this.format = command.getFormat();
        this.status = ExportStatus.REQUESTED;
        this.type = command.getType();
        this.correlationKey = command.getCorrelationKey();
        this.createdAt = Instant.now();
        registerEvent(ExportRequestedEvent.of(this));
    }

    public boolean isComplete() {
        return ExportStatus.SUCCEED.equals(status) || ExportStatus.FAILED.equals(status);
    }

    public void begin() {
        this.status = ExportStatus.PROCESSING;
        this.modifiedAt = Instant.now();
    }

    public void exported(byte[] data) {
        this.data = data;
        this.status = ExportStatus.SUCCEED;
        this.modifiedAt = Instant.now();
        registerEvent(ExportCompleteEvent.of(this));
    }

    public void failed() {
        this.status = ExportStatus.FAILED;
        this.modifiedAt = Instant.now();
        registerEvent(ExportCompleteEvent.of(this));
    }
}
