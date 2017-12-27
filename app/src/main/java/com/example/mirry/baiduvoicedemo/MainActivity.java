package com.example.mirry.baiduvoicedemo;

import android.Manifest;
import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.baidu.speech.EventListener;
import com.baidu.speech.EventManager;
import com.baidu.speech.EventManagerFactory;
import com.baidu.speech.asr.SpeechConstant;
import com.baidu.tts.client.SpeechSynthesizer;
import com.baidu.tts.client.SpeechSynthesizerListener;
import com.baidu.tts.client.TtsMode;
import com.example.mirry.baiduvoicedemo.config.InitConfig;
import com.example.mirry.baiduvoicedemo.config.MySyntherizer;
import com.example.mirry.baiduvoicedemo.listener.UiMessageListener;
import com.example.mirry.baiduvoicedemo.utils.OfflineResource;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import static com.example.mirry.baiduvoicedemo.MainHandlerConstant.SPEECH_FINISH;

public class MainActivity extends Activity implements EventListener, View.OnClickListener {
    public static final String TAG = "MainActivity";

    TextView text;

    private EventManager asr;
    private EventManager wakeup;

    private boolean enableOffline = true; // 测试离线命令词，需要改成true

    String appId = "10502066";
    String appKey = "TOcAhOFIQx2r3UnUSk5QtRHh";
    String appSecret = "30e969283e06e3c15f0870bd92a53dce";

    // TtsMode.MIX; 离在线融合，在线优先； TtsMode.ONLINE 纯在线； 没有纯离线
    protected TtsMode ttsMode = TtsMode.MIX;

    // 离线发音选择，VOICE_FEMALE即为离线女声发音。
    // assets目录下bd_etts_speech_female.data为离线男声模型；bd_etts_speech_female.data为离线女声模型
    protected String offlineVoice = OfflineResource.VOICE_FEMALE;

    // 主控制类，所有合成控制方法从这个类开始
    protected MySyntherizer synthesizer;

    protected Handler mainHandler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mainHandler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                super.handleMessage(msg);
                handle(msg);
            }

        };

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

        startWakeUp();    //开启语音唤醒
        initialTts();     //开启语音合成
    }

    protected void handle(Message msg) {
        int what = msg.what;
        switch (what) {
            case SPEECH_FINISH:
                startAsr();
                break;
            default:
                break;
        }
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

    }

    private void initView() {
        text = (TextView) findViewById(R.id.text);
    }

    private void startAsr() {
        Map<String, Object> params = new LinkedHashMap<String, Object>();
        String event = null;
        event = SpeechConstant.ASR_START; // 替换成测试的event
        if (enableOffline){
            params.put(SpeechConstant.DECODER, 2);
        }
        params.put(SpeechConstant.ACCEPT_AUDIO_VOLUME, true);
        params.put(SpeechConstant.VAD,SpeechConstant.VAD_DNN);

        params.put(SpeechConstant.NLU, "enable");
        params.put(SpeechConstant.VAD_ENDPOINT_TIMEOUT, 800);
        params.put(SpeechConstant.VAD, SpeechConstant.VAD_DNN);
        params.put(SpeechConstant.PROP ,20000);

        String json = null; //可以替换成自己的json
        json = new JSONObject(params).toString(); // 这里可以替换成你需要测试的json

        asr.send(event, json, null, 0, 0);

        Log.i(TAG, "输入参数：" + json);
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

    protected void initialTts() {
        // 设置初始化参数
        SpeechSynthesizerListener listener = new UiMessageListener(mainHandler); // 此处可以改为 含有您业务逻辑的SpeechSynthesizerListener的实现类

        Map<String, String> params = getParams();

        // appId appKey secretKey 网站上您申请的应用获取。注意使用离线合成功能的话，需要应用中填写您app的包名。包名在build.gradle中获取。
        InitConfig initConfig = new InitConfig(appId, appKey, appSecret, ttsMode, offlineVoice, params, listener);

        synthesizer = new MySyntherizer(this, initConfig, mainHandler); // 此处可以改为MySyntherizer 了解调用过程
    }

    /**
     * 合成的参数，可以初始化时填写，也可以在合成前设置。
     *
     * @return
     */
    protected Map<String, String> getParams() {
        Map<String, String> params = new HashMap<String, String>();
        // 以下参数均为选填
        params.put(SpeechSynthesizer.PARAM_SPEAKER, "0"); // 设置在线发声音人： 0 普通女声（默认） 1 普通男声 2 特别男声 3 情感男声<度逍遥> 4 情感儿童声<度丫丫>
        params.put(SpeechSynthesizer.PARAM_VOLUME, "5"); // 设置合成的音量，0-9 ，默认 5
        params.put(SpeechSynthesizer.PARAM_SPEED, "5");// 设置合成的语速，0-9 ，默认 5
        params.put(SpeechSynthesizer.PARAM_PITCH, "5");// 设置合成的语调，0-9 ，默认 5
        params.put(SpeechSynthesizer.PARAM_MIX_MODE, SpeechSynthesizer.MIX_MODE_DEFAULT);         // 该参数设置为TtsMode.MIX生效。即纯在线模式不生效。
        return params;
    }

    /**
     * 切换离线发音。注意需要添加额外的判断：引擎在合成时该方法不能调用
     */
    private void loadModel() {
        if (offlineVoice.equals(OfflineResource.VOICE_FEMALE)) {
            offlineVoice = OfflineResource.VOICE_MALE;
        } else {
            offlineVoice = OfflineResource.VOICE_FEMALE;
        }
        int result = synthesizer.loadModel(offlineVoice);
        checkResult(result, "loadModel");
    }

    private void checkResult(int result, String method) {
        if (result != 0) {
            Log.i(TAG,"error code :" + result + " method:" + method + ", 错误码文档:http://yuyin.baidu.com/docs/tts/122 ");
        }
    }

    /**
     * 开始播放
     */
    public void speak(String text) {
        Log.i(TAG, "speak text:" + text);
        int result = synthesizer.speak(text);
        checkResult(result, "speak");
    }

    /**
     * 暂停播放。仅调用speak后生效
     */
    private void pause() {
        int result = synthesizer.pause();
        checkResult(result, "pause");
    }

    /**
     * 继续播放。仅调用speak后生效，调用pause生效
     */
    private void resume() {
        int result = synthesizer.resume();
        checkResult(result, "resume");
    }

    /*
     * 停止合成引擎。即停止播放，合成，清空内部合成队列。
     */
    private void stop() {
        int result = synthesizer.stop();
        checkResult(result, "stop");
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
                    speak("你好，我是小瑞，我能为您做些什么？");
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
            Log.i(TAG, "离线模式加载成功");
        }

        if(name.equals(SpeechConstant.CALLBACK_EVENT_ASR_READY)){
            // 引擎就绪，可以说话，一般在收到此事件后通过UI通知用户可以说话了
            Log.i(TAG, "开始识别");
        }else if(name.equals(SpeechConstant.CALLBACK_EVENT_ASR_BEGIN)){
            // 检测到说话开始
            Log.i(TAG, "开始说话");
        }else if(name.equals(SpeechConstant.CALLBACK_EVENT_ASR_END)){
            // 检测到说话结束
            Log.i(TAG, "结束说话");
        }else if(name.equals(SpeechConstant.CALLBACK_EVENT_ASR_FINISH)){
            // 识别结束
            Log.i(TAG, "结束识别");
            try {
                JSONObject json = new JSONObject(params);
                int errCode = json.getInt("error");
                if(errCode != 0) {
                    int subErrCode = json.getJSONObject("origin_result").getInt("sub_error");
                    String errorDesc = json.getJSONObject("origin_result").getString("desc");
                    Log.i(TAG, subErrCode + ":" + errorDesc);
                    switch (errCode){
                        case 1:
                        case 2:
                            speak("当前网络信号较弱，请检查网络连接");
                            break;
                        case 3:
                            switch (subErrCode){
                                case 3001:
                                case 3002:
                                case 3003:
                                case 3006:
                                    speak("打开录音机失败，请检查录音权限是否开启");
                                    break;
                                case 3101:
                                case 3102:
                                    speak("您没有说话");
                                    break;
                                default:
                                    break;
                            }
                            break;
                        case 4:
                            Log.i(TAG, "appid和appkey的鉴权失败");
                            break;
                        case 7:
                            speak("当前环境太嘈杂，请避开嘈杂的环境");
                            break;
                        case 9:
                            speak("打开录音机失败，请检查录音权限是否开启");
                            break;
                    }
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }else if (name.equals(SpeechConstant.CALLBACK_EVENT_ASR_PARTIAL)) {
            //识别结果
            if(params.contains("final_result")){
                try {
                    Log.i(TAG, "识别成功");
                    String words = "";
                    JSONObject json = new JSONObject(params);
                    JSONArray result = json.getJSONArray("results_recognition");
                    for(int i = 0; i < result.length(); i++){
                        words += result.getString(i);
                    }

                    processResult(words);

                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        } else if(name.equals(SpeechConstant.CALLBACK_EVENT_ASR_VOLUME)){
            try {
                JSONObject json = new JSONObject(params);
                int volume = json.getInt("volume");
                Toast.makeText(this, "当前音量："+ volume, Toast.LENGTH_SHORT).show();
            } catch (JSONException e) {
                e.printStackTrace();
            }
        } else if (data != null) {
            logTxt += " ;data length=" + data.length;
        }

        Log.i(TAG, logTxt);
    }

    private void processResult(String words) {
        text.setText(words);
        switch (words){
            case "小瑞":
                speak("我是小芮");
                break;
            case "你好":
                speak("你也好");
                break;
            default:
                break;
        }
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
