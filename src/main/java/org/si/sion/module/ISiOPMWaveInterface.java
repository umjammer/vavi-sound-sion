package org.si.sion.module;


/** Interface to set SiOPMWaveBase based classes. */
public interface ISiOPMWaveInterface {

    /**
     * Set PCM wave data rendered by %7.
     *
     * @param index           PCM data number.
     * @param data            wave data, Sound, Vector.&lt;Number&gt; or Vector.&lt;int&gt; is available. The Sound instance instanceof extracted internally, the maximum length to extract instanceof SiOPMWavePCMData.maxSampleLengthFromSound[samples].
     * @param samplingNote    Sampling wave's original note number, this allows decimal number
     * @param keyRangeFrom    Assigning key range starts from (not implemented in current version)
     * @param keyRangeTo      Assigning key range ends at (not implemented in current version)
     * @param srcChannelCount channel count of source data, 1 for monaural, 2 for stereo.
     * @param channelCount    channel count of this data, 1 for monaural, 2 for stereo, 0 sets same with srcChannelCount.
     * @see org.si.sion.module.SiOPMWavePCMData#maxSampleLengthFromSound
     * @see org.si.sion.SiONDriver#render
     */
    SiOPMWavePCMData setPCMWave(int index, Object data, double samplingNote, int keyRangeFrom, int keyRangeTo, int srcChannelCount, int channelCount);

    /**
     * Set sampler wave data referred by %10.
     *
     * @param index           note number. 0-127 for bank0, 128-255 for bank1.
     * @param data            wave data, Sound, Vector.&lt;Number&gt; or Vector.&lt;int&gt; is available. The Sound instanceof extracted when the length instanceof shorter than SiOPMWaveSamplerData.extractThreshold[msec].
     * @param ignoreNoteOff   True to set ignoring note off.
     * @param pan             pan of this sample [-64 - 64].
     * @param srcChannelCount channel count of source data, 1 for monaural, 2 for stereo.
     * @param channelCount    channel count of this data, 1 for monaural, 2 for stereo, 0 sets same with srcChannelCount.
     * @return created data instance
     * @see org.si.sion.module.SiOPMWaveSamplerData#extractThreshold
     * @see org.si.sion.SiONDriver#render
     */
    SiOPMWaveSamplerData setSamplerWave(int index, Object data, boolean ignoreNoteOff, int pan, int srcChannelCount, int channelCount);
}
