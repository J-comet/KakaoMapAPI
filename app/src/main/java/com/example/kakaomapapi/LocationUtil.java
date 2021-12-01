package com.example.kakaomapapi;

import android.app.Activity;
import android.content.Context;
import android.location.Address;
import android.location.Geocoder;
import android.util.Log;
import android.widget.Toast;

import java.io.IOException;
import java.util.List;

public class LocationUtil {

    // 주소 -> 위도/경도
    public static String changeForLatLng(Activity activity, String searchAddress){
        String resultLatLng = null;
        double lat = 0;
        double lng = 0;

        Geocoder geocoder = new Geocoder(activity);
        List<Address> addressList;

        try {
            addressList = geocoder.getFromLocationName(
                    searchAddress, // 지역 이름
                    5); // 읽을 개수
            Log.e("hs", "searchAddress" + addressList.toString());

            if (addressList == null || addressList.size() < 0) {
                Toast.makeText(activity, "찾을 수 없는 지역입니다.",Toast.LENGTH_SHORT).show();

            } else {
                lat = addressList.get(0).getLatitude();
                lng = addressList.get(0).getLongitude();
                resultLatLng = lat + "%" + lng;
            }

        } catch (IOException e) {
            e.printStackTrace();
        }

        return resultLatLng;
    }

    // 위도/경도 -> 주소로 변환
    public static String changeForAddress(Activity activity, double lat, double lng) {
        String address = null;
        Geocoder geocoder = new Geocoder(activity);

        List<Address> addressList;

        try {
            geocoder.getFromLocation(lat, lng, 1);
            addressList = geocoder.getFromLocation(lat, lng, 1);

            if (addressList == null || addressList.size() < 0) {
                Toast.makeText(activity, "주소를 발견 하지 못했습니다", Toast.LENGTH_SHORT).show();
                address = null;

            } else {
                address = addressList.get(0).getAddressLine(0);
            }

            Log.e("hs", "address.get(0) = " + address);

        } catch (IOException e) {
            e.printStackTrace();
            Log.e("hs", e.getMessage());
        }

        return address;
    }
}
