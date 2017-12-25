package com.example.lgcam;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;

import static android.content.ContentValues.TAG;

import com.lge.octopus.tentacles.wifi.client.WifiClient;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Objects;


public class MainActivity extends AppCompatActivity {
    private EditText editTextIPAddress;
    private Button buttonTakePicture;
    private Button buttonConnect;
    private ImageView alive;

    public Handler mHandler;
    private Bitmap mbitmap;

    protected URL mURL;
    private HttpURLConnection mHttpURLConnection;
    private String mHttpRequestMethod = "POST";
    private String mHttpRequestData;

    private Context mContext;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main_activity);

        editTextIPAddress = (EditText) findViewById(R.id.editTextIPAddr);
        buttonTakePicture = (Button) findViewById(R.id.button_takePicture);
        buttonConnect = (Button) findViewById(R.id.button_connect);
        alive = (ImageView) findViewById(R.id.image_live);

        mContext = this;

        mHandler = new Handler()
        {
            @Override
            public void handleMessage(Message msg)
            {
                mbitmap =  (Bitmap) msg.obj;
            }
        };

        //new Getpic().start();

        //Send 'take picture' request to camera
        //Need to start session before taking picture
        buttonTakePicture.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //getalive();
                //getpicture();
                //new Getpic().start();
                alive.setImageBitmap(mbitmap);

                Intent i = new Intent(mContext, GetaLiveActivity.class);
                startActivity(i);
            }
        });

        buttonConnect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent i = new Intent(mContext, ConnectionActivity.class);
                startActivity(i);
            }
        });
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

