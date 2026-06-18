package com.typeahead.batch;

import com.typeahead.model.SearchEvent;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;

@Component
public class SearchQueue {

    private final ConcurrentLinkedQueue<SearchEvent> queue = new ConcurrentLinkedQueue<>();

    public void enqueue(SearchEvent event) {
        queue.add(event);
    }

    public List<SearchEvent> drainAll() {
        List<SearchEvent> events = new ArrayList<>();
        SearchEvent event;
        while ((event = queue.poll()) != null) {
            events.add(event);
        }
        return events;
    }

    public int size() {
        return queue.size();
    }
}
