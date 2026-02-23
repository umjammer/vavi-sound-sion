/*
 * Copyright (c) 2026 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package org.si.utils;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;


/**
 * EventSupport.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 2026-02-23 nsano initial version <br>
 */
public class EventSupport {

    List<Consumer<Event>> listeners = new ArrayList<>();

    public void addEventListener(String message, Consumer<Event> listener) {
        listeners.add(listener);
    }

    public void dispatchEvent(Event progressEvent) {
        listeners.forEach(listener -> listener.accept(progressEvent));
    }
}
