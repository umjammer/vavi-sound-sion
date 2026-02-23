package org.si.utils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class EventDispatcher {

    private static final class ListenerEntry {
        final EventListener listener;
        final int priority;

        ListenerEntry(EventListener listener, int priority) {
            this.listener = listener;
            this.priority = priority;
        }
    }

    private final Map<String, List<ListenerEntry>> listeners = new HashMap<>();

    public void addEventListener(String type, EventListener listener) {
        addEventListener(type, listener, false, 0);
    }

    public void addEventListener(String type, EventListener listener, boolean useCapture, int eventPriority) {
        if (type == null || listener == null) {
            return;
        }
        List<ListenerEntry> list = listeners.computeIfAbsent(type, key -> new ArrayList<>());
        for (ListenerEntry entry : list) {
            if (entry.listener == listener) {
                return;
            }
        }
        int index = 0;
        while (index < list.size() && list.get(index).priority >= eventPriority) {
            index++;
        }
        list.add(index, new ListenerEntry(listener, eventPriority));
    }

    public void removeEventListener(String type, EventListener listener) {
        if (type == null || listener == null) {
            return;
        }
        List<ListenerEntry> list = listeners.get(type);
        if (list == null) {
            return;
        }
        list.removeIf(entry -> entry.listener == listener);
        if (list.isEmpty()) {
            listeners.remove(type);
        }
    }

    public void dispatchEvent(Object event) {
        if (event instanceof Event typedEvent) {
            dispatchEvent(typedEvent);
        }
    }

    public void dispatchEvent(Event event) {
        if (event == null || event.type == null) {
            return;
        }
        if (event.target == null) {
            event.setTarget(this);
        }
        event.setCurrentTarget(this);

        List<ListenerEntry> list = listeners.get(event.type);
        if (list == null || list.isEmpty()) {
            return;
        }
        List<ListenerEntry> snapshot = new ArrayList<>(list);
        for (ListenerEntry entry : snapshot) {
            entry.listener.onEvent(event);
        }
    }

    public boolean hasEventListener(String type) {
        List<ListenerEntry> list = listeners.get(type);
        return list != null && !list.isEmpty();
    }
}
