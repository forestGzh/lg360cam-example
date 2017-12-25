package com.example.lgcam;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.opengl.GLES20;
import android.opengl.GLUtils;
import android.opengl.Matrix;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import com.google.vr.sdk.base.Eye;
import com.google.vr.sdk.base.GvrActivity;
import com.google.vr.sdk.base.GvrView;
import com.google.vr.sdk.base.HeadTransform;
import com.google.vr.sdk.base.Viewport;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.Socket;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;

import javax.microedition.khronos.egl.EGL10;
import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.egl.EGLContext;
import javax.microedition.khronos.egl.EGLDisplay;

import static android.content.ContentValues.TAG;

public class GetaLiveActivity extends GvrActivity implements GvrView.StereoRenderer {

    protected float[] modelCube = new float[16];
    protected float[] modelPosition = new float[] {0.0f, 0.0f, -MAX_MODEL_DISTANCE / 2.0f};
    private static final float Z_NEAR = 0.1f;
    private static final float Z_FAR = 1000.0f;
    private static final float CAMERA_Z = 0.01f;
    private static final int COORDS_PER_VERTEX = 3;

    private FloatBuffer cubeVertices;
    private FloatBuffer mUvTexVertex;
    private ShortBuffer cubeVerticesINdex;

    private int cubeProgram;
    private int cubePositionParam;
    private int cubeModelViewProjectionParam;

    private int mTexCoordHandle;
    private int mTexSamplerHandle;

    private float[] camera = new float[16];
    private float[] view = new float[16];
    private float[] headView = new float[16];
    private float[] modelViewProjection = new float[16];
    private float[] modelView = new float[16];
    private float[] headRotation = new float[4];;

    //private static final float MAX_MODEL_DISTANCE = 7.0f;
    private static final float MAX_MODEL_DISTANCE = 14.0f;

    private int TexName;

    Bitmap abitmap;

    public Handler mHandler;
    private Bitmap mbitmap;

    protected URL mURL;
    private HttpURLConnection mHttpURLConnection;
    private String mHttpRequestMethod = "POST";
    private String mHttpRequestData;


    private int mCount;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        initializeGvrView();
        //bitmap = BitmapFactory.decodeResource(getResources(),R.drawable.pic);
    }
    public void initializeGvrView() {
        setContentView(R.layout.activity_geta_live);
        GvrView gvrView = (GvrView) findViewById(R.id.gvr_viewsphere);
        //gvrView.setEGLConfigChooser(8, 8, 8, 8, 16, 8);
        //gvrView.setRenderer(this);
        gvrView.enableCardboardTriggerEmulation();
        gvrView.setAsyncReprojectionEnabled(false);
        setGvrView(gvrView);
        gvrView.setRenderer(this);

        mHandler = new Handler()
        {
            @Override
            public void handleMessage(Message msg)
            {
                //对msg进行处理，获得其中的数据也就是计算结果
                //super.handleMessage(msg);
                mbitmap =  (Bitmap) msg.obj;
                int screenWidth = getMaximumTextureSize();
                int screenHeight = screenWidth / 2;
                abitmap = scaleBitmap(mbitmap, screenWidth, screenHeight);
            }
        };
        new Getpic().start();
    }

    public static Bitmap scaleBitmap(Bitmap srcBmp, int iWidth, int iHeight) {
        float fWidth = srcBmp.getWidth();
        float fHeight = srcBmp.getHeight();

        if (fWidth > iWidth) {
            float mWidth = (float) (fWidth / 100);
            float fScale = (float) (iWidth / mWidth);
            fWidth *= (fScale / 100);
            fHeight *= (fScale / 100);
        } else if (fHeight > iHeight) {
            float mHeight = (float) (fHeight / 100);
            float fScale = (float) (iHeight / mHeight);
            fWidth *= (fScale / 100);
            fHeight *= (fScale / 100);
        }
        return Bitmap.createScaledBitmap(srcBmp, (int) fWidth, (int) fHeight, true);
    }

    public static int getMaximumTextureSize() {
        EGL10 egl = (EGL10) EGLContext.getEGL();
        EGLDisplay display = egl.eglGetDisplay(EGL10.EGL_DEFAULT_DISPLAY);

        int[] version = new int[2];
        egl.eglInitialize(display, version);

        int[] totalConfigurations = new int[1];
        egl.eglGetConfigs(display, null, 0, totalConfigurations);

        EGLConfig[] configurationsList = new EGLConfig[totalConfigurations[0]];
        egl.eglGetConfigs(display, configurationsList, totalConfigurations[0], totalConfigurations);

        int[] textureSize = new int[1];
        int maximumTextureSize = 0;

        for (int i = 0; i < totalConfigurations[0]; i++) {
            egl.eglGetConfigAttrib(display, configurationsList[i], EGL10.EGL_MAX_PBUFFER_WIDTH, textureSize);
            if (maximumTextureSize < textureSize[0]) {
                maximumTextureSize = textureSize[0];
            }
        }
        egl.eglTerminate(display);

        return maximumTextureSize;
    }

    @Override
    public void onPause() { super.onPause();}
    @Override
    public void onResume() {
        super.onResume();
    }
    @Override
    public void onRendererShutdown() {}
    @Override
    public void onSurfaceChanged(int width, int height) {}

    @Override
    public void onSurfaceCreated(EGLConfig config) {
        Log.d("GLSurfaceView", "surface created");
        GLES20.glClearColor(0.1f, 0.1f, 0.1f, 0.5f);// dark background so text shows up well.

        SkySphere data = new SkySphere();

        ByteBuffer bbVertices = ByteBuffer.allocateDirect(data.vertices.length * 4);
        bbVertices.order(ByteOrder.nativeOrder());
        cubeVertices = bbVertices.asFloatBuffer();
        cubeVertices.put(data.vertices);
        cubeVertices.position(0);

        ByteBuffer mVertexIndexBuffer = ByteBuffer.allocateDirect(data.indices.length * 2);
        mVertexIndexBuffer.order(ByteOrder.nativeOrder());
        cubeVerticesINdex= mVertexIndexBuffer.asShortBuffer();
        cubeVerticesINdex.put(data.indices);
        cubeVerticesINdex.position(0);

        mCount = data.indices.length;

        ByteBuffer mUvTexVertexBuffer = ByteBuffer.allocateDirect(data.textcoords.length * 4);//////////
        mUvTexVertexBuffer.order(ByteOrder.nativeOrder());//////////////
        mUvTexVertex=mUvTexVertexBuffer.asFloatBuffer();//////////////
        mUvTexVertex.put(data.textcoords);//////////////
        mUvTexVertex.position(0);//////////////

        int vertexShader = loadGLShader(GLES20.GL_VERTEX_SHADER, R.raw.light_vertex);
        int passthroughShader = loadGLShader(GLES20.GL_FRAGMENT_SHADER, R.raw.passthrough_fragment);

        cubeProgram = GLES20.glCreateProgram();//创建一个空的OpenGL ES Program
        GLES20.glAttachShader(cubeProgram, vertexShader);//将vertex shader添加到program
        GLES20.glAttachShader(cubeProgram, passthroughShader);
        GLES20.glLinkProgram(cubeProgram);//创建可执行的 OpenGL ES program
        GLES20.glUseProgram(cubeProgram);//将program加入OpenGL ES环境中
        cubePositionParam = GLES20.glGetAttribLocation(cubeProgram, "a_Position");//获取指向vertex shader的成员a_Position的 cubePositionParam
        cubeModelViewProjectionParam = GLES20.glGetUniformLocation(cubeProgram, "u_MVP");
        mTexCoordHandle = GLES20.glGetAttribLocation(cubeProgram, "a_texCoord");////////////
        mTexSamplerHandle = GLES20.glGetUniformLocation(cubeProgram, "s_texture");////////////

        updateModelPosition();
    }
    protected void updateModelPosition() {
        Matrix.setIdentityM(modelCube, 0);
        Matrix.translateM(modelCube, 0, modelPosition[0], modelPosition[1], modelPosition[2]);//偏移量 xyz平移量
    }
    @Override
    public void onNewFrame(HeadTransform headTransform) {
        // Build the camera matrix and apply it to the ModelView.
        //Matrix.setLookAtM(camera, 0, 0.0f, 0.0f, CAMERA_Z, 0.0f, 0.0f, 0.0f, 0.0f, 1.0f, 0.0f);//相機視角，眼睛相對物體的位置改變，设置相机的位置(视口矩阵)
        //Matrix.setLookAtM（mVMatrix,0,//偏移量cx, cy, cz,//相机位置,tx, ty, tz,//观察点位置upx, upy, upz//顶部朝向）
        Matrix.setLookAtM(camera, 0, 0.0f, 0.0f, CAMERA_Z, 0.0f, 0.0f, 0.0f, 0.0f, 1.0f, 0.0f);
        headTransform.getHeadView(headView, 0);//表示从相机到头部的变换的矩阵。头部原点被定义为两只眼睛之间的中心点,参数，headview要写入4x4列主要转换矩阵的数组
        // Update the 3d audio engine with the most recent head rotation.
        headTransform.getQuaternion(headRotation, 0);//Provides the quaternion representing the head rotation.
        Log.d("GLSurfaceView", "onnewframe");
        GLES20.glUseProgram(cubeProgram);
        GLES20.glDeleteTextures(1, new int[] { TexName }, 0);
        teximage();
    }
    @Override
    public void onDrawEye(Eye eye) {
        Log.d("GLSurfaceView", "ondraweye");
        GLES20.glEnable(GLES20.GL_DEPTH_TEST);
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);
        Matrix.multiplyMM(view, 0, eye.getEyeView(), 0, camera, 0);//计算投影和视口变换  getEyeView返回从相机转换为当前眼睛的矩阵
        float[] perspective = eye.getPerspective(Z_NEAR, Z_FAR);//返回这个眼睛的透视投影矩阵
        Matrix.multiplyMM(modelView, 0, view, 0, modelCube, 0);
        Matrix.multiplyMM(modelViewProjection, 0, perspective, 0, modelView, 0);
        //teximage();
        draw();
    }
    public void draw(){
        GLES20.glVertexAttribPointer(cubePositionParam, COORDS_PER_VERTEX, GLES20.GL_FLOAT, false, 0, cubeVertices);//准备圖形的坐标数据
        GLES20.glVertexAttribPointer(mTexCoordHandle, 2, GLES20.GL_FLOAT, false, 0, mUvTexVertex);
        GLES20.glUniformMatrix4fv(cubeModelViewProjectionParam, 1, false, modelViewProjection, 0);
        GLES20.glUniform1i(mTexSamplerHandle, 0);
        GLES20.glEnableVertexAttribArray(cubePositionParam);//启用一个指向圖形的顶点数组的cubePositionParam
        GLES20.glEnableVertexAttribArray(mTexCoordHandle);
        GLES20.glDrawElements(GLES20.GL_TRIANGLES, mCount, GLES20.GL_UNSIGNED_SHORT, cubeVerticesINdex);
        Log.d("GLSurfaceView", "huatu");
        // Disable vertex arrays
        GLES20.glDisableVertexAttribArray(cubePositionParam);//禁用指向圖形的顶点数组对于 attribute 类型的变量，我们需要先 enable，再赋值，绘制完毕之后再 disable。我们可以通过 GLES20.glDrawArrays 或者 GLES20.glDrawElements 开始绘制。注意，执行完毕之后，GPU 就在显存中处理好帧数据了，但此时并没有更新到 surface 上，是 GLSurfaceView 会在调用 renderer.onDrawFrame 之后，调用 mEglHelper.swap()，来把显存的帧数据更新到 surface 上
        GLES20.glDisableVertexAttribArray(mTexCoordHandle);////////////////////
        //GLES20.glDeleteTextures(1, new int[] { TexName }, 0);
    }
    public void teximage(){
        int[] mTexNames = new int[1];
        GLES20.glGenTextures(1, mTexNames, 0);
        TexName = mTexNames[0];
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, TexName);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER,
                GLES20.GL_LINEAR);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER,
                GLES20.GL_LINEAR);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S,
                GLES20.GL_REPEAT);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T,
                GLES20.GL_REPEAT);
        GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, abitmap, 0);
        Log.d("GLSurfaceView", "teximage");
    }
    /*class drawvideo extends Thread{
        public void run(){

        }
    }*/


    @Override
    public void onFinishFrame(Viewport viewport) {}

    private int loadGLShader(int type, int resId) {
        String code = readRawTextFile(resId);
        int shader = GLES20.glCreateShader(type);
        GLES20.glShaderSource(shader, code);
        GLES20.glCompileShader(shader);
        // Get the compilation status.
        final int[] compileStatus = new int[1];
        GLES20.glGetShaderiv(shader, GLES20.GL_COMPILE_STATUS, compileStatus, 0);
        // If the compilation failed, delete the shader.
        if (compileStatus[0] == 0) {
            GLES20.glDeleteShader(shader);
            shader = 0;
        }
        if (shader == 0) {
            throw new RuntimeException("Error creating shader.");
        }
        return shader;
    }
    private String readRawTextFile(int resId) {
        InputStream inputStream = getResources().openRawResource(resId);
        try {
            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line).append("\n");
            }
            reader.close();
            return sb.toString();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    class Getpic extends Thread{
        public Getpic(){}
        public void run(){
            while (true){
                try {
                    JSONObject data = new JSONObject();
                    try {
                        data.put("name", "camera.getLivePreview");
                    } catch (JSONException e) {
                        Log.v(TAG, "Error: Json error for put data in function");
                        e.printStackTrace();
                    }
                    mHttpRequestData = data.toString();

                    mURL = new URL("http://192.168.43.1:6624/osc/commands/execute");
                    mHttpURLConnection = (HttpURLConnection) mURL.openConnection();

                    mHttpURLConnection.setRequestProperty("Host", "192.168.43.1:6624");
                    mHttpURLConnection.setRequestProperty("Content-Type", "application/json;charset=utf-8");
                    mHttpURLConnection.setRequestProperty("Accept", "image/jpeg");
                    mHttpURLConnection.setRequestProperty("Content-Length", String.valueOf(mHttpRequestData.length()));
                    mHttpURLConnection.setRequestProperty("X-XSRF-Protected", "1");


                    //Send request
                    mHttpURLConnection.setDoInput(true);
                    mHttpURLConnection.setRequestMethod(mHttpRequestMethod);
                    mHttpURLConnection.setDoOutput(true);

                    //mHttpURLConnection.connect();
                    if (mHttpRequestData != null) {
                        OutputStream os = mHttpURLConnection.getOutputStream();
                        os.write(mHttpRequestData.getBytes("UTF-8"));
                        os.flush();
                        os.close();
                        Log.v(TAG, "OutputData:" + ": " + mHttpRequestData);
                    }
                    //Get request result
                    int responseCode = mHttpURLConnection.getResponseCode();
                    Log.v(TAG, "ResponseCode: " + responseCode);
                    InputStream is;
                    //is = mHttpURLConnection.getInputStream();
                    //Log.v(TAG, "Inputstream: " + is.toString());
                    if(responseCode == 200) {
                        //Normal response
                        is = mHttpURLConnection.getInputStream();
                        //errorFlag = false;
                    } else{
                        //Error response
                        is = mHttpURLConnection.getErrorStream();
                        //errorFlag = true;
                    }
                    //is.close();
                    mbitmap = BitmapFactory.decodeStream(is);
                    Message msg = Message.obtain();
                    msg.obj = mbitmap;
                    mHandler.sendMessage(msg);

                    //Object response = parseResponse(is);
                    //Object response = aresponse.toString();

                    //if (listener != null) {
                    // 回调onFinish()方法
                    //listener.onFinish(response);
                    //listener.onFinish(is);
                    // }
                } catch (Exception e) {
                    e.printStackTrace();
                    // 回调onError()方法
                    //listener.onError(e);
                }

            }
        }

    }
}

