package vavi.events;

import org.si.utils.ByteArray;
import org.si.utils.Event;

public class SampleDataEvent extends Event {
    public static final String SAMPLE_DATA = "sampleData";
    public ByteArray data;
    public double position;

    public SampleDataEvent(String type) {
        super(type);
        this.data = new ByteArray();
    }
}
