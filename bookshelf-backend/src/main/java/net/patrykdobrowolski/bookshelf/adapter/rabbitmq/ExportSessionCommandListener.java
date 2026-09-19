package net.patrykdobrowolski.bookshelf.adapter.rabbitmq;

import jakarta.inject.Named;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.patrykdobrowolski.bookshelf.domain.exception.ExportException;
import net.patrykdobrowolski.bookshelf.domain.exception.CatalogingSessionNotFoundException;
import net.patrykdobrowolski.bookshelf.adapter.rabbitmq.dto.ExportCommandDto;
import net.patrykdobrowolski.bookshelf.service.ExportCatalogingSessionService;
import org.springframework.amqp.rabbit.annotation.RabbitHandler;
import org.springframework.amqp.rabbit.annotation.RabbitListener;

@RabbitListener(queues = "${rabbitmq.export-command-queue}")
@Slf4j
@Named
@RequiredArgsConstructor
public class ExportSessionCommandListener {

    private final ExportCatalogingSessionService exportCatalogingSessionService;

    @RabbitHandler
    public void handleFetchBookDetailsCommand(ExportCommandDto command) throws CatalogingSessionNotFoundException, ExportException.ExportNotRequestedException, ExportException.ExportNotFoundException {
        exportCatalogingSessionService.doExport(command.getExportId());
    }
}
