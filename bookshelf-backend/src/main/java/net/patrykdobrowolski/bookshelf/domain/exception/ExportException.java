package net.patrykdobrowolski.bookshelf.domain.exception;

import lombok.NoArgsConstructor;
import net.patrykdobrowolski.bookshelf.domain.model.value.ExportFormat;

@NoArgsConstructor
public abstract class ExportException extends Exception {

    protected ExportException(String message, Exception cause) {
        super(message, cause);
    }

    protected ExportException(String message) {
        super(message);
    }

    public static class ExportFailedException extends ExportException {

        public ExportFailedException(String message, Exception cause) {
            super(message, cause);
        }
    }

    public static class ExportFormatNotSupportedException extends ExportException {

        public ExportFormatNotSupportedException(ExportFormat exportFormat) {
            super(exportFormat.name() + " not supported");
        }
    }

    public static class ExportAlreadyRequestedException extends ExportException {
    }

    public static class ExportNotFoundException extends ExportException {
    }

    public static class ExportNotRequestedException extends ExportException {
    }

    public static class ExtractingExportDataException extends ExportException {
    }
}
