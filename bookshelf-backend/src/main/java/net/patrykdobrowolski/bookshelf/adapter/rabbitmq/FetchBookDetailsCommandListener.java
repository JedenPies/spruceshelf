package net.patrykdobrowolski.bookshelf.adapter.rabbitmq;

import jakarta.inject.Named;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.patrykdobrowolski.bookshelf.domain.exception.DraftBookNotFoundException;
import net.patrykdobrowolski.bookshelf.domain.exception.CatalogingSessionNotFoundException;
import net.patrykdobrowolski.bookshelf.domain.model.value.DraftBookStatus;
import net.patrykdobrowolski.bookshelf.adapter.rabbitmq.dto.FetchBookDetailsCommandDto;
import net.patrykdobrowolski.bookshelf.service.FetchBookService;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.amqp.rabbit.annotation.RabbitHandler;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;

@RabbitListener(queues = "${rabbitmq.fetch-book-command-queue}")
@Slf4j
@Named
@RequiredArgsConstructor
public class FetchBookDetailsCommandListener {

    @Value("${rabbitmq.command-exchange}")
    private String fetchBookCommandExchangeName;

    @Value("${rabbitmq.fetch-book-command-retry-queue}")
    private String fetchBookCommandRetryQueueName;

    private final FetchBookService fetchBookService;
    private final RabbitTemplate rabbitTemplate;

    @RabbitHandler
    public void handleFetchBookDetailsCommand(FetchBookDetailsCommandDto command) {
        try {
            command.tried();
            fetchBookService.startFetchingBook(command.getSessionId(), command.getDraftBookId());
            DraftBookStatus draftBookStatus = fetchBookService.fetchBookForDraft(command.getSessionId(), command.getDraftBookId(), command.getTryCount() >= 3);
            if (draftBookStatus == DraftBookStatus.FETCHING) retry(command);
        } catch (DraftBookNotFoundException e) {
            log.error("Draft book not found", e);
            throw new AmqpRejectAndDontRequeueException(e.getMessage());
        } catch (CatalogingSessionNotFoundException e) {
            log.error("Session not found", e);
            throw new AmqpRejectAndDontRequeueException(e.getMessage());
        }
    }

    private void retry(FetchBookDetailsCommandDto command) {
        log.warn("Nie udało się pobrać danych dla sesji {} draft book {}. Próba: {}/3",
                command.getSessionId(), command.getDraftBookId(), command.getTryCount() + 1);
        if (command.getTryCount() < 3) {
            rabbitTemplate.convertAndSend(fetchBookCommandExchangeName, fetchBookCommandRetryQueueName, command);
        } else {
            log.error("Wyczerpano 3 próby pobrania dla sesji {} draft book {}", command.getSessionId(), command.getDraftBookId());
        }
    }
}
