package com.tracy.gyrodemo;

import android.Manifest;
import android.app.Activity;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;


import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.DecimalFormat;

public class MainActivity extends Activity {
    private static final boolean isDebug = true;
    SensorManager sensorManager;
    Sensor gyroSensor, mPressure;

    // 将纳秒转化为秒
    private static final float NS2S = 1.0f / 1000000000.0f;
    private float timestamp;
    private float angle[] = new float[3];

    private TextView mAltitude, mPressureVal;

    private static final String TAG = MainActivity.class.getSimpleName();
    private double latitude = 0.0;
    private double longitude = 0.0;
    private double alititude = 0.0;
    private TextView info;
    private LocationManager locationManager;
    private TextView malititude2;
    private TextView manglex, mangley, manglez;

    private double mGPSAliti = 14.5d;
    private TextView mFloor;
    private double floorHeight = 4.1d;
    private double pressureValue = 1013.25d;
    private TextView city;
    private String cityName;

    private Handler weatherHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            if (msg.what == 1) {
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        getAddressFromBaidu(latitude, longitude);
                    }
                }).start();
            } else if (msg.what == 2) {
                final String city = (String) msg.obj;
                cityName = city;
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        getWeather1(city);
                    }
                }).start();

            } else if (msg.what == 3) {
                String pres = (String) msg.obj;
                pressureValue = Double.parseDouble(pres);
                city.setText("city：" + cityName + "pres：" + pres );

                Toast.makeText(MainActivity.this, "city：" + cityName + "pres：" + pres, Toast.LENGTH_LONG).show();
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        //陀螺仪
        init();
        //气压计
        initBarometer();
        //网络定位
        initLocation();


    }

    private void initLocation() {
        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            //申请权限

            if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                    Manifest.permission.ACCESS_COARSE_LOCATION)) {

            } else {

                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.ACCESS_COARSE_LOCATION},
                        1);

            }
        } else {
            //
        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            //申请权限

            if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                    Manifest.permission.ACCESS_FINE_LOCATION)) {

            } else {

                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                        2);

            }
        } else {
            //
        }
        if (locationManager.getProvider(LocationManager.NETWORK_PROVIDER) != null)
            locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, 0, locationListener);
        else Toast.makeText(this, "无法定位", Toast.LENGTH_SHORT).show();
    }

    LocationListener locationListener = new LocationListener() {
        @Override
        public void onLocationChanged(Location location) {
            if (isDebug)
                Log.i("", "-------lat-" + location.getLatitude() + "-long-" + location.getLongitude());

            latitude = location.getLatitude();
            longitude = location.getLongitude();
            Message msg = new Message();
            msg.what = 1;
            weatherHandler.sendMessage(msg);
            locationManager.removeUpdates(locationListener);


        }

        @Override
        public void onStatusChanged(String provider, int status, Bundle extras) {

        }

        @Override
        public void onProviderEnabled(String provider) {


        }

        @Override
        public void onProviderDisabled(String provider) {


        }
    };

    @Override
    protected void onResume() {
        super.onResume();
        //  initGRS();

    }

    private void initBarometer() {
        mAltitude = (TextView) findViewById(R.id.mAltitude);
        mPressureVal = (TextView) findViewById(R.id.mPressureVal);
        info = (TextView) findViewById(R.id.lanitude);
        malititude2 = (TextView) findViewById(R.id.alititude);
        manglex = (TextView) findViewById(R.id.anglex);
        mangley = (TextView) findViewById(R.id.angley);
        manglez = (TextView) findViewById(R.id.anglez);
        mFloor = (TextView) findViewById(R.id.floor);
        city = (TextView) findViewById(R.id.city);


        mPressure = sensorManager.getDefaultSensor(Sensor.TYPE_PRESSURE);
        if (mPressure == null) {
            mPressureVal.setText("您的手机不支持气压传感器(Your cell phone does not support the barometric pressure sensor)");
            return;
        }
        //mAccelerate = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);

        sensorManager.registerListener(pressureListener, mPressure,
                SensorManager.SENSOR_DELAY_NORMAL);

    }

    SensorEventListener pressureListener = new SensorEventListener() {

        @Override
        public void onSensorChanged(SensorEvent event) {
            // TODO Auto-generated method stub
            float sPV = event.values[0];
            mPressureVal.setText("气压值（Pressure value）：" + String.valueOf(sPV) + " smbar");
            DecimalFormat df = new DecimalFormat("0.00");
            df.getRoundingMode();
            // 计算海拔
            double height = 44330000 * (1 - (Math.pow((Double.parseDouble(df.format(sPV)) / pressureValue),
                    (float) 1.0 / 5255.0)));
            //Altitude  = (44330.0f*(1.0f - pow((float)Pressure/101325.0f, 0.190295f))) ;
            mAltitude.setText("通过气压计获取的海拔高度（Obtain the altitude by air pressure）：" + df.format(height) + " m");
            double hei = Double.parseDouble(df.format(height));
            long floorCount = Math.round((hei - mGPSAliti) / floorHeight) + 1;

            mFloor.setText("Your floor is " + floorCount + "th floor" + "\nMobile phone height：" + df.format((hei - mGPSAliti - (floorCount - 1) * floorHeight)) + "m");

        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {
            // TODO Auto-generated method stub

        }
    };

    private void getWeather1(String cityPinyin) {
        try {
            String url1 = "https://free-api.heweather.com/v5/now?city=" + cityPinyin + "&key=6573d44570a845dba08c353a82c72c67";
            URL url = new URL(url1);
            // 打开连接
            HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
            if (200 == urlConnection.getResponseCode()) {
                // 得到输入流
                InputStream is = urlConnection.getInputStream();
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                byte[] buffer = new byte[1024];
                int len = 0;
                while (-1 != (len = is.read(buffer))) {
                    baos.write(buffer, 0, len);
                    baos.flush();
                }
                String jsonData = baos.toString("utf-8");
                try {
                    JSONObject jsonObject = new JSONObject(jsonData);
                    JSONArray jsonArray = jsonObject.getJSONArray("HeWeather5");
                    JSONObject jsonObject2 = jsonArray.getJSONObject(0);
                    String pres = jsonObject2.getJSONObject("now").getString("pres");
                    Message msg = new Message();
                    msg.what = 3;
                    msg.obj = pres;
                    weatherHandler.sendMessageDelayed(msg, 3000);

                } catch (JSONException e) {
                    e.printStackTrace();
                }
                baos.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    private void init() {
        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        gyroSensor = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
        if (gyroSensor == null) {
            Toast.makeText(this, "您的设备不支持该功能！", Toast.LENGTH_LONG).show();
        } else {
            String str = "\n名字：" + gyroSensor.getName() + "\n电池："
                    + gyroSensor.getPower() + "\n类型：" + gyroSensor.getType()
                    + "\nVendor:" + gyroSensor.getVendor() + "\n版本："
                    + gyroSensor.getVersion() + "\n幅度："
                    + gyroSensor.getMaximumRange();
            // Toast.makeText(this, str, Toast.LENGTH_LONG).show();
        }

        /**
         * 注册监听器
         */
        sensorManager.registerListener(sensoreventlistener, gyroSensor,
                SensorManager.SENSOR_DELAY_NORMAL);

    }

    /**
     * 传感器的监听
     */
    private SensorEventListener sensoreventlistener = new SensorEventListener() {

        @Override
        public void onSensorChanged(SensorEvent event) {
            // TODO Auto-generated method stub

//            float[] valuse = event.values;
            //每一次旋转的角度
//            Log.i("", "沿X轴旋转的角度为：" + Float.toString(valuse[0]));
//            Log.i("", "沿Y轴旋转的角度为：" + Float.toString(valuse[1]));
//            Log.i("", "沿Z轴旋转的角度为：" + Float.toString(valuse[2]));
//            Toast.makeText(
//                    MainActivity.this,
//                    "anglex：" + Float.toString(valuse[0]) + "\nangley：" + Float.toString(valuse[1]) + "\nanglez："
//                            + Float.toString(valuse[2]), Toast.LENGTH_LONG).show();
//            if (true) {
//                return;
//            }

            if (timestamp != 0) {
                // 得到两次检测到手机旋转的时间差（纳秒），并将其转化为秒
                final float dT = (event.timestamp - timestamp) * NS2S;
                // 将手机在各个轴上的旋转角度相加，即可得到当前位置相对于初始位置的旋转弧度
                angle[0] += event.values[0] * dT;
                if (isDebug)
                    Log.i("", "--------------angle[0]--" + event.values[0] * dT + "");
                angle[1] += event.values[1] * dT;
                angle[2] += event.values[2] * dT;
                // 将弧度转化为角度
                final float anglex = (float) Math.toDegrees(angle[0]);
                final float angley = (float) Math.toDegrees(angle[1]);
                final float anglez = (float) Math.toDegrees(angle[2]);

//    Toast.makeText(
//                        MainActivity.this,
//                        "anglex：" + anglez + "\nangley：" + anglex + "\nanglez："
//                                + angley, Toast.LENGTH_LONG).show();
                manglex.setText("anglex：" + anglez);
                mangley.setText("angley：" + anglex);
                manglez.setText("anglez：" + angley);

            }
            // 将当前时间赋值给timestamp
            timestamp = event.timestamp;

        }


        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {
            // TODO Auto-generated method stub

        }
    };

    private void initGRS() {

        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            getLocation();
            //gps已打开
        } else {
            toggleGPS();
            new Handler() {
            }.postDelayed(new Runnable() {
                @Override
                public void run() {
                    getLocation();
                }
            }, 2000);

        }
    }

    private void toggleGPS() {
        Intent gpsIntent = new Intent();
        gpsIntent.setClassName("com.android.settings", "com.android.settings.widget.SettingsAppWidgetProvider");
        gpsIntent.addCategory("android.intent.category.ALTERNATIVE");
        gpsIntent.setData(Uri.parse("custom:3"));
        try {
            PendingIntent.getBroadcast(this, 0, gpsIntent, 0).send();
        } catch (PendingIntent.CanceledException e) {
            e.printStackTrace();

        }
    }

    private void getLocation() {

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            //申请权限

            if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                    Manifest.permission.ACCESS_COARSE_LOCATION)) {

            } else {

                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.ACCESS_COARSE_LOCATION},
                        1);

            }
        } else {
            //
        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            //申请权限

            if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                    Manifest.permission.ACCESS_FINE_LOCATION)) {

            } else {

                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                        2);

            }
        } else {
            //
        }
        Location location = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
        if (location != null) {
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000, 0, locationListenerGPS);
            latitude = location.getLatitude();
            longitude = location.getLongitude();
            alititude = location.getAltitude();
            info.setText("纬度latitude：" + latitude + "\n" + "经度longitude：" + longitude);
            malititude2.setText("alititude：" + alititude);
        } else {
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000, 0, locationListenerGPS);

        }
    }

    //权限请求回调
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case 1:
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                } else {

                }
                break;

            case 2:
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                } else {

                }
                break;
        }

    }

    LocationListener locationListenerGPS = new LocationListener() {
        // Provider的状态在可用、暂时不可用和无服务三个状态直接切换时触发此函数
        @Override
        public void onStatusChanged(String provider, int status, Bundle extras) {
        }

        // Provider被enable时触发此函数，比如GPS被打开
        @Override
        public void onProviderEnabled(String provider) {
            Log.e(TAG, provider);
        }

        // Provider被disable时触发此函数，比如GPS被关闭
        @Override
        public void onProviderDisabled(String provider) {
            Log.e(TAG, provider);
        }

        // 当坐标改变时触发此函数，如果Provider传进相同的坐标，它就不会被触发
        @Override
        public void onLocationChanged(Location location) {
            if (location != null) {
                Log.e("Map", "Location changed : Lat: " + location.getLatitude() + " Lng: " + location.getLongitude());
                latitude = location.getLatitude(); // 经度
                longitude = location.getLongitude(); // 纬度
                alititude = location.getAltitude();//海拔

                info.setText("纬度latitude：" + latitude + "\n" + "经度longitude：" + longitude);
                malititude2.setText("alititude：" + alititude);
            }
        }
    };


    private void getAddressFromBaidu(double latitude, double longitude) {
        String url1 = "http://api.map.baidu.com/geocoder/v2/?ak=xdm5TLml9xuzcmMXDXYVGWQgtXMGf1Na&location=" + latitude + "," + longitude + "&output=json&pois=0&mcode=07:DE:5B:5B:63:59:11:B3:59:F2:37:06:90:7B:CA:82:8A:E7:A4:EB;com.tracy.gyrodemo";
        if (isDebug)
            Log.i("wxy", "------------------uri-" + url1);

        try {
            URL url = new URL(url1);
            // 打开连接
            HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
            if (200 == urlConnection.getResponseCode()) {
                // 得到输入流
                InputStream is = urlConnection.getInputStream();
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                byte[] buffer = new byte[1024];
                int len = 0;
                while (-1 != (len = is.read(buffer))) {
                    baos.write(buffer, 0, len);
                    baos.flush();
                }
                String jsonData = baos.toString("utf-8");
                try {
                    JSONObject jsonObject = new JSONObject(jsonData);
                    if (isDebug)
                        Log.i("", "-----------jsondata--" + jsonData.toString());
                    int status = jsonObject.getInt("status");
                    if (status != 0) {
                        Log.i("", "-----------查询地址失败--");
                        return;
                    }

                    JSONObject jsonObject1 = jsonObject.getJSONObject("result");
                    JSONObject jsonObject2 = jsonObject1.getJSONObject("addressComponent");
                    String city = jsonObject2.getString("city");

                    Message msg = new Message();
                    msg.what = 2;
                    msg.obj = city;
                    weatherHandler.sendMessage(msg);
                    if (isDebug)
                        Log.i("", "-----------city--" + city);

                } catch (JSONException e) {
                    e.printStackTrace();
                }
                baos.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

    }


    @Override
    protected void onPause() {
        super.onPause();


//        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
//            //申请权限
//        }
//        try {
        if (locationManager != null)
            locationManager.removeUpdates(locationListener);
//        } catch (Exception e) {
//            e.printStackTrace();
//        }

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        sensorManager.unregisterListener(pressureListener);
        sensorManager.unregisterListener(sensoreventlistener);

    }


}

