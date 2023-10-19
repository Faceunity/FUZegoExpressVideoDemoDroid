package im.zego.expresssample.ui;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.tencent.bugly.crashreport.CrashReport;

import im.zego.Scenes.VideoForMultipleUsers.VideoForMultipleUsersLogin;
import im.zego.advancedaudioprocessing.audio3a.Audio3aActivity;
import im.zego.advancedaudioprocessing.audioeffectplayer.AudioEffectPlayerActivity;
import im.zego.advancedaudioprocessing.customaudiocaptureandrendering.CustomAudioCaptureAndRenderingLoginActivity;
import im.zego.advancedaudioprocessing.earreturnandchannelsettings.EarReturnandChannelSettingsActivity;
import im.zego.advancedaudioprocessing.originalaudiodataacquisition.OriginalAudioDataAcquisitionActivity;
import im.zego.advancedaudioprocessing.rangeaudio.ui.RangeAudioActivity;
import im.zego.advancedaudioprocessing.soundlevelandspectrum.ui.SoundLevelAndSpectrumMainActivity;
import im.zego.advancedaudioprocessing.voicechange.VoiceChangeActivity;
import im.zego.advancedstreaming.h265.H265LoginActivity;
import im.zego.advancedstreaming.lowlatencylive.LowLatencyLive;
import im.zego.advancedstreaming.publishingmultiplestreams.ui.PublishingMultipleStreams;
import im.zego.advancedstreaming.streamByCdn.StreamByCdn;
import im.zego.advancedstreaming.streammonitoring.StreamMonitoring;
import im.zego.advancedvideoprocessing.CustomerVideoCapture.CustomerVideoCaptureActivity;
import im.zego.advancedvideoprocessing.customrender.ui.ZGVideoRenderTypeUI;
import im.zego.advancedvideoprocessing.encodinganddecoding.EncodingAndDecoding;
import im.zego.commonfeatures.commonvideoconfig.CommonVideoConfigActivity;
import im.zego.commonfeatures.roommessage.RoomMessageActivity;
import im.zego.commonfeatures.videorotation.VideoRotationSelectionActivity;
import im.zego.debugandconfig.SettingActivity;
import im.zego.expresssample.R;
import im.zego.expresssample.adapter.MainAdapter;
import im.zego.expresssample.entity.ModuleInfo;
import im.zego.others.beautyandwatermarkandsnapshot.BeautyWatermarkSnapshotActivity;
import im.zego.others.camera.CameraActivity;
import im.zego.others.effectsbeauty.EffectsBeautyActivity;
import im.zego.others.flowcontrol.FlowControlActivity;
import im.zego.others.mediaplayer.UI.MediaPlayerSelectionActivity;
import im.zego.others.multiplerooms.MultipleRoomsActivity;
import im.zego.others.multivideosource.MultiVideoSourceActivity;
import im.zego.others.networkandperformance.NetworkAndPerformanceActivity;
import im.zego.others.recording.RecordingActivity;
import im.zego.others.screensharing.ScreenSharingActivity;
import im.zego.others.security.SecurityActivity;
import im.zego.others.sei.SEIActivity;
import im.zego.others.streammixing.MixerMainActivity;
import im.zego.others.superresolution.SuperResolutionActivity;
import im.zego.others.videoobjectsegmentation.VideoObjectSegmentationLoginActivity;
import im.zego.quickstart.commonusage.CommonUsage;
import im.zego.quickstart.playing.Playing;
import im.zego.quickstart.publishing.Publishing;
import im.zego.quickstart.videochat.VideoChatLogin;


public class MainActivity extends AppCompatActivity {


    private MainAdapter mainAdapter = new MainAdapter();
    private static final int REQUEST_PERMISSION_CODE = 101;

    private RecyclerView moduleList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        CrashReport.initCrashReport(getApplicationContext(), "13a8646a49", false);

        if (!isTaskRoot()) {
            /* If this is not the root activity */
            Intent intent = getIntent();
            String action = intent.getAction();
            if (intent.hasCategory(Intent.CATEGORY_LAUNCHER) && Intent.ACTION_MAIN.equals(action)) {
                finish();
                return;
            }
        }
        moduleList = findViewById(R.id.module_list);
        mainAdapter.setOnItemClickListener((view, position) -> {
            boolean orRequestPermission = this.checkOrRequestPermission(REQUEST_PERMISSION_CODE);
            ModuleInfo moduleInfo = (ModuleInfo) view.getTag();
            if (orRequestPermission) {
                String module = moduleInfo.getModule();
                if (module.equals(getString(R.string.custom_video_rendering))) {
                    ZGVideoRenderTypeUI.actionStart(MainActivity.this);
                } else if (module.equals(getString(R.string.common_video_config))) {
                    CommonVideoConfigActivity.actionStart(MainActivity.this);
                } else if (module.equals(getString(R.string.video_for_multiple_users))) {
                    VideoForMultipleUsersLogin.actionStart(MainActivity.this);
                } else if (module.equals(getString(R.string.video_rotation))) {
                    VideoRotationSelectionActivity.actionStart(MainActivity.this);
                } else if (module.equals(getString(R.string.beautify_watermark_snapshot))) {
                    BeautyWatermarkSnapshotActivity.actionStart(MainActivity.this);
                } else if (module.equals(getString(R.string.stream_monitoring))) {
                    StreamMonitoring.actionStart(MainActivity.this);
                } else if (module.equals(getString(R.string.publishing_multiple_streams))) {
                    PublishingMultipleStreams.actionStart(MainActivity.this);
                } else if (module.equals(getString(R.string.stream_by_cdn))) {
                    StreamByCdn.actionStart(MainActivity.this);
                } else if (module.equals(getString(R.string.low_latency_live))) {
                    LowLatencyLive.actionStart(MainActivity.this);
                } else if (module.equals(getString(R.string.h265))) {
                    H265LoginActivity.actionStart(MainActivity.this);
                } else if (module.equals(getString(R.string.common_usage))) {
                    CommonUsage.actionStart(MainActivity.this);
                } else if (module.equals(getString(R.string.video_chat))) {
                    VideoChatLogin.actionStart(MainActivity.this);
                } else if (module.equals(getString(R.string.publishing))) {
                    Publishing.actionStart(MainActivity.this);
                } else if (module.equals(getString(R.string.playing))) {
                    Playing.actionStart(MainActivity.this);
                } else if (module.equals(getString(R.string.room_message))) {
                    RoomMessageActivity.actionStart(MainActivity.this);
                } else if (module.equals(getString(R.string.encoding_decoding))) {
                    EncodingAndDecoding.actionStart(MainActivity.this);
                } else if (module.equals(getString(R.string.custom_video_capture))) {
                    CustomerVideoCaptureActivity.actionStart(MainActivity.this);
                } else if (module.equals(getString(R.string.log_version_debug))) {
                    SettingActivity.actionStart(MainActivity.this);
                } else if (module.equals(getString(R.string.stream_mixing))) {
                    MixerMainActivity.actionStart(MainActivity.this);
                } else if (module.equals(getString(R.string.recording))) {
                    RecordingActivity.actionStart(MainActivity.this);
                } else if (module.equals(getString(R.string.media_player))) {
                    MediaPlayerSelectionActivity.actionStart(MainActivity.this);
                } else if (module.equals(getString(R.string.camera))) {
                    CameraActivity.actionStart(MainActivity.this);
                } else if (module.equals(getString(R.string.multiple_rooms))) {
                    MultipleRoomsActivity.actionStart(MainActivity.this);
                } else if (module.equals(getString(R.string.flow_control))) {
                    FlowControlActivity.actionStart(MainActivity.this);
                } else if (module.equals(getString(R.string.network_and_performance))) {
                    NetworkAndPerformanceActivity.actionStart(MainActivity.this);
                } else if (module.equals(getString(R.string.security))) {
                    SecurityActivity.actionStart(MainActivity.this);
                } else if (module.equals(getString(R.string.screen_sharing))) {
                    ScreenSharingActivity.actionStart(MainActivity.this);
                } else if (module.equals(getString(R.string.sei))) {
                    SEIActivity.actionStart(MainActivity.this);
                } else if (module.equals(getString(R.string.voice_change_reverb_stereo))) {
                    VoiceChangeActivity.actionStart(MainActivity.this);
                } else if (module.equals(getString(R.string.ear_return_and_channel_settings))) {
                    EarReturnandChannelSettingsActivity.actionStart(MainActivity.this);
                } else if (module.equals(getString(R.string.soundlevel_and_audioSpectrum))) {
                    SoundLevelAndSpectrumMainActivity.actionStart(MainActivity.this);
                } else if (module.equals(getString(R.string.aec_ans_agc))) {
                    Audio3aActivity.actionStart(MainActivity.this);
                } else if (module.equals(getString(R.string.audio_effect_player))) {
                    AudioEffectPlayerActivity.actionStart(MainActivity.this);
                } else if (module.equals(getString(R.string.original_audio_data_acquisition))) {
                    OriginalAudioDataAcquisitionActivity.actionStart(MainActivity.this);
                } else if (module.equals(getString(R.string.custom_audio_capture_and_rendering))) {
                    CustomAudioCaptureAndRenderingLoginActivity.actionStart(MainActivity.this);
                } else if (module.equals(getString(R.string.range_audio))) {
                    RangeAudioActivity.actionStart(MainActivity.this);
                } else if (module.equals(getString(R.string.effects_beauty))) {
                    EffectsBeautyActivity.actionStart(MainActivity.this);
                } else if (module.equals(getString(R.string.super_resolution))) {
                    SuperResolutionActivity.actionStart(MainActivity.this);
                } else if (module.equals(getString(R.string.subject_segmentation))) {
                    VideoObjectSegmentationLoginActivity.actionStart(MainActivity.this);
                } else if (module.equals(getString(R.string.multi_video_source))) {
                    MultiVideoSourceActivity.actionStart(MainActivity.this);
                }
            }
        });

        // UI Setting
        moduleList.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false));
        moduleList.setAdapter(mainAdapter);
        moduleList.setItemAnimator(new DefaultItemAnimator());

        // Add Module
        mainAdapter.addModuleInfo(new ModuleInfo().moduleName(getString(R.string.common_usage)).titleName(getString(R.string.quick_start)));
        mainAdapter.addModuleInfo(new ModuleInfo().moduleName(getString(R.string.video_chat)));
        mainAdapter.addModuleInfo(new ModuleInfo().moduleName(getString(R.string.publishing)));
        mainAdapter.addModuleInfo(new ModuleInfo().moduleName(getString(R.string.playing)));
        mainAdapter.addModuleInfo(new ModuleInfo().moduleName(getString(R.string.video_for_multiple_users)).titleName(getString(R.string.scenes)));
        mainAdapter.addModuleInfo(new ModuleInfo().moduleName(getString(R.string.common_video_config)).titleName(getString(R.string.common_features)));
        mainAdapter.addModuleInfo(new ModuleInfo().moduleName(getString(R.string.video_rotation)));
        mainAdapter.addModuleInfo(new ModuleInfo().moduleName(getString(R.string.room_message)));
        mainAdapter.addModuleInfo(new ModuleInfo().moduleName(getString(R.string.stream_monitoring)).titleName(getString(R.string.advanced_streaming)));
        mainAdapter.addModuleInfo(new ModuleInfo().moduleName(getString(R.string.publishing_multiple_streams)));
        mainAdapter.addModuleInfo(new ModuleInfo().moduleName(getString(R.string.stream_by_cdn)));
//        mainAdapter.addModuleInfo(new ModuleInfo().moduleName(getString(R.string.low_latency_live)));
        mainAdapter.addModuleInfo(new ModuleInfo().moduleName(getString(R.string.h265)));
        mainAdapter.addModuleInfo(new ModuleInfo().moduleName(getString(R.string.encoding_decoding)).titleName(getString(R.string.advanced_video_processing)));
        mainAdapter.addModuleInfo(new ModuleInfo().moduleName(getString(R.string.custom_video_rendering)));
        mainAdapter.addModuleInfo(new ModuleInfo().moduleName(getString(R.string.custom_video_capture)));
        mainAdapter.addModuleInfo(new ModuleInfo().moduleName(getString(R.string.voice_change_reverb_stereo)).titleName(getString(R.string.advanced_audio_processing)));
        mainAdapter.addModuleInfo(new ModuleInfo().moduleName(getString(R.string.ear_return_and_channel_settings)));
        mainAdapter.addModuleInfo(new ModuleInfo().moduleName(getString(R.string.soundlevel_and_audioSpectrum)));
        mainAdapter.addModuleInfo(new ModuleInfo().moduleName(getString(R.string.aec_ans_agc)));
        mainAdapter.addModuleInfo(new ModuleInfo().moduleName(getString(R.string.audio_effect_player)));
        mainAdapter.addModuleInfo(new ModuleInfo().moduleName(getString(R.string.original_audio_data_acquisition)));
        mainAdapter.addModuleInfo(new ModuleInfo().moduleName(getString(R.string.custom_audio_capture_and_rendering)));
        mainAdapter.addModuleInfo(new ModuleInfo().moduleName(getString(R.string.range_audio)));
        mainAdapter.addModuleInfo(new ModuleInfo().moduleName(getString(R.string.subject_segmentation)).titleName(getString(R.string.others)));
        mainAdapter.addModuleInfo(new ModuleInfo().moduleName(getString(R.string.super_resolution)));
        mainAdapter.addModuleInfo(new ModuleInfo().moduleName(getString(R.string.beautify_watermark_snapshot)));
        mainAdapter.addModuleInfo(new ModuleInfo().moduleName(getString(R.string.effects_beauty)));
        mainAdapter.addModuleInfo(new ModuleInfo().moduleName(getString(R.string.stream_mixing)));
        mainAdapter.addModuleInfo(new ModuleInfo().moduleName(getString(R.string.recording)));
        mainAdapter.addModuleInfo(new ModuleInfo().moduleName(getString(R.string.media_player)));
        mainAdapter.addModuleInfo(new ModuleInfo().moduleName(getString(R.string.camera)));
        mainAdapter.addModuleInfo(new ModuleInfo().moduleName(getString(R.string.multiple_rooms)));
        mainAdapter.addModuleInfo(new ModuleInfo().moduleName(getString(R.string.flow_control)));
        mainAdapter.addModuleInfo(new ModuleInfo().moduleName(getString(R.string.network_and_performance)));
        mainAdapter.addModuleInfo(new ModuleInfo().moduleName(getString(R.string.security)));
        mainAdapter.addModuleInfo(new ModuleInfo().moduleName(getString(R.string.screen_sharing)));
        mainAdapter.addModuleInfo(new ModuleInfo().moduleName(getString(R.string.sei)));
        mainAdapter.addModuleInfo(new ModuleInfo().moduleName(getString(R.string.multi_video_source)));
        mainAdapter.addModuleInfo(new ModuleInfo().moduleName(getString(R.string.log_version_debug)).titleName(getString(R.string.debug_config)));
    }

    // 需要申请 麦克风权限-读写sd卡权限-摄像头权限
    private static String[] PERMISSIONS_STORAGE = {
            "android.permission.CAMERA",
            "android.permission.RECORD_AUDIO",
            "android.permission.WRITE_EXTERNAL_STORAGE"};

    /**
     * 校验并请求权限
     */
    public boolean checkOrRequestPermission(int code) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (ContextCompat.checkSelfPermission(this, "android.permission.CAMERA") != PackageManager.PERMISSION_GRANTED
                    || ContextCompat.checkSelfPermission(this, "android.permission.RECORD_AUDIO") != PackageManager.PERMISSION_GRANTED
                    || ContextCompat.checkSelfPermission(this, "android.permission.WRITE_EXTERNAL_STORAGE") != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(PERMISSIONS_STORAGE, code);
                return false;
            }
        }
        return true;
    }
}
