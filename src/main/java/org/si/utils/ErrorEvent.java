package org.si.utils;


public class ErrorEvent extends Event {

    public static final String ERROR = "error";
    public String text;

    public ErrorEvent(String type, boolean bubbles, boolean cancelable, String text) {
        super(type, bubbles, cancelable);
        this.text = text;
    }

    public ErrorEvent(String text) {
        this(ERROR, false, false, text);
    }

    @Override
    public ErrorEvent clone() {
        ErrorEvent cloned = new ErrorEvent(type, bubbles, cancelable, text);
        cloned.target = target;
        cloned.currentTarget = currentTarget;
        return cloned;
    }

    @Override
    public String toString() {
        return text != null ? text : super.toString();
    }
}
