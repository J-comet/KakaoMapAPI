package com.example.kakaomapapi;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.Signature;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import net.daum.mf.map.api.MapPOIItem;
import net.daum.mf.map.api.MapPoint;
import net.daum.mf.map.api.MapView;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    private String[] arrGpsPermissions = {Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION};

    private static final int CODE_GPS_PERMISSION_ALL_GRANTED = 300;  // 모두허용
    private static final int CODE_GPS_PERMISSION_FINE_DENIED = 200;  // 대략허용
    private static final int CODE_GPS_PERMISSION_DENIED_TRUE = 100;  // 허용 거부한적 있는 유저
    private static final int CODE_GPS_PERMISSION_FIRST = 1000;  // 처음 권한 요청하는 유저

    /**
     * 위치서비스 꺼져있을 때 요청할 StartActivityForResult
     */
    ActivityResultLauncher<Intent> gpsSettingRequest = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(),
            new ActivityResultCallback<ActivityResult>() {
                @Override
                public void onActivityResult(ActivityResult result) {
                    if (result.getResultCode() == Activity.RESULT_OK) {
                        Intent intent = result.getData();
                        Log.e("hs", "intent :" + intent.toString());
                    }
                }
            });

    // 권한 획득 launcher
    ActivityResultLauncher<String[]> gpsPermissionRequest =
            registerForActivityResult(new ActivityResultContracts
                            .RequestMultiplePermissions(), result -> {

                        Boolean fineLocationGranted = null;
                        Boolean coarseLocationGranted = null;

                        Log.e("result", "result :" + result.toString());

                        // API 24 이상에서 동작
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                            fineLocationGranted = result.getOrDefault(
                                    Manifest.permission.ACCESS_FINE_LOCATION, false);
                            coarseLocationGranted = result.getOrDefault(
                                    Manifest.permission.ACCESS_COARSE_LOCATION, false);

                        } else {
                            // API 24 이하에서 동작
                            fineLocationGranted = result.get(Manifest.permission.ACCESS_FINE_LOCATION);
                            coarseLocationGranted = result.get(Manifest.permission.ACCESS_COARSE_LOCATION);
                        }

                        /**
                         * Android 12 이상 위치권한 동작방식 변경
                         * 1) 정밀한 위치일 때 ACCESS_FINE_LOCATION , ACCESS_COARSE_LOCATION 두가지 권한 다 필요.
                         * 2) 대략적인 위치일 때 ACCESS_COARSE_LOCATION 권한만 필요.
                         */
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {

                            Log.e("fineLocation", "fineLocationGranted :" + fineLocationGranted);
                            Log.e("coarseLocation", "coarseLocationGranted :" + coarseLocationGranted);

                            if (fineLocationGranted != null && fineLocationGranted && coarseLocationGranted != null && coarseLocationGranted) {
                                // Precise location access granted.
                                // 정밀한 위치 사용에 대해서 위치권한 허용
                                Log.e("hs", "정밀한위치");

                            } else if (coarseLocationGranted != null && coarseLocationGranted && fineLocationGranted == false) {
                                // Only approximate location access granted.
                                // 대략적인 위치 사용에 대해서만 위치권한 허용
                                Log.e("hs", "대략적인위치");

                            } else {
                                // No location access granted.
                                // 권한획득 거부
                                Toast.makeText(this, "현재위치를 가져오려면 위치 권한은 필수 입니다", Toast.LENGTH_SHORT).show();
                                Log.e("hs", "위치권한거부");
                            }

                        } else {
                            Toast.makeText(this, "현재위치를 가져오려면 위치 권한은 필수 입니다", Toast.LENGTH_SHORT).show();
                            Log.e("hs", "위치권한거부");
                        }
                    }
            );


    private MapView mapView;
    private Button btnPermission;  // 권한 받을 버튼
    private TextView tvLocation;  // 위도 경도
    private TextView tvAddress;
    private EditText etSearchAddress;
    private Button btnSearch;

    private LocationManager locationManager;
    private LocationListener locationListener;

    private Location lastLocation;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        init();

        mapView = findViewById(R.id.map_view);

        // 중심점 이동
        mapView.setMapCenterPoint(MapPoint.mapPointWithGeoCoord(37.53737528, 127.00557633), true);

        // 줌 레벨 변경
        mapView.setZoomLevel(7, true);

        // 중심점 변경 + 줌 레벨 변경
//        mapView.setMapCenterPointAndZoomLevel(MapPoint.mapPointWithGeoCoord(33.41, 126.52), 9, true);

        // 줌 인
        mapView.zoomIn(true);

        // 줌 아웃
        mapView.zoomOut(true);

        /**
         * 기본 마커 작동 중
         */
        /*MapPOIItem marker = new MapPOIItem();
        marker.setItemName("Default Marker");
        marker.setTag(0);
        marker.setMapPoint(MapPoint.mapPointWithGeoCoord(37.549310, 126.939174));
        marker.setMarkerType(MapPOIItem.MarkerType.BluePin); // 기본으로 제공하는 BluePin 마커 모양.
        marker.setSelectedMarkerType(MapPOIItem.MarkerType.RedPin); // 마커를 클릭했을때, 기본으로 제공하는 RedPin 마커 모양.

        mapView.addPOIItem(marker);*/

        /**
         * 커스텀 마커 현재 미작동중
         */
//        MapPOIItem customMarker = new MapPOIItem();
//        customMarker.setItemName("Custom Marker");
//        customMarker.setTag(1);
//
//        customMarker.setMapPoint(MapPoint.mapPointWithGeoCoord(37.549310,126.939174));
//        customMarker.setMarkerType(MapPOIItem.MarkerType.CustomImage); // 마커타입을 커스텀 마커로 지정.
//        customMarker.setCustomImageResourceId(R.drawable.custom_marker_red); // 마커 이미지.
//        customMarker.setCustomImageAutoscale(true); // hdpi, xhdpi 등 안드로이드 플랫폼의 스케일을 사용할 경우 지도 라이브러리의 스케일 기능을 꺼줌.
//        customMarker.setCustomImageAnchor(0.5f, 1.0f); // 마커 이미지중 기준이 되는 위치(앵커포인트) 지정 - 마커 이미지 좌측 상단 기준 x(0.0f ~ 1.0f), y(0.0f ~ 1.0f) 값.
//
//        mapView.addPOIItem(customMarker);
    }

    private void init() {
        btnPermission = findViewById(R.id.btn_permission);
        btnPermission.setOnClickListener(this);
        btnSearch = findViewById(R.id.btn_search);
        btnSearch.setOnClickListener(this);
        tvLocation = findViewById(R.id.tv_location);
        tvAddress = findViewById(R.id.tv_address);
        etSearchAddress = findViewById(R.id.et_search_address);

        locationListener = new LocationListener() {
            @Override
            public void onLocationChanged(@NonNull Location location) {
                double lat = location.getLatitude();
                double lng = location.getLongitude();
                tvLocation.setText(lat + "," + lng);
                // 중심점 이동
                mapView.setMapCenterPoint(MapPoint.mapPointWithGeoCoord(lat, lng), true);

                tvAddress.setText(LocationUtil.changeForAddress(MainActivity.this, lat, lng));

                Log.e("hs", tvLocation.getText().toString());
            }

            @Override
            public void onFlushComplete(int requestCode) {

            }

            @Override
            public void onStatusChanged(String provider, int status, Bundle extras) {

            }

            @Override
            public void onProviderEnabled(@NonNull String provider) {

            }

            @Override
            public void onProviderDisabled(@NonNull String provider) {

            }
        };

        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            permissionResultAction(gpsPermissionCheck());
            Toast.makeText(this, "위치권한 허용 필요", Toast.LENGTH_SHORT).show();

            Log.e("hs", "권한 없음");
            return;
        }

        // 마지막 위치 값 가져오기
//        lastLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
//        lastLocation = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);

        if (checkLocationServicesStatus()){
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, locationListener);
            locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, 0, locationListener);
            Log.e("hs", "권한 OO");
        } else {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("위치서비스");
            builder.setMessage("위치서비스를 활성화 해주세요");
            builder.setCancelable(false);
            builder.setPositiveButton("확인", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    dialog.dismiss();
                    Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                    gpsSettingRequest.launch(intent);
                }
            });
            builder.setNegativeButton("취소", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    dialog.dismiss();
                    Toast.makeText(MainActivity.this, "위치서비스 비활성화 상태", Toast.LENGTH_SHORT).show();
                }
            });
            builder.show();

        }


    }

    /**
     * 권한 얻은 후 행동할 메서드
     */
    private void permissionResultAction(int permissionResult) {

        switch (permissionResult) {
            case CODE_GPS_PERMISSION_ALL_GRANTED:  // 모두허용
                Toast.makeText(MainActivity.this, "위치권한 모두허용", Toast.LENGTH_SHORT).show();
                break;

            case CODE_GPS_PERMISSION_FINE_DENIED:  // 대략허용
                Toast.makeText(MainActivity.this, "위치권한 대략허용", Toast.LENGTH_SHORT).show();
                break;

            case CODE_GPS_PERMISSION_DENIED_TRUE:  // 권한 거부한적 있는 유저

                AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                builder.setTitle("필수권한")
                        .setMessage("위치정보를 얻기 위해\n위치권한을 허용해주세요")
                        .setPositiveButton("확인", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.dismiss();
                                Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).setData(Uri.parse("package:" + getPackageName()));
                                startActivity(intent);
                            }
                        }).setNegativeButton("취소", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                        Toast.makeText(MainActivity.this, "위치권한 거부", Toast.LENGTH_SHORT).show();
                    }
                });

                builder.show();

                break;

            case CODE_GPS_PERMISSION_FIRST:  // 처음 실행
                gpsPermissionRequest.launch(arrGpsPermissions);
                break;
        }
    }

    /**
     * 사용자가 GPS 활성화 시켰는지 확인
     */
    public boolean checkLocationServicesStatus() {
        LocationManager locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);

        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
                || locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
    }


    private int gpsPermissionCheck() {

        int isPermission = -1;

        // 권한 허용한 사용자인지 체크
        if (ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                && ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {

            /**
             * 위치권한 모두 허용
             */
            isPermission = CODE_GPS_PERMISSION_ALL_GRANTED;

        } else if (ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
                && ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_DENIED) {

            /**
             * 위치권한 'ACCESS_COARSE_LOCATION' 만 허용
             */
            isPermission = CODE_GPS_PERMISSION_FINE_DENIED;

        } else {

            /**
             * 위치권한 거부
             * 1. 명시적으로 거부한 유저인지
             * 2. 권한을 요청한 적 없는 유저인지
             */

            // 사용자가 권한요청을 명시적으로 거부했던 적 있는 경우 true 를 반환
            if (ActivityCompat.shouldShowRequestPermissionRationale(MainActivity.this, Manifest.permission.ACCESS_FINE_LOCATION)
                    || ActivityCompat.shouldShowRequestPermissionRationale(MainActivity.this, Manifest.permission.ACCESS_COARSE_LOCATION)) {
                isPermission = CODE_GPS_PERMISSION_DENIED_TRUE;
            } else {
                isPermission = CODE_GPS_PERMISSION_FIRST;
            }
        }
        return isPermission;

    }


    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btn_permission:
                /**
                 * 권한요청 부분
                 */
                permissionResultAction(gpsPermissionCheck());
                break;
        }
    }

}