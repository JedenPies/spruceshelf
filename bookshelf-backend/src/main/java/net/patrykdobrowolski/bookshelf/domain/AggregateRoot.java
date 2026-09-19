package net.patrykdobrowolski.bookshelf.domain;

import net.patrykdobrowolski.bookshelf.domain.model.event.BusinessEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public abstract class AggregateRoot {

    private final List<BusinessEvent> events = new ArrayList<>();

    protected void registerEvent(BusinessEvent event) {
        events.add(event);
    }

    synchronized public void publishEvents(Consumer<BusinessEvent> eventPublisher) {
        events.forEach(eventPublisher);
        events.clear();
    }
}
