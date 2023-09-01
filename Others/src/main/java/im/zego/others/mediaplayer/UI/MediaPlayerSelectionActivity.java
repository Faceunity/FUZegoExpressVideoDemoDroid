package im.zego.others.mediaplayer.UI;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatSpinner;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.app.Activity;
import android.content.Intent;
import android.content.res.AssetManager;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.Switch;

import im.zego.others.R;
import im.zego.others.mediaplayer.adapter.MainAdapter;
import im.zego.others.mediaplayer.entity.ModuleInfo;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import im.zego.commontools.logtools.AppLogger;
import im.zego.commontools.logtools.LogView;
import im.zego.commontools.logtools.logLinearLayout;
import im.zego.others.beautyandwatermarkandsnapshot.ImageFilePath;

public class MediaPlayerSelectionActivity extends AppCompatActivity {

    RecyclerView mediaList;
    EditText urlEidt;
    Button enterButton;
    AppCompatSpinner alphaLayoutSpinner;
    Switch alphaBlendSwitch;
    Button buttonChooseLocalFileButton;
    Switch enableHardwareSwitch;

    MainAdapter mainAdapter = new MainAdapter();

    int alphaLayout;
    boolean alphaBlend;
    boolean mediaHardwareDecode;

    final List<String> fileNames = new ArrayList<>();
    public static final int PICK_IMAGE = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_media_selection_player);
        bindView();
        initData();
        setDefaultValue();
        setUI();
        setItem();
        setLogComponent();
    }
    public void bindView(){
        mediaList = findViewById(R.id.mediaList);
        urlEidt = findViewById(R.id.urlInput);
        enterButton = findViewById(R.id.enterButton);
        alphaLayoutSpinner = findViewById(R.id.alphaLayoutSpinner);
        alphaBlendSwitch = findViewById(R.id.switchAlphaBlend);
        buttonChooseLocalFileButton = findViewById(R.id.buttonChooseLocalFileButton);
        enableHardwareSwitch = findViewById(R.id.hardwareDecoderSwitch);
    }
    public void setDefaultValue(){
        setTitle(getString(R.string.media_player));
        alphaLayout = 0;
        alphaBlend = true;
        mediaHardwareDecode = true;
    }

    public void setUI(){
        mainAdapter.setOnItemClickListener((view, position) -> {
            ModuleInfo moduleInfo = (ModuleInfo) view.getTag();
            String module = moduleInfo.getModule();
            if (module.equals(getString(R.string.sample))) {
                startMediaPlayer( getExternalFilesDir("").getPath()+"/sample.mp3", false);
            } else if (module.equals(getString(R.string.test))){
                startMediaPlayer(getExternalFilesDir("").getPath()+"/test.wav", false);
            } else  if (module.equals(getString(R.string.ad))){
                startMediaPlayer(getExternalFilesDir("").getPath()+"/ad.mp4", true);
            } else if (module.equals(getString(R.string.sample_bgm))){
                startMediaPlayer("https://storage.zego.im/demo/sample_astrix.mp3", false);
            } else if (module.equals(getString(R.string.sample_network))){
                startMediaPlayer("https://storage.zego.im/demo/201808270915.mp4", true);
            }else if(module.equals("source1_complex_rl.mp4")){
                startMediaPlayer(getExternalFilesDir("").getPath()+"/source1_complex_rl.mp4", true);
            }else if(module.equals("source2_complex_rl.mp4")){
                startMediaPlayer(getExternalFilesDir("").getPath()+"/source2_complex_rl.mp4", true);
            }else if(module.equals("source3_complex_rl.mp4")){
                startMediaPlayer(getExternalFilesDir("").getPath()+"/source3_complex_rl.mp4", true);
            }else if(module.equals("source4_complex_rl.mp4")){
                startMediaPlayer(getExternalFilesDir("").getPath()+"/source4_complex_rl.mp4", true);
            }else if(module.equals("source1_complex_lr.mp4")){
                startMediaPlayer(getExternalFilesDir("").getPath()+"/source1_complex_lr.mp4", true);
            }else if(module.equals("source2_complex_lr.mp4")){
                startMediaPlayer(getExternalFilesDir("").getPath()+"/source2_complex_lr.mp4", true);
            }else if(module.equals("source3_complex_lr.mp4")){
                startMediaPlayer(getExternalFilesDir("").getPath()+"/source3_complex_lr.mp4", true);
            }else if(module.equals("source4_complex_lr.mp4")){
                startMediaPlayer(getExternalFilesDir("").getPath()+"/source4_complex_lr.mp4", true);
            }else if(module.equals("source1_complex_bt.mp4")){
                startMediaPlayer(getExternalFilesDir("").getPath()+"/source1_complex_bt.mp4", true);
            }else if(module.equals("source2_complex_bt.mp4")){
                startMediaPlayer(getExternalFilesDir("").getPath()+"/source2_complex_bt.mp4", true);
            }else if(module.equals("source3_complex_bt.mp4")){
                startMediaPlayer(getExternalFilesDir("").getPath()+"/source3_complex_bt.mp4", true);
            }else if(module.equals("source4_complex_bt.mp4")){
                startMediaPlayer(getExternalFilesDir("").getPath()+"/source4_complex_bt.mp4", true);
            }
        });

        enterButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String resourceURL = urlEidt.getText().toString();
                startMediaPlayer(resourceURL, true);
            }
        });

        alphaLayoutSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String[] options = getResources().getStringArray(R.array.alphaLayouts);
                switch (options[position]) {
                    case "None":
                        alphaLayout = 0;
                        AppLogger.getInstance().callApi("Set alpha layout: None");
                        break;
                    case "AlphaLeft":
                        alphaLayout = 1;
                        AppLogger.getInstance().callApi("Set alpha layout: Left");
                        break;
                    case "AlphaRight":
                        alphaLayout = 2;
                        AppLogger.getInstance().callApi("Set alpha layout: Right");
                        break;
                    case "AlphaBottom":
                        alphaLayout = 3;
                        AppLogger.getInstance().callApi("Set alpha layout: Bottom");
                        break;
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });

        alphaBlendSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                alphaBlend = isChecked;
                AppLogger.getInstance().i("Set alpha blend: %b", alphaBlend);
            }
        });

        buttonChooseLocalFileButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //choose the watermark from the file.
                Intent getIntent = new Intent(Intent.ACTION_GET_CONTENT);
                getIntent.setType("*/*");

                Intent pickIntent = new Intent(Intent.ACTION_PICK, android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                pickIntent.setType("*/*");

                Intent chooserIntent = Intent.createChooser(getIntent, "Select media file");
                chooserIntent.putExtra(Intent.EXTRA_INITIAL_INTENTS, new Intent[] {pickIntent});

                startActivityForResult(chooserIntent, PICK_IMAGE);
            }
        });

        enableHardwareSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                mediaHardwareDecode = isChecked;
            }
        });
    }
    public void setItem(){
        mainAdapter.addModuleInfo(new ModuleInfo().moduleName(getString(R.string.sample_bgm)).titleName(getString(R.string.network_resource)));
        mainAdapter.addModuleInfo(new ModuleInfo().moduleName(getString(R.string.sample_network)));
        mainAdapter.addModuleInfo(new ModuleInfo().moduleName(getString(R.string.sample)).titleName(getString(R.string.local_resource)));
        mainAdapter.addModuleInfo(new ModuleInfo().moduleName(getString(R.string.ad)));
        mainAdapter.addModuleInfo(new ModuleInfo().moduleName(getString(R.string.test)));
        mainAdapter.addModuleInfo(new ModuleInfo().moduleName("source1_complex_rl.mp4"));
        mainAdapter.addModuleInfo(new ModuleInfo().moduleName("source2_complex_rl.mp4"));
        mainAdapter.addModuleInfo(new ModuleInfo().moduleName("source3_complex_rl.mp4"));
        mainAdapter.addModuleInfo(new ModuleInfo().moduleName("source4_complex_rl.mp4"));
        mainAdapter.addModuleInfo(new ModuleInfo().moduleName("source1_complex_lr.mp4"));
        mainAdapter.addModuleInfo(new ModuleInfo().moduleName("source2_complex_lr.mp4"));
        mainAdapter.addModuleInfo(new ModuleInfo().moduleName("source3_complex_lr.mp4"));
        mainAdapter.addModuleInfo(new ModuleInfo().moduleName("source4_complex_lr.mp4"));
        mainAdapter.addModuleInfo(new ModuleInfo().moduleName("source1_complex_bt.mp4"));
        mainAdapter.addModuleInfo(new ModuleInfo().moduleName("source2_complex_bt.mp4"));
        mainAdapter.addModuleInfo(new ModuleInfo().moduleName("source3_complex_bt.mp4"));
        mainAdapter.addModuleInfo(new ModuleInfo().moduleName("source4_complex_bt.mp4"));



        mediaList.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false));
        mediaList.setAdapter(mainAdapter);
        mediaList.setItemAnimator(new DefaultItemAnimator());
    }
    public void startMediaPlayer(String path, Boolean isVideo){
        Intent intent = new Intent(this, MediaPlayerActivity.class);
        intent.putExtra("path",path);
        intent.putExtra("type",isVideo);
        intent.putExtra("alphaBlend", alphaBlend);
        intent.putExtra("alphaLayout", alphaLayout);
        intent.putExtra("mediaHardwareDecode", mediaHardwareDecode);
        this.startActivity(intent);
    }
    public static void actionStart(Activity activity) {
        Intent intent = new Intent(activity, MediaPlayerSelectionActivity.class);
        activity.startActivity(intent);
    }

    // Set log component. It includes a pop-up dialog.
    public void setLogComponent(){
        logLinearLayout logHiddenView = findViewById(R.id.logView);
        logHiddenView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                LogView logview = new LogView(getApplicationContext());
                logview.show(getSupportFragmentManager(),null);
            }
        });
    }
    private void initData() {
        fileNames.add("sample.mp3");
        fileNames.add("test.wav");
        fileNames.add("ad.mp4");
        fileNames.add("source1_complex_rl.mp4");
        fileNames.add("source1_complex_rl.mp4");
        fileNames.add("source2_complex_rl.mp4");
        fileNames.add("source3_complex_rl.mp4");
        fileNames.add("source4_complex_rl.mp4");
        fileNames.add("source1_complex_lr.mp4");
        fileNames.add("source2_complex_lr.mp4");
        fileNames.add("source3_complex_lr.mp4");
        fileNames.add("source4_complex_lr.mp4");
        fileNames.add("source1_complex_bt.mp4");
        fileNames.add("source2_complex_bt.mp4");
        fileNames.add("source3_complex_bt.mp4");
        fileNames.add("source4_complex_bt.mp4");

        copyAssetsFiles(fileNames);
    }


    private void copyAssetsFiles(final List<String> fileNames) {
        new Thread() {
            public void run() {
                for (String fileName : fileNames) {
                    copyAssetsFile(fileName);
                }
            }
        }.start();
    }

    private void copyAssetsFile(String fileName) {
        final File file = new File(getExternalFilesDir(""), fileName);
        System.out.println("File Path---->" + file.getAbsolutePath());
        if (file.exists()) {
            System.out.println("File exists");
            return;
        }
        try {
            // Get Assets.
            AssetManager assetManager = getAssets();
            InputStream is = assetManager.open(fileName);
            FileOutputStream fos = new FileOutputStream(file);
            byte[] buffer = new byte[1024];
            int len = 0;
            while ((len = is.read(buffer)) != -1) {
                fos.write(buffer, 0, len);
            }
            fos.close();
            is.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_IMAGE && data!=null) {
            // Get chosen file path
            String realPath = ImageFilePath.getPath(getApplicationContext(), data.getData());

            startMediaPlayer(realPath, true);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }
}