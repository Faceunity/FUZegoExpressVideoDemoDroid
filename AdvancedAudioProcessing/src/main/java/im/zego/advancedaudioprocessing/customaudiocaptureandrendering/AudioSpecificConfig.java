package im.zego.advancedaudioprocessing.customaudiocaptureandrendering;

public class AudioSpecificConfig {
    int object_type;
    int sampling_index;
    int chan_config;
    int frame_length_flag;
    int dependsOnCoreCoder;
    int extensionFlag;

    public AudioSpecificConfig()
    {
        object_type = 0;
        sampling_index = 0;
        chan_config = 0;
        frame_length_flag = 0;
        dependsOnCoreCoder = 0;
        extensionFlag=0;
    }
}
