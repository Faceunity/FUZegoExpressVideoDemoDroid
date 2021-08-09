# FuZegoExpressVideo 快速接入文档

FuZegoExpressVideo 集成了 FaceUnity 美颜道具贴纸功能和即构 **[极速视频](https://doc-zh.zego.im/zh/693.html)**。

本文是 FaceUnity SDK 快速对即构极速视频的导读说明，SDK 版本为 **7.4.1.0**。关于 SDK 的详细说明，请参看 **[FULiveDemoDroid](https://github.com/Faceunity/FULiveDemoDroid/)**。

## 快速集成方法

### 一、添加 SDK

### 1. build.gradle配置

#### 1.1 allprojects配置
```java
allprojects {
    repositories {
        ...
        maven { url 'http://maven.faceunity.com/repository/maven-public/' }
        ...
  }
}
```

#### 1.2 dependencies导入依赖
```java
dependencies {
...
implementation 'com.faceunity:core:7.4.1.0' // 实现代码
implementation 'com.faceunity:model:7.4.1.0' // 道具以及AI bundle
...
}
```

##### 备注

集成参考文档：FULiveDemoDroid 工程 doc目录

### 2. 其他接入方式-底层库依赖

```java
dependencies {
...
implementation 'com.faceunity:nama:7.4.1.0' //底层库-标准版
implementation 'com.faceunity:nama-lite:7.4.1.0' //底层库-lite版
...
}
```

  如需指定应用的 so 架构，请修改 app 模块 build.gradle：

  ```groovy
  android {
      // ...
      defaultConfig {
          // ...
          ndk {
              abiFilters 'armeabi-v7a', 'arm64-v8a'
          }
      }
  }
  ```

  如需剔除不必要的 assets 文件，请修改 app 模块 build.gradle：

  ```groovy
  android {
      // ...
      applicationVariants.all { variant ->
          variant.mergeAssetsProvider.configure {
              doLast {
                  delete(fileTree(dir: outputDir, includes: ['model/ai_face_processor_lite.bundle',
                                                             'model/ai_hand_processor.bundle',
                                                             'graphics/controller.bundle',
                                                             'graphics/fuzzytoonfilter.bundle',
                                                             'graphics/fxaa.bundle',
                                                             'graphics/tongue.bundle']))
              }
          }
      }
  }
  ```

###

### 二、使用 SDK

#### 1. 初始化

在 `FURenderer` 类 的  `setup` 静态方法是对 FaceUnity SDK 一些全局数据初始化的封装，可以在 Application 中调用，也可以在工作线程调用，仅需初始化一次即可。

当前demo在 ZegoApplication 类中执行。

#### 2.创建

在 `FaceUnityDataFactory` 类 的  `bindCurrentRenderer` 方法是对 FaceUnity SDK 每次使用前数据初始化的封装。

在 FuCaptureRenderActivity 类中 设置 OnGlRendererListener回调方法，且在onSurfaceCreated方法中执行。

```
    @Override
    public void onRenderAfter(@NotNull FURenderOutputData fuRenderOutputData, @NotNull FURenderFrameData fuRenderFrameData) {
        //这里进数据流推送
        if (mCSVUtils != null) {
            long renderTime = System.nanoTime() - start;
            mCSVUtils.writeCsv(null, renderTime);
        }

        if (fuRenderOutputData.getImage() != null && fuRenderOutputData.getImage().getBuffer() != null) {
            // 使用采集视频帧信息构造VideoCaptureFormat
            // Constructing VideoCaptureFormat using captured video frame information
            ZegoVideoFrameParam param = new ZegoVideoFrameParam();
            param.width = fuRenderOutputData.getImage().getWidth();
            param.height = fuRenderOutputData.getImage().getHeight();
            param.strides[0] = fuRenderOutputData.getImage().getWidth();
            param.strides[1] = fuRenderOutputData.getImage().getWidth();
            param.format = ZegoVideoFrameFormat.NV21;
            param.rotation = 180;

            long now; //部分机型存在 surfaceTexture 时间戳不准确的问题
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
                now = SystemClock.elapsedRealtime();
            } else {
                now = TimeUnit.MILLISECONDS.toMillis(SystemClock.elapsedRealtime());
            }
            // 将采集的数据传给ZEGO SDKc
            // Pass the collected data to ZEGO SDK
            if (byteBuffer == null) {
                byteBuffer = ByteBuffer.allocateDirect(fuRenderOutputData.getImage().getBuffer().length);
            }
            byteBuffer.put(fuRenderOutputData.getImage().getBuffer());
            byteBuffer.flip();
            mSDKEngine.sendCustomVideoCaptureRawData(byteBuffer, byteBuffer.limit(), param, now);
        }
    }

    long start = 0;

    @Override
    public void onRenderBefore(@Nullable FURenderInputData fuRenderInputData) {
        if (mCSVUtils != null) {
            start = System.nanoTime();
        }
        if (fuRenderInputData != null && fuRenderInputData.getRenderConfig() != null)
            fuRenderInputData.getRenderConfig().setNeedBufferReturn(true);
    }

    @Override
    public void onDrawFrameAfter() {
    }

    @Override
    public void onSurfaceChanged(int i, int i1) {

    }

    @Override
    public void onSurfaceCreated() {
        mFaceUnityDataFactory.bindCurrentRenderer();
    }

    @Override
    public void onSurfaceDestroy() {
        FURenderKit.getInstance().release();
    }
```

#### 3. 图像处理
在 `FURenderKit` 类 的  `renderWithInput` 方法是对 FaceUnity SDK 图像处理方法的封装，该方法有许多重载方法适用于不同的数据类型需求。
具体的图像处理在本例中已经封装在CameraRenderer中，本例在FuCaptureRenderActivity 类中 设置 OnGlRendererListener回调方法在onRenderAfter方法中图像已处理完成。（代码如上）

#### 4. 销毁
在 `FURenderKit` 类 的  `release` 方法是对 FaceUnity SDK 数据销毁的封装。
本例在FuCaptureRenderActivity 类中 设置 OnGlRendererListener回调方法在onSurfaceDestroy方法中图像已处理完成。（代码如上）

#### 5. 切换相机
CameraRender提供了switchCamera方法切换相机。

#### 6. 旋转手机
该功能已经在CameraRender中通过SensorManager重新设置deviceOrientation实现。

上面一系列方法的使用，具体在 demo 中的 `AVStreamingActivity`类，参考该代码示例接入即可。

### 三、接口介绍

- IFURenderer 是核心接口，提供了创建、销毁、渲染等接口。
- FaceUnityDataFactory 控制四个功能模块，用于功能模块的切换，初始化
- FaceBeautyDataFactory 是美颜业务工厂，用于调整美颜参数。
- PropDataFactory 是道具业务工厂，用于加载贴纸效果。
- MakeupDataFactory 是美妆业务工厂，用于加载美妆效果。
- BodyBeautyDataFactory 是美体业务工厂，用于调整美体参数。

关于 SDK 的更多详细说明，请参看 **[FULiveDemoDroid](https://github.com/Faceunity/FULiveDemoDroid/)**。如有对接问题，请联系技术支持。