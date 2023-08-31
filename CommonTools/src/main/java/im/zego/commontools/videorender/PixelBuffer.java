package im.zego.commontools.videorender;

import java.nio.ByteBuffer;

/**
 * @author changwei on 2023/7/13 14:13
 */
public class PixelBuffer {
    public int width;
    public int height;
    public ByteBuffer[] buffer;
    public int[] strides;
    public int flip;
}
