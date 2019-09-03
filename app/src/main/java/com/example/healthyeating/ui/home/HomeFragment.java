package com.example.healthyeating.ui.home;

import android.app.Activity;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProviders;

import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.RetryPolicy;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.example.healthyeating.MainActivity;
import com.example.healthyeating.MapsActivity;
import com.example.healthyeating.R;
import com.example.healthyeating.Restaurant;
import com.facebook.AccessToken;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.MapsInitializer;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;

import org.json.JSONException;
import org.json.JSONObject;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import static android.Manifest.permission.ACCESS_FINE_LOCATION;

public class HomeFragment extends Fragment implements OnMapReadyCallback {

    private HomeViewModel homeViewModel;

    private static final int PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION = 1;
    private static final int DEFAULT_ZOOM = 5;
    private static final String TAG = MapsActivity.class.getSimpleName();
    private static final String RESTAURANTS_URL = "https://healthyeatingapp.com/api/restaurants";
    private static final String api_key = "AIzaSyAThFfvYYZk14HJkVGoA1BWdNmUhWWg70U";

    private boolean mLocationPermissionGranted = false;
    private FusedLocationProviderClient mFusedLocationProviderClient;
    private Location mLastKnownLocation = new Location("test");
    private double latitude = 37.32;
    private double longitude = 122.03;
    private static final int MY_PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION = 0;
    private final LatLng mDefaultLocation = new LatLng(37.32, 122.03);
    private GoogleMap mMap;
    private MapView mMapView;
    private View mView;

    private LocationRequest locationRequest;
    private LocationCallback locationCallback;

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mFusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(getActivity());
    }

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        mView = inflater.inflate(R.layout.fragment_home, container, false);
        return mView;
    }

    private void getLocationPermission() {
        Log.e("before", "before");
        // Here, thisActivity is the current activity
        if (ContextCompat.checkSelfPermission(getActivity(),
                ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            Log.e("checking if granted", "here i am");
            // Permission is not granted
            // Should we show an explanation?
            if (ActivityCompat.shouldShowRequestPermissionRationale(getActivity(),
                    ACCESS_FINE_LOCATION)) {
                // Show an explanation to the user *asynchronously* -- don't block
                // this thread waiting for the user's response! After the user
                // sees the explanation, try again to request the permission.
            } else {
                // No explanation needed; request the permission
                ActivityCompat.requestPermissions(getActivity(),
                        new String[]{ACCESS_FINE_LOCATION},
                        MY_PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION);

                // MY_PERMISSIONS_REQUEST_READ_CONTACTS is an
                // app-defined int constant. The callback method gets the
                // result of the request.
            }
        } else {
            // Permission has already been granted
            mLocationPermissionGranted = true;
        }
    }

    public void onRequestPermissionsResult(int requestCode,
                                           String[] permissions, int[] grantResults) {
        switch (requestCode) {
            case MY_PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // permission was granted, yay! Do the
                    // contacts-related task you need to do.
                    mLocationPermissionGranted = true;
                } else {
                    // permission denied, boo! Disable the
                    // functionality that depends on this permission.
                    mLocationPermissionGranted = false;
                }
                return;
            }

            // other 'case' lines to check for other
            // permissions this app might request.
        }
    }

    private void putPins(double latitude, double longitude) {
        //send token to restaurants API
        RequestQueue requestQueue = Volley.newRequestQueue(getActivity());
        JSONObject data = new JSONObject();
        try {
            data.put("token", AccessToken.getCurrentAccessToken().getToken());
            data.put("latitude", latitude);
            data.put("longitude", longitude);
        } catch (JSONException e){
            e.printStackTrace();
        }
        JsonObjectRequest objectRequest = new JsonObjectRequest(
                Request.Method.POST,
                RESTAURANTS_URL,
                data,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        Log.e("response test", response.toString());
                        JsonObject jobj = new Gson().fromJson(response.toString(), JsonObject.class);
                        String restaurantListString = jobj.get("restaurant_list").toString();

                        //store restaurant data in instances of Restaurant.class
                        Gson gson = new Gson();
                        Type listType = new TypeToken<ArrayList<Restaurant>>(){}.getType();
                        List<Restaurant> restaurants = new Gson().fromJson(restaurantListString, listType);

                        //display restaurant pins using latitude and longitude from instance of restaurant class
                        for(int i = 0; i < restaurants.size(); i++) {
                            LatLng location = new LatLng(restaurants.get(i).getLatitude(), restaurants.get(i).getLongitude());

                            String menuItems = "Allowed Menu Items: ";
                            for(int j = 0; j < restaurants.get(i).getMenu_items().size() - 1; j++) {
                                menuItems += restaurants.get(i).getMenu_items().get(j).getItemName() + ", ";
                            }
                            menuItems += restaurants.get(i).getMenu_items().get(restaurants.get(i).getMenu_items().size() - 1).getItemName();
                            mMap.addMarker(new MarkerOptions().position(location).title(restaurants.get(i).getName()).snippet(menuItems));
                        }

                    }
                },
                new Response.ErrorListener() {

                    @Override
                    public void onErrorResponse(VolleyError error) {
                        Log.e("error", error.toString());
                    }

                }
        );
        requestQueue.add(objectRequest);

    }


    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mMapView = (MapView) mView.findViewById(R.id.map);
        if(mMapView != null) {
            mMapView.onCreate(null);
            mMapView.onResume();
            mMapView.getMapAsync(this);

        }
    }


    //todo:fix current activity(line 214)
    public void getDeviceLocation() {
        /*
         * Get the best and most recent location of the device, which may be null in rare
         * cases when a location is not available.
         */

        try {
            if (mLocationPermissionGranted) {
                System.out.println("permission granted");
                Task<Location> locationResult = mFusedLocationProviderClient.getLastLocation();
                locationResult.addOnCompleteListener(getActivity(), new OnCompleteListener<Location>() {
                    @Override
                    public void onComplete(@NonNull Task<Location> task) {
                        if (task.isSuccessful()) {
                            // Set the map's camera position to the current location of the device.
                            mLastKnownLocation = task.getResult();
                            System.out.println("last known location " + mLastKnownLocation);
                            double latitude = mLastKnownLocation.getLatitude();
                            double longitude = mLastKnownLocation.getLongitude();
                            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(
                                    new LatLng(latitude, longitude), DEFAULT_ZOOM));
                            mMap.setMyLocationEnabled(true);
                            putPins(latitude, longitude);
                        } else {
                            Log.d(TAG, "Current location is null. Using defaults.");
                            Log.e(TAG, "Exception: %s", task.getException());
                            mMap.moveCamera(CameraUpdateFactory
                                    .newLatLngZoom(mDefaultLocation, DEFAULT_ZOOM));
                            mMap.getUiSettings().setMyLocationButtonEnabled(false);
                        }
                    }
                });
            }
        } catch (SecurityException e)  {
            Log.e("Exception: %s", e.getMessage());
        }
    }


    @Override
    public void onMapReady(GoogleMap googleMap) {
        /*MapsInitializer.initialize(getContext());
        mMap = googleMap;
        googleMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);
        CameraPosition Liberty = CameraPosition.builder().target(new LatLng(40.689, -74.044)).zoom(16).bearing(0).tilt(45).build();
        googleMap.moveCamera(CameraUpdateFactory.newCameraPosition(Liberty));*/
        mMap = googleMap;
        getLocationPermission();
        getDeviceLocation();
    }
}