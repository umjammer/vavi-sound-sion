package vavi.media;

public class SoundLoaderContext {

    public double bufferTime;
    public boolean checkPolicyFile;

    public SoundLoaderContext(double bufferTime, boolean checkPolicyFile) {
        this.bufferTime = bufferTime;
        this.checkPolicyFile = checkPolicyFile;
    }
}
