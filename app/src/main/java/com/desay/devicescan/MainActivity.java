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
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.RecyclerView;

import com.google.gson.Gson;

import java.net.URL;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.List;

import cn.bingoogolapple.qrcode.core.BGAQRCodeUtil;
import pub.devrel.easypermissions.AfterPermissionGranted;
import pub.devrel.easypermissions.EasyPermissions;


public class MainActivity extends AppCompatActivity implements EasyPermissions.PermissionCallbacks {
    public final static String TAG = MainActivity.class.getSimpleName();
    public final static String SP_NAME = "Lark";
    public final static String SP_SETTINGS_STR = "Settings";
    public final static String SP_APP_TOKEN = "app_token";
    public final static String SP_TABLE_ID = "table_id";
    private static final int REQUEST_CODE_QRCODE_PERMISSIONS = 1;
    private LarkRequestManager larkRequestManager;
    private String recordID, userName;
    private long timestamp;
    private final Gson gson = new Gson();
    private SettingsBean settingsBean = null;
    private RecyclerView recyclerView;
    private TextView rawQrDataView, infoShowTx;
    private final QrCodeDataAdapter adapter = new QrCodeDataAdapter();
    @SuppressLint("HandlerLeak")
    private final Handler handler = new Handler() {
        @Override
        public void handleMessage(@NonNull Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {
                case LarkRequestManager.OK:
                    Toast toast = Toast.makeText(MainActivity.this, "设置成功", Toast.LENGTH_SHORT);
                    toast.show();
                    break;
                case LarkRequestManager.ERROR:
                    Toast toast1 = Toast.makeText(MainActivity.this, "设置失败", Toast.LENGTH_SHORT);
                    toast1.show();
                    break;
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        larkRequestManager = LarkRequestManager.getInstance();
        setSupportActionBar((Toolbar) findViewById(R.id.toolbar));
        String settingsStr = get(SP_SETTINGS_STR, null);
        if (!TextUtils.isEmpty(settingsStr)) {
            Log.d(TAG, "onCreate: to parser settings");
            parseQrSettingData(settingsStr);
        }
        larkRequestManager.setAppTokenAndTableId(get(SP_APP_TOKEN, null), get(SP_TABLE_ID, null));
        ;
        larkRequestManager.setHandler(handler);
        larkRequestManager.getTenantAccessToken(null);
        BGAQRCodeUtil.setDebug(true);
        recyclerView = findViewById(R.id.qr_container);
        rawQrDataView = findViewById(R.id.raw_data);
        infoShowTx = findViewById(R.id.app_tip);
        recyclerView.setAdapter(adapter);
        larkRequestManager.setInfoShow(new LarkRequestManager.InfoShowListener(){
            @Override
            public void infoShow(String msg, int type) {
                runOnUiThread(()->infoShow(msg,type));
            }
        });
    }

    @SuppressLint("DefaultLocale")
    public void onClick(View view) {
        timestamp = System.currentTimeMillis();
        //userName = User.getText().toString();
        if (view.getId() == R.id.scan_device_qrcode) {
            startActivityForResult(new Intent(this, DeviceScanActivity.class), 100);
        }
        String data = "";
        if (view.getId() == R.id.device_un) {
            data = String.format(LarkRequestManager.PREPARE_DEV, userName, timestamp);
        }
        if (view.getId() == R.id.device_in) {
            data = String.format(LarkRequestManager.ACTIVE_DEV, userName, timestamp);
        }
        if (view.getId() == R.id.device_delete) {
            data = String.format(LarkRequestManager.DELTE_DEV, userName, timestamp);
        }
        Log.d(TAG, "onClick: data= " + data);
        if (!TextUtils.isEmpty(data)) {
            larkRequestManager.updateRecord(data, recordID);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == 100 && requestCode == 100 && data != null) {
            String resultString = data.getStringExtra("scanResult");
            Log.d(TAG, "onActivityResult: " + resultString);
            try {
                rawQrDataView.setVisibility(View.GONE);
                recyclerView.setVisibility(View.VISIBLE);
                if (!TextUtils.isEmpty(resultString) && !parseQrSettingData(resultString)) {
                    if (resultString.contains("Uid") && resultString.contains(": ")) {
                        recyclerView.setVisibility(View.VISIBLE);
                        String[] lines = resultString.split("\n");
                        Log.d(TAG, "onActivityResult: " + Arrays.toString(lines));
                        Map<String, String> map = new HashMap<>();
                        for (String line : lines) {
                            String[] parts = line.split(": ");
                            if (parts.length == 2) {
                                map.put(parts[0], parts[1]);
                            }
                        }
                        //{Uid=recu1vmufbQ8PR, User=张勇, Dev=Google Pixel 5, Proj=12.3" AVX, TAM, DevId=SZ00000015, Date=20240111}
                        if (map.get("Uid") != null) {
                            recordID = map.get("Uid");
                            if (recordID != null) {
                                recordID = recordID.trim();
                            }
                        }
                        if (map.get("使用人") != null) {
                            userName = map.get("User");
                        }

                        LinkedList<QrCodeItem> items = new LinkedList<>();
                        for (String key : map.keySet()) {
                            if ("Uid".equals(key)) {
                                continue;
                            }
                            QrCodeItem item = new QrCodeItem();
                            item.key = key;
                            item.value = map.get(key);
                            if (settingsBean.editable.contains(key)) {
                                item.isEditable = true;
                            }
                            items.add(item);
                        }
                        adapter.setData(items);
                        Log.d(TAG, "onActivityResult: " + map);
                    } else {
                        recyclerView.setVisibility(View.GONE);
                        rawQrDataView.setVisibility(View.VISIBLE);
                        if (!TextUtils.isEmpty(resultString)) {
                            rawQrDataView.setText(resultString);
                        }
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public void infoShow(String msg, int type){
        infoShowTx.setText(msg);
        int cl = R.color.infoColorNormal;
        if(type>0){
            cl = type==1?R.color.infoColorWarn:R.color.infoColorDanger;
        }
        infoShowTx.setBackgroundResource(cl);
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

    public boolean parseQrSettingData(String resultString) {
        //String url = "https://yesv-desaysv.feishu.cn/base/CmHmb4MxPaEW7zsWB07c1hCUnhd?table=tbl3OBzMMqjX79gN&view=vewsIt61jC";
        if (TextUtils.isEmpty(resultString)) {
            Log.d(TAG, "parseQrSettingData: error empty resultString");
            return false;
        }
        try {
            settingsBean = gson.fromJson(resultString, SettingsBean.class);
            Log.d(TAG, "parseQrSettingData: " + settingsBean);
            URL aURL = new URL(settingsBean.url);
            String query = aURL.getQuery();
            String[] params = query.split("&");

            Map<String, String> map = new HashMap<String, String>();
            for (String param : params) {
                String name = param.split("=")[0];
                String value = param.split("=")[1];
                map.put(name, value);
            }

            Log.d(TAG, "parseUrl: " + aURL.getPath());
            String[] splits = aURL.getPath().substring(1).split("/");
            String app_token = splits[1];
            String table_id = map.get("table");
            Log.d(TAG, "parseUrl app_token: " + app_token);
            Log.d(TAG, "parseUrl table_id: " + table_id);
            save(SP_APP_TOKEN, app_token);
            save(SP_TABLE_ID, table_id);
            String tip = String.format("设置多维表成功 app_token[%s] table_id[%s]", app_token, table_id);
            Toast toast1 = Toast.makeText(MainActivity.this, tip, Toast.LENGTH_SHORT);
            toast1.show();
            save(SP_SETTINGS_STR, resultString);
        } catch (Exception e) {
            return false;
        }
        return true;
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