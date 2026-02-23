package org.si.utils;


public class SecurityErrorEvent extends ErrorEvent {

    public static final String SECURITY_ERROR = "securityError";

    public SecurityErrorEvent(String type, boolean bubbles, boolean cancelable, String text) {
        super(type, bubbles, cancelable, text);
    }

    public SecurityErrorEvent(String text) {
        this(SECURITY_ERROR, false, false, text);
    }

    @Override
    public SecurityErrorEvent clone() {
        SecurityErrorEvent cloned = new SecurityErrorEvent(type, bubbles, cancelable, text);
        cloned.target = target;
        cloned.currentTarget = currentTarget;
        return cloned;
    }
}
