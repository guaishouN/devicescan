package com.desay.devicescan;

import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.util.Log;
import com.google.gson.Gson;
import com.google.gson.annotations.SerializedName;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;

/**
 *
 * def TAG_access_token(app_id=None, app_secret=None, config_file=None):
 *     # 构建请求URL和请求头
 *     url = "https://open.feishu.cn/open-apis/auth/v3/tenant_access_token/internal"
 *     headers = {
 *         'Content-Type': 'application/json; charset=utf-8',
 *     }
 *
 *     # 构建请求体
 *     payload = {
 *         "app_id": app_id,
 *         "app_secret": app_secret
 *     }
 *
 *     # 发起请求
 *     response = requests.post(url, headers=headers, data=json.dumps(payload))
 *     response_json = response.json()
 *     return response_json.get('code'), response_json.get('tenant_access_token')
 *
 */
public class LarkRequestManager {
    public static final String TAG  = LarkRequestManager.class.getSimpleName();
    private final static int METHOD_GET = 0;
    private final static int METHOD_PUT = 1;
    private final static int METHOD_POST = 2;
    public static final Object TENANT_JOB = new Object();
    public static final Object UPDATE_JOB =  new Object();
    public static final String TERNANT_TOKEN_URL = "https://open.feishu.cn/open-apis/auth/v3/tenant_access_token/internal";
    public static final MediaType JSON = MediaType.parse("application/json;charset=utf-8");
    public static final String APP_ID = "cli_a514aea9fa79900b";
    public static final String APP_SECRET = "IsUeIxmzO5NtJiQA6B3MdfkHqIcmQqws";
    public static final String APP_TOKEN = "CmHmb4MxPaEW7zsWB07c1hCUnhd";
    private String appToken = APP_TOKEN;
    public static final String TABLE_ID = "tbl3OBzMMqjX79gN";
    private String tableId = TABLE_ID;
    //未分配 在使用 已报废
    public static final String PREPARE_DEV = "{\"fields\": {\"使用状态\": \"未分配\", \"使用人\":\"%s\", \"录入日期\":%d}}";
    public static final String ACTIVE_DEV = "{\"fields\": {\"使用状态\": \"在使用\", \"使用人\":\"%s\", \"录入日期\":%d}}";
    public static final String DELTE_DEV = "{\"fields\": {\"使用状态\": \"已报废\", \"使用人\":\"%s\", \"录入日期\":%d}}";
    private static final AtomicBoolean IS_BUSY = new AtomicBoolean(false);
    //public static final String UPDATE_RECORD_URL = "https://open.feishu.cn/open-apis/bitable/v1/apps/:app_token/tables/:table_id/records/:record_id";
    public static final String UPDATE_RECORD_URL_BASE = "https://open.feishu.cn/open-apis/bitable/v1/apps/%s/tables/%s/records/%s";
    public static final String GET_BY_RECORDID_URL_BASE = "https://open.feishu.cn/open-apis/bitable/v1/apps/%s/tables/%s/records/%s";
    public String tenantAccessToken = "";
    public Handler handler = new Handler(Looper.getMainLooper());
    public final OkHttpClient client = new OkHttpClient.Builder()
            .readTimeout(10, TimeUnit.SECONDS)
            .writeTimeout(10, TimeUnit.SECONDS)
            .connectTimeout(10, TimeUnit.SECONDS)
            .callTimeout(10, TimeUnit.SECONDS).build();
    private final static LarkRequestManager INS = new LarkRequestManager();
    private InfoShowListener infoShowListener= (msg, type) -> {
        Log.d(TAG, "null info show");
    };

    private LarkRequestManager(){}

    public static LarkRequestManager getInstance() {
        return INS;
    }

    public void setHandler(Handler handler) {
        this.handler = handler;
    }

    private Handler getHandler() {
        if(handler ==null)
            handler = new Handler(Looper.getMainLooper());
        return handler;
    }


    public void setAppTokenAndTableId(String appToken, String tableId){
        if(!TextUtils.isEmpty(appToken) && !TextUtils.isEmpty(tableId)){
            this.appToken = appToken;
            this.tableId = tableId;
        }
    }


    public void getTenantAccessToken(final TenantAccessListener listener){
        handler.removeCallbacksAndMessages(TENANT_JOB);
        Map<String, String> headers = new HashMap<>();
        String bodyData = "{\"app_id\": \""+APP_ID+"\",\"app_secret\": \""+APP_SECRET+"\"}";
        doRequest(TERNANT_TOKEN_URL, headers, bodyData, new LarkCallBack(){
            @Override
            public void onFailure(@NotNull Call call, @Nullable IOException e) {
                super.onFailure(call, e);
                Log.d(TAG, "getTenantAccessToken Fail!!");
                handler.postDelayed(()->getTenantAccessToken(null), TENANT_JOB,20*1000);
                if(listener!=null){
                    listener.onGot(false);
                }
                infoShowListener.infoShow("链接多维表格失败[网络请求失败]", 2);
            }

            @Override
            public void onResponse(@NotNull Call call, @Nullable Response response) throws IOException {
                super.onResponse(call, response);
                TenantAccessTokenBean tenantAccessTokenBean = new Gson().fromJson(this.jsonData, TenantAccessTokenBean.class);
                Log.d(TAG, "getTenantAccessToken 200: "+tenantAccessTokenBean);
                if (tenantAccessTokenBean!=null && tenantAccessTokenBean.code==0){
                    infoShowListener.infoShow("链接多维表成功", 0);
                    tenantAccessToken = tenantAccessTokenBean.tenant_access_token;
                    if(tenantAccessTokenBean.expire<1800){
                        handler.postDelayed(()->getTenantAccessToken(null), TENANT_JOB,60*1000);
                        if(listener!=null){
                            listener.onGot(true);
                        }
                    }else{
                        getHandler().postDelayed(()->getTenantAccessToken(null), TENANT_JOB,10*60*1000);
                    }
                }else{
                    Log.d(TAG, "onResponse failed by code: "+tenantAccessTokenBean);
                    infoShowListener.infoShow("链接多维表失败, 请确认网络为公司内网并且已给‘设备管理’机器人添加多维表格权限, " +
                            "code["+baseResponseBean.code+"]msg["+baseResponseBean.msg+"]", 2);
                }
            }
        }, METHOD_POST);
    }
    
    public void updateRecord(final String stateStr, final String recordId){
       handler.removeCallbacksAndMessages(TENANT_JOB);
       handler.removeCallbacksAndMessages(UPDATE_JOB);
       if(IS_BUSY.get()){
           getHandler().postDelayed(()->_updateRecord(stateStr, recordId), UPDATE_JOB,1000);
       }else {
           IS_BUSY.set(true);
           getHandler().postDelayed(()->IS_BUSY.set(false), UPDATE_JOB,1000);
           _updateRecord(stateStr, recordId);
       }
    }

    private void _updateRecord(final String stateStr,  final String recordId){
        if(TextUtils.isEmpty(tenantAccessToken)){
            infoShowListener.infoShow("多维表格未链接，正在尝试...", 2);
            Log.d(TAG, "_updateRecord: tenantAccessToken is empty!!");
            getTenantAccessToken(isGot -> {
                Log.d(TAG, "updateRecord onGot: [" + isGot + "]");
                if(isGot){
                    infoShowListener.infoShow("多维表格链接成功，正在尝试更新数据...", 1);
                    _updateRecord(stateStr, recordId);
                }
            });
            return;
        }
        Log.d(TAG, "_updateRecord: tenantAccessToken is Ok!");
        Map<String, String> headers = new HashMap<>();
        headers.put("Authorization", "Bearer "+tenantAccessToken);
        String UPDATE_RECORD_URL = String.format(UPDATE_RECORD_URL_BASE, appToken, tableId, recordId);
        doRequest(UPDATE_RECORD_URL, headers, stateStr, new LarkCallBack(){
            public void onFailure(@NotNull Call call, @Nullable IOException e) {
                super.onFailure(call, e);
                Log.d(TAG, "update Record failed: "+this.jsonData);
                infoShowListener.infoShow("设置数据失败", 2);
            }
            @Override
            public void onResponse(@NotNull Call call, @Nullable Response response) throws IOException {
                super.onResponse(call, response);
                Log.d(TAG, "update Record Sucessed: "+this.jsonData);
                if(baseResponseBean.code == 0){
                    infoShowListener.infoShow("设置数据成功", 0);
                }else{
                    infoShowListener.infoShow("设置数据失败 code["+baseResponseBean.code+"]msg["+baseResponseBean.msg+"]", 2);
                }
            }
        }, METHOD_PUT);
    }

    public void getDataByRecordId(final String recordId){
        if(TextUtils.isEmpty(tenantAccessToken)){
            Log.d(TAG, "getDataByRecordId: tenantAccessToken is empty!!");
            infoShowListener.infoShow("多维表格未链接，正在尝试...", 2);
            Log.d(TAG, "getDataByRecordId: tenantAccessToken is empty!!");
            getTenantAccessToken(isGot -> {
                Log.d(TAG, "getDataByRecordId onGot: [" + isGot + "]");
                if(isGot){
                    infoShowListener.infoShow("多维表格链接成功，正在尝试同步最新记录数据...", 1);
                    getDataByRecordId(recordId);
                }
            });
            return;
        }
        Log.d(TAG, "getDataByRecordId: tenantAccessToken is Ok!");
        Map<String, String> headers = new HashMap<>();
        headers.put("Authorization", "Bearer "+tenantAccessToken);
        String getDataUrl = String.format(GET_BY_RECORDID_URL_BASE, appToken, tableId, recordId);
        doRequest(getDataUrl, headers, "", new LarkCallBack(){
            public void onFailure(@NotNull Call call, @Nullable IOException e) {
                super.onFailure(call, e);
                Log.d(TAG, "getDataByRecordId Record failed: "+this.jsonData);
                infoShowListener.infoShow("获取最新数据记录失败", 2);
            }
            @Override
            public void onResponse(@NotNull Call call, @Nullable Response response) throws IOException {
                super.onResponse(call, response);
                Log.d(TAG, "getDataByRecordId Record Sucessed: "+this.jsonData);
                if(baseResponseBean.code == 0){
                    infoShowListener.infoShow("获取最新数据记录成功", 0);
                    RecordDataResponseBean recordDataResponseBean = new Gson().fromJson(this.jsonData, RecordDataResponseBean.class);
                    Log.d(TAG, "onResponse get newest data: "+recordDataResponseBean);
                    try {
                        infoShowListener.getNewestRecord(recordDataResponseBean.data.record.fields);
                    }catch (Exception e){
                        e.printStackTrace();
                    }

                }else{
                    infoShowListener.infoShow("获取最新数据记录失败 code["+baseResponseBean.code+"]msg["+baseResponseBean.msg+"]", 2);
                }
            }
        }, METHOD_GET);
    }

    public void doRequest(String url, final Map<String, String> headers, String bodyData, LarkCallBack callBack, int methodType) {
        Log.d(TAG, "doRequest() called with: url = [" + url + "], headers = [" + headers + "], bodyData = [" + bodyData + "], callBack = [" + callBack + "]");
        RequestBody body = RequestBody.create(bodyData, JSON);
        Request.Builder builder = new Request.Builder();
        try {
            for (Map.Entry<String, String> entry : headers.entrySet()) {
                builder.addHeader(entry.getKey(), entry.getValue());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        if(methodType == METHOD_PUT){
            builder.url(url).put(body);
        }else if (methodType == METHOD_POST){
            builder.url(url).post(body);
        }else {
            builder.url(url).get();
        }
        final Request request = builder.build();
        Call call = client.newCall(request);
        call.enqueue(callBack);
    }

    public void setInfoShow(InfoShowListener infoShowListener) {
        this.infoShowListener = infoShowListener;
    }

    private interface TenantAccessListener{
        void onGot(boolean isGot);
    }

    public interface InfoShowListener{
        void infoShow(String msg, int type);
        default void getNewestRecord(HashMap<String, Object> fields){};
    }

    public static class LarkCallBack implements Callback {
        public String jsonData = null;
        public BaseResponseBean baseResponseBean = null;
        @Override
        public void onFailure(@NotNull Call call, @Nullable IOException e) {
            Log.i(TAG, "onFailure  IOException: " + (e == null ? " null" : e.getMessage()));
        }

        @Override
        public void onResponse(@NotNull final Call call, @Nullable Response response) throws IOException {
            if (response == null || response.body() == null) {
                Log.i(TAG, "onResponse is null !!!");
                return;
            }
            ResponseBody rbody = response.body();
            String body = rbody==null?"null response.body()":rbody.string();
            Log.i(TAG, "\nonResponse\n header[" + response.headers() + "]\nbody["
                    + body + "]\n code[" + response.code() + "]");
            this.jsonData = body;
            baseResponseBean = new Gson().fromJson(this.jsonData, BaseResponseBean.class);
        }
    }

    /**
     {
     "code": 0,
     "expire": 7192,
     "msg": "ok",
     "tenant_access_token": "t-g1041fhz3Y324MUTD2X7TRQ5OC7SIZOWX6VY2VIF"
     }
     */
    public static class TenantAccessTokenBean {
        public int code = -1;
        public int expire = -1;
        public String msg = "";
        public String tenant_access_token="";

        @Override
        public String toString() {
            return "TenantAccessTokenBean{" +
                    "code=" + code +
                    ", expire=" + expire +
                    ", msg='" + msg + '\'' +
                    ", tenant_access_token='" + tenant_access_token + '\'' +
                    '}';
        }
    }

    public class BaseResponseBean{
        public int code;
        public String msg;

        @Override
        public String toString() {
            return "BaseResponseBean{" +
                    "code=" + code +
                    ", msg='" + msg + '\'' +
                    '}';
        }
    }

    /**
     {
     "code": 0,
     "data": {
     "record": {
     "fields": {
     "使用人": "邓桂辉",
     "使用状态": "在使用",
     "勾选打印标签": true,
     "录入日期": 1704902400000,
     "设备ID": "SZ00000019",
     "设备名称": "iPhone 13",
     "项目": "Carmax 24YM"
     },
     "id": "recu25skfs5Blp",
     "record_id": "recu25skfs5Blp"
     }
     },
     "msg": "success"
     }
    */
    public class RecordDataResponseBean {
        public int code;
        public Data data;
        public String msg;

        @Override
        public String toString() {
            return "RecordDataResponse{" +
                    "code=" + code +
                    ", data=" + data +
                    ", msg='" + msg + '\'' +
                    '}';
        }
    }

    public class Data {
        public Record record;

        @Override
        public String toString() {
            return "Data{" +
                    "record=" + record +
                    '}';
        }
    }

    public class Record {
        public HashMap<String, Object> fields;
        public String id;
        public String record_id;

        @Override
        public String toString() {
            return "Record{" +
                    "fields=" + fields +
                    ", id='" + id + '\'' +
                    ", record_id='" + record_id + '\'' +
                    '}';
        }
    }
}
