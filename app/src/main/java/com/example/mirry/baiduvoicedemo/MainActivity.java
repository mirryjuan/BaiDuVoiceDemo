package com.example.mirry.baiduvoicedemo;

import android.Manifest;
import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.baidu.speech.EventListener;
import com.baidu.speech.EventManager;
import com.baidu.speech.EventManagerFactory;
import com.baidu.speech.asr.SpeechConstant;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;

public class MainActivity extends AppCompatActivity implements EventListener, View.OnClickListener {
    Button online,offline,wakeupApp;


    private EventManager asr;
    private EventManager wakeup;

    private boolean enableOffline = true; // 测试离线命令词，需要改成true

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initPermission();
        initView();
        initListener();

        //初始化EventManager类
        asr = EventManagerFactory.create(this, "asr");    //语音识别
        asr.registerListener(this); //  EventListener 中 onEvent方法

        if(wakeup == null){
            wakeup = EventManagerFactory.create(this, "wp");    //语音唤醒
            wakeup.registerListener(this); //  EventListener 中 onEvent方法
        }

        //加载离线引擎
        if (enableOffline) {
            loadOfflineEngine(); //测试离线命令词请开启, 测试 ASR_OFFLINE_ENGINE_GRAMMER_FILE_PATH 参数时开启
        }

        startWakeUp();
    }

    private void startWakeUp() {
        Map<String, Object> params = new LinkedHashMap<String, Object>();

        params.put(SpeechConstant.ACCEPT_AUDIO_VOLUME, false);
        params.put(SpeechConstant.WP_WORDS_FILE, "assets:///WakeUp.bin");

        String json = null; // 这里可以替换成你需要测试的json
        json = new JSONObject(params).toString();

        if(wakeup == null){
            wakeup = EventManagerFactory.create(this, "wp");    //语音唤醒
            wakeup.registerListener(this); //  EventListener 中 onEvent方法
        }

        wakeup.send(SpeechConstant.WAKEUP_START, json, null, 0, 0);

        Log.i(getClass().getName(), "输入参数：" + json);
    }

    private void stopWakeUp(){
        wakeup.send(SpeechConstant.WAKEUP_STOP, null, null, 0, 0);
    }

    private void initListener() {
        online.setOnClickListener(this);
        offline.setOnClickListener(this);
        wakeupApp.setOnClickListener(this);
    }

    private void initView() {
        online = (Button) findViewById(R.id.online);
        offline = (Button) findViewById(R.id.offline);
        wakeupApp = (Button) findViewById(R.id.wakeup);
    }

    private void startAsr() {
        Map<String, Object> params = new LinkedHashMap<String, Object>();
        String event = null;
        event = SpeechConstant.ASR_START; // 替换成测试的event
        if (enableOffline){
            params.put(SpeechConstant.DECODER, 2);
        }
        params.put(SpeechConstant.ACCEPT_AUDIO_VOLUME, false);
        params.put(SpeechConstant.VAD,SpeechConstant.VAD_DNN);

        params.put(SpeechConstant.NLU, "enable");
        params.put(SpeechConstant.VAD_ENDPOINT_TIMEOUT, 800);
        params.put(SpeechConstant.VAD, SpeechConstant.VAD_DNN);
        params.put(SpeechConstant.PROP ,20000);

        String json = null; //可以替换成自己的json
        json = new JSONObject(params).toString(); // 这里可以替换成你需要测试的json

        asr.send(event, json, null, 0, 0);

        Log.i(getClass().getName(), "输入参数：" + json);
    }

    private void stopAsr() {
        asr.send(SpeechConstant.ASR_STOP, null, null, 0, 0);  // 发送停止录音事件，提前结束录音等待识别结果
    }

    private void cancelAsr(){
        asr.send(SpeechConstant.ASR_CANCEL, null, null, 0, 0); // 取消本次识别，取消后将立即停止不会返回识别结果
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()){
            case R.id.online:
                startAsr();
                break;
            case R.id.offline:
                loadOfflineEngine();
                break;
            case R.id.wakeup:
                startWakeUp();
                break;
            default:
                break;
        }
    }

    private void loadOfflineEngine() {
        Map<String, Object> params = new LinkedHashMap<String, Object>();
        params.put(SpeechConstant.DECODER, 2);
        params.put(SpeechConstant.ASR_OFFLINE_ENGINE_GRAMMER_FILE_PATH, "assets://baidu_speech_grammar.bsg");

//        try {
//            // 下面这段可选，用于生成SLOT_DATA参数， 用于动态覆盖ASR_OFFLINE_ENGINE_GRAMMER_FILE_PATH文件的词条部分
//            JSONObject  json = new JSONObject();
//            json.put("name",new JSONArray().put("第三条任务").put("第四条任务"));
//            params.put(SpeechConstant.SLOT_DATA, json.toString());
//        } catch (JSONException e) {
//            e.printStackTrace();
//        }

        asr.send(SpeechConstant.ASR_KWS_LOAD_ENGINE, new JSONObject(params).toString(), null, 0, 0);
    }

    private void unloadOfflineEngine() {
        asr.send(SpeechConstant.ASR_KWS_UNLOAD_ENGINE, null, null, 0, 0);
    }

    private void initPermission(){
        String permissions[] = {
                Manifest.permission.RECORD_AUDIO,
                Manifest.permission.ACCESS_NETWORK_STATE,
                Manifest.permission.INTERNET,
                Manifest.permission.READ_PHONE_STATE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
        };

        ArrayList<String> toApplyList = new ArrayList<String>();
        for (String perm :permissions){
            if (PackageManager.PERMISSION_GRANTED != ContextCompat.checkSelfPermission(this, perm)) {
                //进入到这里代表没有权限.
                if(ActivityCompat.shouldShowRequestPermissionRationale(this,perm)){
                    //已经禁止提示了
                    Toast.makeText(MainActivity.this, "您已禁止该权限，需要重新开启。", Toast.LENGTH_SHORT).show();
                }else{
                    toApplyList.add(perm);
                }
            }
        }

        String tmpList[] = new String[toApplyList.size()];
        if (!toApplyList.isEmpty()){
            ActivityCompat.requestPermissions(this, toApplyList.toArray(tmpList), 1);
        }else{
            //语音唤醒
//            startWakeUp();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if(grantResults.length >0 &&grantResults[0]==PackageManager.PERMISSION_GRANTED){
            //用户同意授权
            //语音唤醒
//            startWakeUp();
        }else{
            //用户拒绝授权
            Intent intent = new Intent("android.settings.APPLICATION_DETAILS_SETTINGS");

            String pkg = "com.android.settings";
            String cls = "com.android.settings.applications.InstalledAppDetails";

            intent.setComponent(new ComponentName(pkg, cls));
            intent.setData(Uri.parse("package:" + getPackageName()));
            startActivity(intent);
        }
    }

    @Override
    public void onEvent(String name, String params, byte[] data, int offset, int length) {
        String logTxt = "name: " + name;
        if (params != null && !params.isEmpty()) {
            logTxt += " ;params :" + params;
        }

        if(name.equals(SpeechConstant.CALLBACK_EVENT_WAKEUP_SUCCESS)){
            try {
                JSONObject json = new JSONObject(params);
                int errorCode = json.getInt("errorCode");
                if(errorCode == 0){
                    //唤醒成功
                    Toast.makeText(this, "唤醒成功", Toast.LENGTH_SHORT).show();
                } else {
                    //唤醒失败
                    Toast.makeText(this, "唤醒失败", Toast.LENGTH_SHORT).show();
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        } else if(name.equals(SpeechConstant.CALLBACK_EVENT_WAKEUP_STOPED)){
            //唤醒已停止
            Toast.makeText(this, "唤醒停止", Toast.LENGTH_SHORT).show();
        }

        if(name.equals(SpeechConstant.CALLBACK_EVENT_ASR_LOADED)){
            Toast.makeText(this, "离线模式加载成功", Toast.LENGTH_SHORT).show();
        }

        if(name.equals(SpeechConstant.CALLBACK_EVENT_ASR_READY)){
            // 引擎就绪，可以说话，一般在收到此事件后通过UI通知用户可以说话了
            Toast.makeText(this, "开始识别", Toast.LENGTH_SHORT).show();
        }else if(name.equals(SpeechConstant.CALLBACK_EVENT_ASR_BEGIN)){
            // 检测到说话开始
            Toast.makeText(this, "开始说话", Toast.LENGTH_SHORT).show();
        }else if(name.equals(SpeechConstant.CALLBACK_EVENT_ASR_END)){
            // 检测到说话结束
            Toast.makeText(this, "结束说话", Toast.LENGTH_SHORT).show();
        }else if(name.equals(SpeechConstant.CALLBACK_EVENT_ASR_FINISH)){
            // 识别结束
            Toast.makeText(this, "结束识别", Toast.LENGTH_SHORT).show();
        }else if (name.equals(SpeechConstant.CALLBACK_EVENT_ASR_PARTIAL)) {
            //识别结果
            if (params.contains("\"nlu_result\"")) {
                if (length > 0 && data.length > 0) {
                    logTxt += ", 语义解析结果：" + new String(data, offset, length);
                }
            }
        } else if (data != null) {
            logTxt += " ;data length=" + data.length;
        }

        Log.i(getClass().getName(), logTxt);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        asr.send(SpeechConstant.ASR_CANCEL, "{}", null, 0, 0);
        wakeup.send(SpeechConstant.WAKEUP_STOP, "{}", null, 0, 0);
        if (enableOffline) {
            unloadOfflineEngine(); //测试离线命令词请开启, 测试 ASR_OFFLINE_ENGINE_GRAMMER_FILE_PATH 参数时开启
        }
    }
}
