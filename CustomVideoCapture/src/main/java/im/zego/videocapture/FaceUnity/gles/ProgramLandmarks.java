/*
 * Copyright (C) 2011 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package im.zego.videocapture.FaceUnity.gles;

import android.hardware.Camera;
import android.opengl.GLES20;
import android.opengl.Matrix;

import im.zego.videocapture.FaceUnity.gles.core.Drawable2d;
import im.zego.videocapture.FaceUnity.gles.core.GlUtil;
import im.zego.videocapture.FaceUnity.gles.core.Program;

public class ProgramLandmarks extends Program {

    private static final String VERTEX_SHADER =
            // This matrix member variable provides a hook to manipulate
            // the coordinates of the objects that use this vertex shader
            "uniform mat4 uMVPMatrix;" +
                    "attribute vec4 vPosition;" +
                    "uniform float uPointSize;" +
                    "void main() {" +
                    // the matrix must be included as a modifier of gl_Position
                    // Note that the uMVPMatrix factor *must be first* in order
                    // for the matrix multiplication product to be correct.
                    "  gl_Position = uMVPMatrix * vPosition;" +
                    "  gl_PointSize = uPointSize;" +
                    "}";

    private static final String FRAGMENT_SHADER =
            "precision mediump float;" +
                    "uniform vec4 vColor;" +
                    "void main() {" +
                    "  gl_FragColor = vColor;" +
                    "}";

    private static final float[] POINT_COLOR = {1.0f, 0f, 0f, 1.0f};
    private static final float POINT_SIZE = 6.0f;

    private int mPositionHandle;
    private int mColorHandle;
    private int mMvpMatrixHandle;
    private int mPointSizeHandle;

    public ProgramLandmarks() {
        super(VERTEX_SHADER, FRAGMENT_SHADER);
    }

    @Override
    protected Drawable2d getDrawable2d() {
        return new Drawable2d(new float[75 * 2]);
    }

    @Override
    protected void getLocations() {
        // get handle to vertex shader's vPosition member
        mPositionHandle = GLES20.glGetAttribLocation(mProgramHandle, "vPosition");
        GlUtil.checkGlError("vPosition");
        // get handle to fragment shader's vColor member
        mColorHandle = GLES20.glGetUniformLocation(mProgramHandle, "vColor");
        GlUtil.checkGlError("vColor");
        // get handle to shape's transformation matrix
        mMvpMatrixHandle = GLES20.glGetUniformLocation(mProgramHandle, "uMVPMatrix");
        GlUtil.checkGlError("glGetUniformLocation");
        mPointSizeHandle = GLES20.glGetUniformLocation(mProgramHandle, "uPointSize");
        GlUtil.checkGlError("uPointSize");
    }

    @Override
    public void drawFrame(int textureId, float[] texMatrix, float[] mvpMatrix) {
        // Add program to OpenGL environment
        GLES20.glUseProgram(mProgramHandle);

        // Enable a handle to the triangle vertices
        GLES20.glEnableVertexAttribArray(mPositionHandle);

        // Prepare the triangle coordinate data
        GLES20.glVertexAttribPointer(
                mPositionHandle, Drawable2d.COORDS_PER_VERTEX,
                GLES20.GL_FLOAT, false,
                Drawable2d.VERTEXTURE_STRIDE, mDrawable2d.vertexArray());

        // Set color for drawing the triangle
        GLES20.glUniform4fv(mColorHandle, 1, POINT_COLOR, 0);

        // Apply the projection and view transformation
        GLES20.glUniformMatrix4fv(mMvpMatrixHandle, 1, false, mvpMatrix, 0);

        GLES20.glUniform1f(mPointSizeHandle, POINT_SIZE);

        // Draw the triangle
        GLES20.glDrawArrays(GLES20.GL_POINTS, 0, mDrawable2d.vertexCount());

        // Disable vertex array
        GLES20.glDisableVertexAttribArray(mPositionHandle);
        GLES20.glUseProgram(0);
    }

    public void drawFrame(int x, int y, int width, int height) {
        drawFrame(0, null, mMvpMatrix, x, y, width, height);
    }

    private int mCameraFacing;
    private int mCameraOrientation;
    private int mCameraWidth;
    private int mCameraHeight;
    private final float[] mMvpMatrix = new float[16];

    public void refresh(float[] landmarksData, int cameraWidth, int cameraHeight, int cameraOrientation, int cameraFacing, float[] mvpMatrix) {
        if (mCameraWidth != cameraWidth || mCameraHeight != cameraHeight
                || mCameraOrientation != cameraOrientation || mCameraFacing != cameraFacing) {
            float[] orthoMtx = new float[16];
            Matrix.orthoM(orthoMtx, 0, 0, cameraWidth, 0, cameraHeight, -1, 1);
            float[] rotateMtx = new float[16];
            Matrix.setRotateM(rotateMtx, 0, 360 - cameraOrientation, 0.0f, 0.0f, 1.0f);
            if (cameraFacing == Camera.CameraInfo.CAMERA_FACING_BACK) {
                Matrix.rotateM(rotateMtx, 0, 180, 1.0f, 0.0f, 0.0f);
            }
            float[] retMtx = new float[16];
            Matrix.multiplyMM(retMtx, 0, rotateMtx, 0, orthoMtx, 0);
            Matrix.multiplyMM(mMvpMatrix, 0, mvpMatrix, 0, retMtx, 0);

            mCameraWidth = cameraWidth;
            mCameraHeight = cameraHeight;
            mCameraOrientation = cameraOrientation;
            mCameraFacing = cameraFacing;
        }

        updateVertexArray(landmarksData);
    }
}
