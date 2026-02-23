package org.si.utils;


@FunctionalInterface
public interface EventListener extends java.util.EventListener {

    void onEvent(Event event);
}
