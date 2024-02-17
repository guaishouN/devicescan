package com.desay.devicescan;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Message;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;


import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.List;

import cn.bingoogolapple.qrcode.core.BGAQRCodeUtil;
import pub.devrel.easypermissions.AfterPermissionGranted;
import pub.devrel.easypermissions.EasyPermissions;


public class MainActivity extends AppCompatActivity implements EasyPermissions.PermissionCallbacks {
    public final static String TAG = MainActivity.class.getSimpleName();
    public final static String SP_NAME = "Lark";
    public final static String SP_APP_TOKEN = "app_token";
    public final static String SP_TABLE_ID = "table_id";
    private static final int REQUEST_CODE_QRCODE_PERMISSIONS = 1;
    private TextView Uid, DevId, Dev, Proj, time;
    private EditText User;
    private LarkRequestManager larkRequestManager;
    private String recordID, userName;
    private long timestamp;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        larkRequestManager = LarkRequestManager.getInstance();
        setSupportActionBar((Toolbar) findViewById(R.id.toolbar));
        BGAQRCodeUtil.setDebug(true);
        Uid = findViewById(R.id.editTextUId);
        DevId = findViewById(R.id.editTextDId);
        Dev = findViewById(R.id.editTextDev);
        Proj = findViewById(R.id.editTextProj);
        time = findViewById(R.id.editTextTime);
        User = findViewById(R.id.editTextUser);

        @SuppressLint("HandlerLeak")
        Handler handler = new Handler(){
            @SuppressLint("HandlerLeak")
            @Override
            public void handleMessage(@NonNull Message msg) {
                super.handleMessage(msg);
                switch (msg.what){
                    case LarkRequestManager.OK:
                        Toast toast=Toast.makeText(MainActivity.this, "设置成功", Toast.LENGTH_SHORT);
                        toast.show();
                        break;
                    case LarkRequestManager.ERROR:
                        Toast toast1=Toast.makeText(MainActivity.this, "设置失败", Toast.LENGTH_SHORT);
                        toast1.show();
                       break;
                }
            }
        };
        larkRequestManager.setAppTokenAndTableId(get(SP_APP_TOKEN,null),get(SP_TABLE_ID, null));;
        larkRequestManager.setHandler(handler);
        larkRequestManager.getTenantAccessToken(null);

    }

    @SuppressLint("DefaultLocale")
    public void onClick(View view){
        timestamp = System.currentTimeMillis();
        userName = User.getText().toString();
        if (view.getId() == R.id.scan_device_qrcode){
            startActivityForResult(new Intent(this, DeviceScanActivity.class),100);
        }
        String data = "";
        if (view.getId() == R.id.device_un){
            data = String.format(LarkRequestManager.PREPARE_DEV, userName, timestamp);
        }
        if (view.getId() == R.id.device_in){
            data = String.format(LarkRequestManager.ACTIVE_DEV, userName, timestamp);
        }
        if (view.getId() == R.id.device_delete){
            data = String.format(LarkRequestManager.DELTE_DEV, userName, timestamp);
        }
        Log.d(TAG, "onClick: data= "+data);
        if(!TextUtils.isEmpty(data)){
            larkRequestManager.updateRecord(data,recordID);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(resultCode == 100 && requestCode == 100){
            String resultString  = data.getStringExtra("scanResult");
            //String input = "DevId：SZ00000015\nDev：Google Pixel 5\nProj：12.3\" AVX, TAM\nUser：张勇\nDate：20240111";
            if(!TextUtils.isEmpty(resultString) && resultString.startsWith("https://yesv-desaysv.feishu.cn/base/")){
                try {
                    parseUrl(resultString);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }else if(!TextUtils.isEmpty(resultString)){
                String[] lines = resultString.split("\n");
                Map<String, String> map = new HashMap<>();
                for (String line : lines) {
                    String[] parts = line.split("：");
                    if (parts.length == 2) {
                        map.put(parts[0], parts[1]);
                    }
                }
                //{Uid=recu1vmufbQ8PR, User=张勇, Dev=Google Pixel 5, Proj=12.3" AVX, TAM, DevId=SZ00000015, Date=20240111}
                if(map.get("Uid")!=null){
                    Uid.setText(map.get("Uid"));
                    recordID = map.get("Uid");
                }
                if(map.get("DevId")!=null){
                    DevId.setText(map.get("DevId"));
                }
                if(map.get("Dev")!=null){
                    Dev.setText(map.get("Dev"));
                }
                if(map.get("Proj")!=null){
                    Proj.setText(map.get("Proj"));
                }
                if(map.get("Date")!=null){
                    time.setText(map.get("Date"));
                }
                if(map.get("User")!=null){
                    User.setText(map.get("User"));
                    userName = map.get("User");
                }
                Log.d(TAG, "onActivityResult: "+map);
            }
        }
    }


    @Override
    protected void onStart() {
        super.onStart();
        requestCodeQRCodePermissions();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        EasyPermissions.onRequestPermissionsResult(requestCode, permissions, grantResults, this);
    }

    @Override
    public void onPermissionsGranted(int requestCode, List<String> perms) {
    }

    @Override
    public void onPermissionsDenied(int requestCode, List<String> perms) {
    }

    @AfterPermissionGranted(REQUEST_CODE_QRCODE_PERMISSIONS)
    private void requestCodeQRCodePermissions() {
        String[] perms = {
                Manifest.permission.CAMERA,
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.INTERNET,
                Manifest.permission.ACCESS_WIFI_STATE,
                Manifest.permission.ACCESS_NETWORK_STATE,
        };
        if (!EasyPermissions.hasPermissions(this, perms)) {
            EasyPermissions.requestPermissions(this, "扫描二维码需要打开相机和散光灯的权限", REQUEST_CODE_QRCODE_PERMISSIONS, perms);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        larkRequestManager.handler.removeCallbacks(null);
        larkRequestManager.setHandler(null);
    }

    public void parseUrl(String url) throws Exception{
        //String url = "https://yesv-desaysv.feishu.cn/base/CmHmb4MxPaEW7zsWB07c1hCUnhd?table=tbl3OBzMMqjX79gN&view=vewsIt61jC";

        URL aURL = new URL(url);
        String query = aURL.getQuery();
        String[] params = query.split("&");

        Map<String, String> map = new HashMap<String, String>();
        for (String param : params) {
            String name = param.split("=")[0];
            String value = param.split("=")[1];
            map.put(name, value);
        }

        Log.d(TAG, "parseUrl: "+aURL.getPath());
        String[] splits = aURL.getPath().substring(1).split("/");
        String app_token = splits[1];
        String table_id = map.get("table");
        Log.d(TAG,"parseUrl app_token: " + app_token);
        Log.d(TAG,"parseUrl table_id: " + table_id);
        save(SP_APP_TOKEN, app_token);
        save(SP_TABLE_ID, table_id);
        String tip = String.format("设置多维表成功 app_token[%s] table_id[%s]",app_token,table_id);
        Toast toast1=Toast.makeText(MainActivity.this, tip, Toast.LENGTH_SHORT);
        toast1.show();
    }

    private void save(String key, String value) {
        SharedPreferences sp = getSharedPreferences(SP_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sp.edit();
        editor.putString(key, value);
        editor.apply();
    }

    private String get(String key, String defValue) {
        SharedPreferences sp = getSharedPreferences(SP_NAME, Context.MODE_PRIVATE);
        return sp.getString(key, defValue);
    }
}