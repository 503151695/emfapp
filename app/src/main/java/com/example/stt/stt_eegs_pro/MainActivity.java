package com.example.stt.stt_eegs_pro;

import android.Manifest;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;

import com.amap.api.maps2d.AMap;
import com.amap.api.maps2d.AMap.OnMapClickListener;
import com.amap.api.maps2d.CameraUpdate;
import com.amap.api.maps2d.CameraUpdateFactory;
import com.amap.api.maps2d.CoordinateConverter;
import com.amap.api.maps2d.MapView;
import com.amap.api.maps2d.model.BitmapDescriptorFactory;
import com.amap.api.maps2d.model.LatLng;
import com.amap.api.maps2d.model.Marker;
import com.amap.api.maps2d.model.MarkerOptions;


import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import dji.common.flightcontroller.DJIFlightControllerCurrentState;
import dji.common.util.DJICommonCallbacks;
import dji.sdk.flightcontroller.DJIFlightController;
import dji.common.flightcontroller.DJIFlightControllerDataType;
import dji.sdk.flightcontroller.DJIFlightControllerDelegate;
import dji.sdk.missionmanager.DJIHotPointMission;
import dji.sdk.missionmanager.DJIMission;
import dji.sdk.missionmanager.DJIMissionManager;
import dji.sdk.missionmanager.DJIWaypoint;
import dji.sdk.missionmanager.DJIWaypointMission;
import dji.sdk.products.DJIAircraft;
import dji.sdk.base.DJIBaseComponent;
import dji.sdk.base.DJIBaseProduct;
import dji.common.error.DJIError;
import dji.sdk.sdkmanager.DJISDKManager;
import dji.sdk.flightcontroller.DJIFlightControllerDelegate.FlightControllerReceivedDataFromExternalDeviceCallback;

public class MainActivity extends FragmentActivity implements View.OnClickListener, DJIMissionManager.MissionProgressStatusCallback, DJICommonCallbacks.DJICompletionCallback, OnMapClickListener,SeekBar.OnSeekBarChangeListener {

    protected static final String TAG = "MainActivity";

    private MapView mapView;
    private AMap aMap;

    private Button locate, add, clear;
    private Button config, prepare, start, stop;

    private Button prepare_fly,start_fly,locate_fly;//--mogu button
    private TextView mydatashow;
    private TextView mTvSpeed,mTvPoint,mTvInterval,mTvTime;    //--
    private SeekBar mSeekBarSpeed,mSeekBarPoint,mSeekBarInterval,mSeekBarTime;
    private EditText mEtHight,mEtLati,mEtLongi;

    private double mFlyHight = 2.0f;
    private double mFlyLati,mFlyLongi;
    private float mFlySpeed = 5.0f;
    private int mFlyPoint = 10;
    private float mFlyInterval = 5.0f;
    private int mFlyTime = 20;

    private boolean isAdd = false;

    private double droneLocationLat = 181, droneLocationLng = 181;
    private double gpsDroneLocationLat = 181,gpsDroneLocationLng = 181;
    private final Map<Integer, Marker> mMarkers = new ConcurrentHashMap<Integer, Marker>();
    private Marker droneMarker = null;

    private float altitude = 100.0f;
    private float mSpeed = 10.0f;

    private DJIWaypointMission mWaypointMission;
    private DJIMissionManager mMissionManager;
    private DJIFlightController mFlightController;

    private DJIHotPointMission mHotpointMission;
    //private DJIWaypoint.DJIWaypointAction mWaypointAction;

    private DJIWaypointMission.DJIWaypointMissionFinishedAction mFinishedAction = DJIWaypointMission.DJIWaypointMissionFinishedAction.NoAction;
    private DJIWaypointMission.DJIWaypointMissionHeadingMode mHeadingMode = DJIWaypointMission.DJIWaypointMissionHeadingMode.Auto;

    @Override
    protected void onResume(){
        super.onResume();
        initFlightController();
        initMissionManager();
    }

    @Override
    protected void onPause(){
        super.onPause();
    }

    @Override
    protected void onDestroy(){
        super.onDestroy();
        unregisterReceiver(mReceiver);
    }

    /**
     * @Description : RETURN Button RESPONSE FUNCTION
     */
    public void onReturn(View view){
        Log.d(TAG, "onReturn");
        this.finish();
    }

    private void setResultToToast(final String string){
        MainActivity.this.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(MainActivity.this, string, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void initUI() {

        locate = (Button) findViewById(R.id.locate);
        add = (Button) findViewById(R.id.add);
        clear = (Button) findViewById(R.id.clear);
        config = (Button) findViewById(R.id.config);
        prepare = (Button) findViewById(R.id.prepare);
        start = (Button) findViewById(R.id.start);
        stop = (Button) findViewById(R.id.stop);

        //--
        locate_fly = (Button) findViewById(R.id.locate_fly);
        prepare_fly = (Button) findViewById(R.id.prepare_fly);
        start_fly = (Button) findViewById(R.id.start_fly);

        mydatashow = (TextView) findViewById(R.id.datashow);

        mTvSpeed = (TextView) findViewById(R.id.tv_speed);
        mTvPoint = (TextView) findViewById(R.id.tv_point);
        mTvInterval = (TextView) findViewById(R.id.tv_interval);
        mTvTime = (TextView) findViewById(R.id.tv_time);

        mSeekBarSpeed = (SeekBar) findViewById(R.id.seekbar_speed);
        mSeekBarSpeed.setOnSeekBarChangeListener(this);
        mSeekBarSpeed = (SeekBar) findViewById(R.id.seekbar_point);
        mSeekBarSpeed.setOnSeekBarChangeListener(this);
        mSeekBarSpeed = (SeekBar) findViewById(R.id.seekbar_interval);
        mSeekBarSpeed.setOnSeekBarChangeListener(this);
        mSeekBarSpeed = (SeekBar) findViewById(R.id.seekbar_time);
        mSeekBarSpeed.setOnSeekBarChangeListener(this);

        locate_fly.setOnClickListener(this);
        start_fly.setOnClickListener(this);
        prepare_fly.setOnClickListener(this);

        mEtLati = (EditText) findViewById(R.id.et_lati);
        mEtLongi = (EditText) findViewById(R.id.et_longi);
        mEtHight = (EditText) findViewById(R.id.et_hight);

        //--

        locate.setOnClickListener(this);
        add.setOnClickListener(this);
        clear.setOnClickListener(this);
        config.setOnClickListener(this);
        prepare.setOnClickListener(this);
        start.setOnClickListener(this);
        stop.setOnClickListener(this);

    }

    private void initMapView() {

        if (aMap == null) {
            aMap = mapView.getMap();
            aMap.setOnMapClickListener(this);// add the listener for click for amap object
        }

        LatLng shenzhen = new LatLng(22.5362, 113.9454);
        aMap.addMarker(new MarkerOptions().position(shenzhen).title("Marker in Shenzhen"));
        aMap.moveCamera(CameraUpdateFactory.newLatLng(shenzhen));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // When the compile and target version is higher than 22, please request the
        // following permissions at runtime to ensure the
        // SDK work well.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.VIBRATE,
                            Manifest.permission.INTERNET, Manifest.permission.ACCESS_WIFI_STATE,
                            Manifest.permission.WAKE_LOCK, Manifest.permission.ACCESS_COARSE_LOCATION,
                            Manifest.permission.ACCESS_NETWORK_STATE, Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.CHANGE_WIFI_STATE, Manifest.permission.MOUNT_UNMOUNT_FILESYSTEMS,
                            Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.SYSTEM_ALERT_WINDOW,
                            Manifest.permission.READ_PHONE_STATE,
                    }
                    , 1);
        }

        setContentView(R.layout.activity_main);

        IntentFilter filter = new IntentFilter();
        filter.addAction(DJIDemoApplication.FLAG_CONNECTION_CHANGE);
        registerReceiver(mReceiver, filter);

        mapView = (MapView) findViewById(R.id.map);
        mapView.onCreate(savedInstanceState);
/*
        DJIAircraft mAircraft = (DJIAircraft)DJISDKManager.getInstance().getDJIProduct();
        DJIFlightController mFlightController = mAircraft.getFlightController();
        mFlightController.setReceiveExternalDeviceDataCallback(new FlightControllerReceivedDataFromExternalDeviceCallback() {
            @Override
            public void onResult(byte[] data) {
                String str = new String(data);
                Toast.makeText(MainActivity.this, str, Toast.LENGTH_SHORT).show();
            }
        });
*/
        initMapView();
        initUI();

    }

    protected BroadcastReceiver mReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            onProductConnectionChange();
        }
    };

    private void onProductConnectionChange()
    {
        initMissionManager();
        initFlightController();
        //--
        initDataTransmission();
    }

    private void initMissionManager() {
        DJIBaseProduct product = DJIDemoApplication.getProductInstance();

        if (product == null || !product.isConnected()) {
            setResultToToast("Disconnected");
            mMissionManager = null;
            return;
        } else {

            setResultToToast("Product connected");
            mMissionManager = product.getMissionManager();
            mMissionManager.setMissionProgressStatusCallback(this);
            mMissionManager.setMissionExecutionFinishedCallback(this);
        }

        mWaypointMission = new DJIWaypointMission();
    }

    private void initFlightController() {

        DJIBaseProduct product = DJIDemoApplication.getProductInstance();
        if (product != null && product.isConnected()) {
            if (product instanceof DJIAircraft) {
                mFlightController = ((DJIAircraft) product).getFlightController();
            }
        }

        if (mFlightController != null) {
            mFlightController.setUpdateSystemStateCallback(new DJIFlightControllerDelegate.FlightControllerUpdateSystemStateCallback() {

                @Override
                public void onResult(DJIFlightControllerCurrentState state) {
                    gpsDroneLocationLat = state.getAircraftLocation().getLatitude();
                    gpsDroneLocationLng = state.getAircraftLocation().getLongitude();
                    LatLng pos = new LatLng(gpsDroneLocationLat, gpsDroneLocationLng);
                    //--GPS转换为高德坐标系
                    CoordinateConverter converter  = new CoordinateConverter();
                    // CoordType.GPS 待转换坐标类型
                    converter.from(CoordinateConverter.CoordType.GPS);
                    // sourceLatLng待转换坐标点 DPoint类型
                    converter.coord(pos);
                    // 执行转换操作
                    LatLng desLatLng = converter.convert();
                    droneLocationLat = desLatLng.latitude;
                    droneLocationLng = desLatLng.longitude;
                    //droneLocationLat = state.getAircraftLocation().getLatitude();
                    //droneLocationLng = state.getAircraftLocation().getLongitude();

                    updateDroneLocation();
                }
            });
        }
    }
    //--初始化数据透传
    private void initDataTransmission(){
        DJIBaseProduct product = DJIDemoApplication.getProductInstance();
        if (product != null && product.isConnected()) {
            if (product instanceof DJIAircraft) {
                mFlightController = ((DJIAircraft) product).getFlightController();
            }
        }

        setResultToToast("DataTransmission init");

        if (mFlightController != null) {
            mFlightController.setReceiveExternalDeviceDataCallback(new FlightControllerReceivedDataFromExternalDeviceCallback() {
                @Override
                public void onResult(byte[] data) {

                    //setResultToToast("DataTransmission ok");
                    String str = new String(data);
                    mydatashow.setText(str);
                    //Toast.makeText(MainActivity.this,str , Toast.LENGTH_SHORT).show();
                }
            });
        }


    }

    /**
     * DJIMissionManager Delegate Methods
     */
    @Override
    public void missionProgressStatus(DJIMission.DJIMissionProgressStatus progressStatus) {

    }

    /**
     * DJIMissionManager Delegate Methods
     */
    @Override
    public void onResult(DJIError error) {
        setResultToToast("Execution finished: " + (error == null ? "Success!" : error.getDescription()));
    }

    @Override
    public void onMapClick(LatLng point) {
        if (isAdd == true){
            markWaypoint(point);
            DJIWaypoint mWaypoint = new DJIWaypoint(point.latitude, point.longitude, altitude);
            //Add Waypoints to Waypoint arraylist;
            if (mWaypointMission != null) {
                mWaypointMission.addWaypoint(mWaypoint);
            }
        }else{
            setResultToToast("Cannot Add Waypoint");
        }
    }

    public static boolean checkGpsCoordination(double latitude, double longitude) {
        return (latitude > -90 && latitude < 90 && longitude > -180 && longitude < 180) && (latitude != 0f && longitude != 0f);
    }

    // Update the drone location based on states from MCU.
    private void updateDroneLocation(){

        LatLng pos = new LatLng(droneLocationLat, droneLocationLng);
        //Create MarkerOptions object
        final MarkerOptions markerOptions = new MarkerOptions();
        markerOptions.position(pos);
        markerOptions.icon(BitmapDescriptorFactory.fromResource(R.drawable.aircraft));

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (droneMarker != null) {
                    droneMarker.remove();
                }

                if (checkGpsCoordination(droneLocationLat, droneLocationLng)) {
                    droneMarker = aMap.addMarker(markerOptions);
                }
            }
        });
    }


    private void markWaypoint(LatLng point){
        //Create MarkerOptions object
        MarkerOptions markerOptions = new MarkerOptions();
        markerOptions.position(point);
        markerOptions.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE));
        Marker marker = aMap.addMarker(markerOptions);
        mMarkers.put(mMarkers.size(), marker);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.locate:{
                updateDroneLocation();
                cameraUpdate(); // Locate the drone's place
                break;
            }
            case R.id.add:{
                enableDisableAdd();
                break;
            }
            case R.id.clear:{
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        aMap.clear();
                    }

                });
                if (mWaypointMission != null){
                    mWaypointMission.removeAllWaypoints(); // Remove all the waypoints added to the task
                }
                break;
            }
            case R.id.config:{
                showSettingDialog();
                break;
            }
            case R.id.prepare:{
                prepareWayPointMission();
                break;
            }
            case R.id.start:{
                startWaypointMission();
                break;
            }
            case R.id.stop:{
                stopWaypointMission();
                break;
            }
            case R.id.prepare_fly:{
                prepareMyWayPointMission();
                break;
            }
            case R.id.start_fly:{
                prepareMyHotPointMission();
                break;
            }
            case R.id.locate_fly:{
                updateDroneLocation();
                cameraUpdate(); // Locate the drone's place
                showDroneLocation();//show the locate place into text
                break;
            }
            default:
                break;
        }
    }

    private void cameraUpdate(){
        LatLng pos = new LatLng(droneLocationLat, droneLocationLng);
        float zoomlevel = (float) 18.0;
        CameraUpdate cu = CameraUpdateFactory.newLatLngZoom(pos, zoomlevel);
        aMap.moveCamera(cu);

    }

    private void enableDisableAdd(){
        if (isAdd == false) {
            isAdd = true;
            add.setText("Exit");
        }else{
            isAdd = false;
            add.setText("Add");
        }
    }

    private void showSettingDialog(){
        LinearLayout wayPointSettings = (LinearLayout)getLayoutInflater().inflate(R.layout.dialog_waypointsetting, null);

        final TextView wpAltitude_TV = (TextView) wayPointSettings.findViewById(R.id.altitude);
        RadioGroup speed_RG = (RadioGroup) wayPointSettings.findViewById(R.id.speed);
        RadioGroup actionAfterFinished_RG = (RadioGroup) wayPointSettings.findViewById(R.id.actionAfterFinished);
        RadioGroup heading_RG = (RadioGroup) wayPointSettings.findViewById(R.id.heading);

        speed_RG.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener(){

            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                if (checkedId == R.id.lowSpeed){
                    mSpeed = 3.0f;
                } else if (checkedId == R.id.MidSpeed){
                    mSpeed = 5.0f;
                } else if (checkedId == R.id.HighSpeed){
                    mSpeed = 10.0f;
                }
            }

        });

        actionAfterFinished_RG.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {

            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                Log.d(TAG, "Select finish action");
                if (checkedId == R.id.finishNone){
                    mFinishedAction = DJIWaypointMission.DJIWaypointMissionFinishedAction.NoAction;
                } else if (checkedId == R.id.finishGoHome){
                    mFinishedAction = DJIWaypointMission.DJIWaypointMissionFinishedAction.GoHome;
                } else if (checkedId == R.id.finishAutoLanding){
                    mFinishedAction = DJIWaypointMission.DJIWaypointMissionFinishedAction.AutoLand;
                } else if (checkedId == R.id.finishToFirst){
                    mFinishedAction = DJIWaypointMission.DJIWaypointMissionFinishedAction.GoFirstWaypoint;
                }
            }
        });

        heading_RG.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {

            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                Log.d(TAG, "Select heading");

                if (checkedId == R.id.headingNext) {
                    mHeadingMode = DJIWaypointMission.DJIWaypointMissionHeadingMode.Auto;
                } else if (checkedId == R.id.headingInitDirec) {
                    mHeadingMode = DJIWaypointMission.DJIWaypointMissionHeadingMode.UsingInitialDirection;
                } else if (checkedId == R.id.headingRC) {
                    mHeadingMode = DJIWaypointMission.DJIWaypointMissionHeadingMode.ControlByRemoteController;
                } else if (checkedId == R.id.headingWP) {
                    mHeadingMode = DJIWaypointMission.DJIWaypointMissionHeadingMode.UsingWaypointHeading;
                }
            }
        });

        new AlertDialog.Builder(this)
                .setTitle("")
                .setView(wayPointSettings)
                .setPositiveButton("Finish",new DialogInterface.OnClickListener(){
                    public void onClick(DialogInterface dialog, int id) {

                        String altitudeString = wpAltitude_TV.getText().toString();
                        altitude = Integer.parseInt(nulltoIntegerDefalt(altitudeString));
                        Log.e(TAG,"altitude "+altitude);
                        Log.e(TAG,"speed "+mSpeed);
                        Log.e(TAG, "mFinishedAction "+mFinishedAction);
                        Log.e(TAG, "mHeadingMode "+mHeadingMode);
                        configWayPointMission();
                    }

                })
                .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        dialog.cancel();
                    }

                })
                .create()
                .show();
    }

    String nulltoIntegerDefalt(String value){
        if(!isIntValue(value)) value="0";
        return value;
    }

    boolean isIntValue(String val)
    {
        try {
            val=val.replace(" ","");
            Integer.parseInt(val);
        } catch (Exception e) {return false;}
        return true;
    }

    private void configWayPointMission(){

        if (mWaypointMission != null){
            mWaypointMission.finishedAction = mFinishedAction;
            mWaypointMission.headingMode = mHeadingMode;
            mWaypointMission.autoFlightSpeed = mSpeed;

            if (mWaypointMission.waypointsList.size() > 0){
                for (int i=0; i< mWaypointMission.waypointsList.size(); i++){
                    mWaypointMission.getWaypointAtIndex(i).altitude = altitude;
                }

                setResultToToast("Set Waypoint attitude successfully");

            }
        }
    }

    private void prepareWayPointMission(){

        if (mMissionManager != null && mWaypointMission != null) {

            DJIMission.DJIMissionProgressHandler progressHandler = new DJIMission.DJIMissionProgressHandler() {
                @Override
                public void onProgress(DJIMission.DJIProgressType type, float progress) {
                }
            };

            mMissionManager.prepareMission(mWaypointMission, progressHandler, new DJICommonCallbacks.DJICompletionCallback() {
                @Override
                public void onResult(DJIError error) {
                    setResultToToast(error == null ? "Mission Prepare Successfully" : error.getDescription());
                }
            });
        }

    }


    private void startWaypointMission(){

        if (mMissionManager != null) {

            mMissionManager.startMissionExecution(new DJICommonCallbacks.DJICompletionCallback() {
                @Override
                public void onResult(DJIError error) {
                    setResultToToast("Mission Start: " + (error == null ? "Successfully" : error.getDescription()));
                }
            });

        }
    }

    private void stopWaypointMission(){

        if (mMissionManager != null) {
            mMissionManager.stopMissionExecution(new DJICommonCallbacks.DJICompletionCallback() {

                @Override
                public void onResult(DJIError error) {
                    setResultToToast("Mission Stop: " + (error == null ? "Successfully" : error.getDescription()));
                }
            });

            if (mWaypointMission != null){
                mWaypointMission.removeAllWaypoints();
            }
        }
    }
    //-- Mobile send bytes to Onboard
    private void OnReceiver(){
        if (mFlightController != null) {
            String str = "Hello Onboard!This is Mobile!";
            byte[] mbyte = str.getBytes();
            mFlightController.sendDataToOnboardSDKDevice(mbyte,
                    new DJICommonCallbacks.DJICompletionCallback() {
                        @Override
                        public void onResult(DJIError pError) {
                            setResultToToast(pError == null ? "SendtoOSDK  Successfully" : pError.getDescription());

                        }
                    });
        }
    }
    private void showDroneLocation(){
        LatLng pos = new LatLng(droneLocationLat, droneLocationLng);
        String strlati = String.valueOf(pos.latitude);
        String strlangi = String.valueOf(pos.longitude);

        mEtLati.setText(strlati);
        mEtLongi.setText(strlangi);
    }

    private void prepareMyWayPointMission(){
        float hight;
        String strlati = mEtLati.getText().toString();
        String strlangi = mEtLongi.getText().toString();
        String strhight = mEtHight.getText().toString();
        LatLng pos = new LatLng(Double.valueOf(strlati),Double.valueOf(strlangi));

        if (mWaypointMission != null){
            mWaypointMission.finishedAction = DJIWaypointMission.DJIWaypointMissionFinishedAction.GoHome;
            mWaypointMission.headingMode = DJIWaypointMission.DJIWaypointMissionHeadingMode.Auto;
            mWaypointMission.autoFlightSpeed = mFlySpeed;
            mWaypointMission.flightPathMode = DJIWaypointMission.DJIWaypointMissionFlightPathMode.Normal;
            //mWaypoint.hasAction = true;
            setResultToToast("Set Waypoint");
            //mWaypointAction.mActionType = DJIWaypoint.DJIWaypointActionType.Stay;
            //mWaypointAction.mActionParam = mFlyTime;

        }

        for(int i=0;i<mFlyPoint;i++) {
            hight = Float.valueOf(strhight) + i * mFlyInterval;
            DJIWaypoint.DJIWaypointAction mWaypointAction = new DJIWaypoint.DJIWaypointAction(DJIWaypoint.DJIWaypointActionType.Stay,mFlyTime * 1000);
            DJIWaypoint mWaypoint = new DJIWaypoint(pos.latitude, pos.longitude, hight);
            mWaypoint.addAction(mWaypointAction);
            if (mWaypointMission != null) {
                setResultToToast(String.valueOf(hight));
                mWaypointMission.addWaypoint(mWaypoint);

            } else {
                setResultToToast("Cannot Add Waypoint");
            }
        }

    }
    private void prepareMyHotPointMission(){

        float hight;
        String strlati = mEtLati.getText().toString();
        String strlangi = mEtLongi.getText().toString();
        String strhight = mEtHight.getText().toString();
        LatLng pos = new LatLng(Double.valueOf(strlati),Double.valueOf(strlangi));

        DJIHotPointMission mHotpointMission = new DJIHotPointMission(pos.latitude, pos.longitude);

        mHotpointMission.startPoint = DJIHotPointMission.DJIHotPointStartPoint.Nearest;
        mHotpointMission.angularVelocity = 10;
        mHotpointMission.altitude = 10;
        mHotpointMission.radius = 10;

        setResultToToast("Add Hotpoint");

        if (mMissionManager != null && mHotpointMission != null) {

            DJIMission.DJIMissionProgressHandler progressHandler = new DJIMission.DJIMissionProgressHandler() {
                @Override
                public void onProgress(DJIMission.DJIProgressType type, float progress) {
                }
            };

            mMissionManager.prepareMission(mHotpointMission, progressHandler, new DJICommonCallbacks.DJICompletionCallback() {
                @Override
                public void onResult(DJIError error) {
                    setResultToToast(error == null ? "Mission Prepare Successfully" : error.getDescription());
                }
            });
        }

    }

    /*
    * SeekBar停止滚动的回调函数
    */
    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {

    }

    /*
     * SeekBar开始滚动的回调函数
     */
    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {

    }

    /*
     * SeekBar滚动时的回调函数
     */
    @Override
    public void onProgressChanged(SeekBar seekBar, int progress,
                                  boolean fromUser) {
        Log.d(TAG, "seekid:"+seekBar.getId()+", progess"+progress);
        switch(seekBar.getId()) {
            case R.id.seekbar_speed:{
                mFlySpeed = seekBar.getProgress();
                mTvSpeed.setText(getResources().getString(R.string.text_speed)+" : "+String.valueOf(seekBar.getProgress()));
                break;
            }
            case R.id.seekbar_point:{
                mFlyPoint = seekBar.getProgress();
                mTvPoint.setText(getResources().getString(R.string.text_point)+" : "+String.valueOf(seekBar.getProgress()));
                break;
            }
            case R.id.seekbar_interval:{
                mFlyInterval = seekBar.getProgress();
                mTvInterval.setText(getResources().getString(R.string.text_interval)+" : "+String.valueOf(seekBar.getProgress()));
                break;
            }
            case R.id.seekbar_time:{
                mFlyTime = seekBar.getProgress();
                mTvTime.setText(getResources().getString(R.string.text_time)+" : "+String.valueOf(seekBar.getProgress()));
                break;
            }

            default:
                break;
        }
    }


}
