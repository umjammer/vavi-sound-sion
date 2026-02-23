package org.si.utils;


public class ProgressEvent extends Event {

    public static final String PROGRESS = "progress";
    public int bytesLoaded;
    public int bytesTotal;

    public ProgressEvent(String type, boolean bubbles, boolean cancelable, int loaded, int total) {
        super(type, bubbles, cancelable);
        this.bytesLoaded = loaded;
        this.bytesTotal = total;
    }

    @Override
    public ProgressEvent clone() {
        ProgressEvent cloned = new ProgressEvent(type, bubbles, cancelable, bytesLoaded, bytesTotal);
        cloned.target = target;
        cloned.currentTarget = currentTarget;
        return cloned;
    }
}
