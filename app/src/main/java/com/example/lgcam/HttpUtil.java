package com.example.lgcam;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import static android.content.ContentValues.TAG;

/**
 * Created by Administrator on 2017/9/24.
 */

public class HttpUtil {
    /**
     * 发送网络请求的时候调用该方法
     * @param //address URL对象
     * @param listener  实例化的接口对象，回调方法需要实例化接口
     */
    public static void sendHttpRequest( final HttpCallbackListener listener) {
        new Thread() {
            protected URL mURL;
            private HttpURLConnection mHttpURLConnection;
            private String mHttpRequestMethod = "POST";
            private String mHttpRequestData;

            public Handler mHandler;

            public void run() {
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
                    BufferedReader in = new BufferedReader(new InputStreamReader(is));
                    StringBuffer aresponse = new StringBuffer();
                    String inputLine;
                    while ((inputLine = in.readLine()) != null) {
                        aresponse.append(inputLine);
                    }
                    //Log.v(TAG, "is: " + is);
                    //is.close();

                    //Object response = parseResponse(is);
                    //Object response = aresponse.toString();

                    if (listener != null) {
                        // 回调onFinish()方法
                        //listener.onFinish(response);
                        listener.onFinish(is);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    // 回调onError()方法
                    listener.onError(e);
                } finally {
                    if (mHttpURLConnection != null) {
                        mHttpURLConnection.disconnect();//连接不为空就关闭连接
                    }
                }
            };
        }.start();
    }

    public interface HttpCallbackListener{
        //void onFinish(Object response);
        void onFinish(InputStream inputStream);
        void onError(Exception e);
    }

}
