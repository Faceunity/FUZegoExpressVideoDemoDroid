package im.zego.advancedaudioprocessing.customaudiocaptureandrendering;

public class AACFixedHeader {
    int Syncword; //同步头 总是0xFFF, all bits must be 1，代表着一个ADTS帧的开始
    int ID; //MPEG标识符，0标识MPEG-4，1标识MPEG-2
    int Layer; //always: '00'
    int protection_absent; //表示是否误码校验。Warning, set to 1 if there is no CRC and 0 if there is CRC
    int profile; //表示使用哪个级别的AAC，如00 Main profile, 01 Low Complexity(LC)--- AAC LC。有些芯片只支持AAC LC 。10 Scalable Sampling Rate profile (SSR) 11 Reserved
    int sampling_frequency_index; // 0: 96000; 1 88200; 2 64000; 3 48000; 4 44100; 5 32000; 6 24000; 7 22050; 8 16000; 9 12000; 10 11025; 11 8000; 12 7350; other reserved;
    int private_bit;
    int channel_configuration; //表示声道数，比如2表示立体声双声道 /* 0: Defined in AOT Specifc Config; 1: 1 channel: front-center; 2: 2 channels: front-left, front-right; 3: 3 channels: front-center, front-left, front-right; 4: 4 channels: front-center, front-left, front-right, back-center; 5: 5 channels: front-center, front-left, front-right, back-left, back-right; 6: 6 channels: front-center, front-left, front-right, back-left, back-right, LFE-channel; 7: 8 channels: front-center, front-left, front-right, side-left, side-right, back-left, back-right, LFE-channel; 8-15: Reserved */
    int original_copy;
    int home;
    int copyright_identification_bit;
    int copyright_identification_start;
    long aac_frame_length; //aac frame 长度 byte，包含了header
    int adts_buffer_fullness; //
    int number_of_raw_data_blocks_in_frame; // 表示ADTS帧中有number_of_raw_data_blocks_in_frame + 1个AAC原始帧

    public AACFixedHeader()
    {
        Syncword = 0;
        ID = 0;
        Layer = 0;
        protection_absent = 0;
        profile = 0;
        sampling_frequency_index = 0;
        private_bit = 0;
        channel_configuration = 0;
        original_copy = 0;
        home = 0;
        copyright_identification_bit = 0;
        copyright_identification_start = 0;
        aac_frame_length = 0;
        adts_buffer_fullness = 0;
        number_of_raw_data_blocks_in_frame = 0;
    }
};