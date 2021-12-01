package com.example.kakaomapapi;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;

import java.util.List;

public class GpsTracker implements LocationListener {

    /**
     * 현재 위치를 가져오는데 2가지 방식이 존재.
     *  -> 서로 다른 이점이 있으므로 두 공급자를 모두 사용하는 것을 추천
     *
     * 네트워크 위치 제공자  vs GPS 위치 제공자
     *
     *  1. '네트워크 제공자' 가 'GPS 제공자' 보다 위치 좌표를 제공하는데 비교적 빠름.
     *  2. GPS 제공자가 실내에 있을 때 매우 느릴 수 있고 모바일 배터리 소모.
     *  3. 네트워크 제공자는 기지국에 따라 다르며 가장 가까운 기지국 위치를 반환.
     *  4. GPS 위치 제공 업체는 현재 우리의 위치를 정확하게 제공해줌.
     *
     */


    /**
     * 위치를 확인하는 단계
     *
     *  1. 위치 업데이트 수신을 위해 Manifest 에 권한 제공
     *  2. 위치 서비스에 대한 참조로 LocationManager 인스턴스 생성
     *  3. LocationManager 에서 위치 요청
     *  4. 위치 변경시 LocationListener 에서 위치 업데이트 수신
     */

    private Context context;

    public GpsTracker(Context mContext) {
        this.context = mContext;
    }

    void setLocationManager(LocationManager locationManager) {
        locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(context,"위치 권한을 허용해주세요", Toast.LENGTH_SHORT).show();
            return;
        }
        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, this);
    }

    @Override
    public void onLocationChanged(@NonNull Location location) {

    }

    @Override
    public void onLocationChanged(@NonNull List<Location> locations) {

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

    // 마지막으로 확인된 위치
    // LocationManager.getLastKnownLocation()

    // 위치가 변경될 때마다 이벤트를 받는 Listener 등록
    // LocationManager.requestLocationUpdates()



}
