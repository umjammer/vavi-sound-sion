package org.si.utils;

import java.util.EventObject;


public class Event extends EventObject {

    private static final Object DEFAULT_SOURCE = new Object();

    public static final String COMPLETE = "complete";
    public static final String ID3 = "id3";
    public static final String OPEN = "open";
    public String type;
    public boolean bubbles;
    public boolean cancelable;
    public Object target;
    private boolean defaultPrevented;
    public Object currentTarget;

    public Event(String type, boolean bubbles, boolean cancelable) {
        super(DEFAULT_SOURCE);
        this.type = type;
        this.bubbles = bubbles;
        this.cancelable = cancelable;
    }

    public Event(String type) {
        this(type, false, false);
    }

    public void preventDefault() {
        if (cancelable) defaultPrevented = true;
    }

    public boolean isDefaultPrevented() {
        return defaultPrevented;
    }

    void setTarget(Object target) {
        this.target = target;
        this.source = (target != null) ? target : DEFAULT_SOURCE;
    }

    void setCurrentTarget(Object currentTarget) {
        this.currentTarget = currentTarget;
    }

    @Override
    public Event clone() {
        Event cloned = new Event(type, bubbles, cancelable);
        cloned.defaultPrevented = defaultPrevented;
        cloned.target = target;
        cloned.currentTarget = currentTarget;
        if (cloned.target != null) {
            cloned.source = cloned.target;
        }
        return cloned;
    }
}
