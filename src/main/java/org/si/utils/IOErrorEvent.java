package org.si.utils;


public class IOErrorEvent extends ErrorEvent {

    public static final String IO_ERROR = "ioError";

    public IOErrorEvent(String type, boolean bubbles, boolean cancelable, String text) {
        super(type, bubbles, cancelable, text);
    }

    public IOErrorEvent(String text) {
        this(IO_ERROR, false, false, text);
    }

    @Override
    public IOErrorEvent clone() {
        IOErrorEvent cloned = new IOErrorEvent(type, bubbles, cancelable, text);
        cloned.target = target;
        cloned.currentTarget = currentTarget;
        return cloned;
    }
}
