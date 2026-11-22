package net.patrykdobrowolski.bookshelf.adapter.rest;

import lombok.extern.slf4j.Slf4j;
import net.patrykdobrowolski.bookshelf.domain.exception.*;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;

@ControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(CatalogingSessionNotFoundException.class)
    @ResponseStatus(code = HttpStatus.NOT_FOUND)
    public void handleException() {}

    @ExceptionHandler(ShareCodeGenerationException.class)
    @ResponseStatus(code = HttpStatus.SERVICE_UNAVAILABLE)
    public void handleShareCodeGenerationException() {}

    @ExceptionHandler(ShareCodeNotFoundException.class)
    @ResponseStatus(code = HttpStatus.NOT_FOUND)
    public void handleShareCodeNotFoundException() {}

    @ExceptionHandler(ExportException.ExportAlreadyRequestedException.class)
    @ResponseStatus(code = HttpStatus.CONFLICT)
    public void handleExportAlreadyRequestedException(ExportException.ExportAlreadyRequestedException e) {
        log.error("Export already requested", e);
    }

    @ExceptionHandler(ExportException.ExportNotRequestedException.class)
    @ResponseStatus(code = HttpStatus.NOT_FOUND)
    public void exportNotRequestedException() {}

    @ExceptionHandler(ExportException.ExportNotFoundException.class)
    @ResponseStatus(code = HttpStatus.NOT_FOUND)
    public void exportNotFoundException() {}
}
