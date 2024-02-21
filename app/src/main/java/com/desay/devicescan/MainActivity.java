package com.desay.devicescan;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.RecyclerView;

import com.addisonelliott.segmentedbutton.SegmentedButtonGroup;
import com.google.gson.Gson;

import java.math.BigDecimal;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Locale;
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
    private final Gson gson = new Gson();
    private SettingsBean settingsBean = null;
    private RecyclerView recyclerView;
    private TextView rawQrDataView, infoShowTx;
    private String app_token = "";
    private final QrCodeDataAdapter adapter = new QrCodeDataAdapter();
    private final Handler handler = new Handler(Looper.getMainLooper());
    private SegmentedButtonGroup useStatOptions;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        larkRequestManager = LarkRequestManager.getInstance();
        setSupportActionBar((Toolbar) findViewById(R.id.toolbar));
        recyclerView = findViewById(R.id.qr_container);
        rawQrDataView = findViewById(R.id.raw_data);
        infoShowTx = findViewById(R.id.app_tip);
        useStatOptions = findViewById(R.id.state_segment_group);
        recyclerView.setAdapter(adapter);
        String settingsStr = get(SP_SETTINGS_STR, null);
        if (!TextUtils.isEmpty(settingsStr)) {
            Log.d(TAG, "onCreate: to parser settings");
            parseQrSettingData(settingsStr);
            larkRequestManager.getTenantAccessToken(null);
        }else{
            infoShow("请先扫码配置多维表格链接，二维码从设备管理标签生成器生成", 1);
        }
        larkRequestManager.setAppTokenAndTableId(get(SP_APP_TOKEN, null), get(SP_TABLE_ID, null));
        larkRequestManager.setHandler(handler);
        BGAQRCodeUtil.setDebug(true);
        larkRequestManager.setInfoShow(new LarkRequestManager.InfoShowListener() {
            @Override
            public void infoShow(String msg, int type) {
                runOnUiThread(() -> MainActivity.this.infoShow(msg, type));
            }

            @Override
            public void getNewestRecord(HashMap<String, Object> fields) {
                try{
                    runOnUiThread(() -> MainActivity.this.getNewestRecord(fields));
                }catch (Exception e){
                    e.printStackTrace();
                }

            }
        });
        if(TextUtils.isEmpty(app_token)){
            infoShow("还没绑定多维表格到手机端, 请扫描配置二维码！",2);
        }
    }

    @SuppressLint("DefaultLocale")
    public void onClick(View view) {
        long timestamp = System.currentTimeMillis();
        //userName = User.getText().toString();
        if (view.getId() == R.id.scan_device_qrcode) {
            startActivityForResult(new Intent(this, DeviceScanActivity.class), 100);
            return;
        }
        if(TextUtils.isEmpty(app_token)){
            infoShow("还没绑定多维表格到手机端, 请扫描配置二维码！",2);
            return;
        }

        if(view.getId() == R.id.get_newest_record){
            if(TextUtils.isEmpty(recordID)){
                infoShow("还没有识别到数据记录，请先对标签二维码扫码识别",2);
                return;
            }
            infoShow("正在获取对应多维表最新记录....",1);
            larkRequestManager.getDataByRecordId(recordID);
            return;
        }
        try{
            String data = "";
            LinkedList<QrCodeItem> dataList = adapter.getDataList();
            if (dataList==null || dataList.isEmpty()){
                infoShow("没有数据, 不处理", 1);
                return;
            }
            dataList.forEach(qrCodeItem -> {
                if("使用人".equals(qrCodeItem.key)){
                    userName = qrCodeItem.value;
                }
            });
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
                infoShow("正在发送数据到多维表....", 1);
                larkRequestManager.updateRecord(data, recordID);
            }
        }catch (Exception e){
            e.printStackTrace();
            infoShow("设置出现错误 "+e.getMessage(), 2);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == 100 && requestCode == 100 && data != null) {
            String resultString = data.getStringExtra("scanResult");
            Log.d(TAG, "onActivityResult: " + resultString);
            infoShow("完成扫码", 1);
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
                            userName = map.get("使用人");
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
                        infoShow("识别成功", 0);
                    } else {
                        recyclerView.setVisibility(View.GONE);
                        rawQrDataView.setVisibility(View.VISIBLE);
                        if (!TextUtils.isEmpty(resultString)) {
                            rawQrDataView.setText(resultString);
                            infoShow("识别成功", 1);
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
        int cl = R.drawable.info_bg_ok;
        if(type>0){
            cl = type==1?R.drawable.info_bg_warn :R.drawable.info_bg_error;
        }
        infoShowTx.setBackgroundResource(cl);
    }

    private void getNewestRecord(HashMap<String, Object> fields) {
        if (fields!=null && !fields.isEmpty() && settingsBean!=null
                && settingsBean.item!=null && !settingsBean.item.isEmpty()){
            LinkedList<QrCodeItem> items = new LinkedList<>();
            for (String key : settingsBean.item){
                Object object = fields.get(key);
                if (object!=null) {
                    QrCodeItem item = new QrCodeItem();
                    item.key = key;
                    if(object instanceof String){
                        item.value = (String)object;
                    }else {
                        item.value = object.toString();
                        if (item.value.contains("E12")&&item.value.contains("1.")){
                            BigDecimal bigDecimal = new BigDecimal(item.value);
                            long result = bigDecimal.longValue();
                            SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyyMMdd", Locale.CHINA);
                            item.value = simpleDateFormat.format(new Date(result));
                        }
                    }
                    if (settingsBean.editable.contains(key)) {
                        item.isEditable = true;
                    }
                    items.add(item);
                }
            }
            Object object = fields.get("使用状态");
            if (object instanceof String) {
                String useStat = (String)object;
                if("未分配".equals(useStat)){
                    useStatOptions.setPosition(0,true);
                } else if("在使用".equals(useStat)){
                    useStatOptions.setPosition(1,true);
                } else if("已报废".equals(useStat)){
                    useStatOptions.setPosition(2,true);
                }
            }

            adapter.setData(items);
            Log.d(TAG, "getNewestRecord: " + items);
            infoShow("获取最新数据成功", 0);
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
        infoShow("已授权", 0);
    }

    @Override
    public void onPermissionsDenied(int requestCode, List<String> perms) {
        infoShow("没有网络权限或相机散光灯的权限", 2);
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
            EasyPermissions.requestPermissions(this, "扫描二维码需要网络权限或打开相机和散光灯的权限", REQUEST_CODE_QRCODE_PERMISSIONS, perms);
            infoShow("没有网络权限或相机散光灯的权限", 2);
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

            Map<String, String> map = new HashMap<>();
            for (String param : params) {
                String name = param.split("=")[0];
                String value = param.split("=")[1];
                map.put(name, value);
            }

            Log.d(TAG, "parseUrl: " + aURL.getPath());
            String[] splits = aURL.getPath().substring(1).split("/");
            app_token = splits[1];
            String table_id = map.get("table");
            Log.d(TAG, "parseUrl app_token: " + app_token);
            Log.d(TAG, "parseUrl table_id: " + table_id);
            larkRequestManager.setAppTokenAndTableId(app_token, table_id);
            save(SP_APP_TOKEN, app_token);
            save(SP_TABLE_ID, table_id);
            infoShow("已更新扫码配置", 0);
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