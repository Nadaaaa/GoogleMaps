package com.example.hp.googlemaps;

import android.*;
import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.AutoCompleteTextView;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesNotAvailableException;
import com.google.android.gms.common.GooglePlayServicesRepairableException;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.places.AutocompletePrediction;
import com.google.android.gms.location.places.GeoDataClient;
import com.google.android.gms.location.places.Place;
import com.google.android.gms.location.places.PlaceBuffer;
import com.google.android.gms.location.places.Places;
import com.google.android.gms.location.places.ui.PlacePicker;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class MapActivity extends AppCompatActivity implements OnMapReadyCallback, GoogleApiClient.OnConnectionFailedListener {

    //Constants
    private static final String TAG = "MapActivity";
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1234;
    private static final float DEFAULT_ZOOM = 15f;
    private static final int PLACE_PICKER_REQUEST = 1;
    private static final LatLngBounds LAT_LNG_BOUNDS = new LatLngBounds(
            new LatLng(-40, -168), new LatLng(71, 136));

    //Vars
    boolean isLocationPermissionGranted = false;
    GoogleMap googleMap;
    private FusedLocationProviderClient fusedLocationProviderClient;
    private GoogleApiClient googleApiClient;
    private PlaceAutocompleteAdapter placeAutocompleteAdapter;
    GeoDataClient geoDataClient;
    PlaceInfo placeInfo;
    Marker marker;

    //Widgets
    AutoCompleteTextView autoCompleteTextView_search;
    ImageView imageView_getDeviceLocation;
    ImageView imageView_getPlaceInfo;
    ImageView imageView_getNearbyPlaces;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map);

        getLocationPermissions();
        initVars();
    }

    public void initVars() {
        autoCompleteTextView_search = findViewById(R.id.editText_search);

        autoCompleteTextView_search.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_SEARCH
                        || actionId == EditorInfo.IME_ACTION_DONE
                        || event.getAction() == KeyEvent.ACTION_DOWN
                        || event.getAction() == KeyEvent.KEYCODE_ENTER) {
                    geoLocate();
                }
                return false;
            }
        });

        imageView_getDeviceLocation = findViewById(R.id.image_getDeviceLocation);
        onClickGetDeviceLocationImageView();

        googleApiClient = new GoogleApiClient.Builder(this).addApi(Places.GEO_DATA_API).addApi(Places.PLACE_DETECTION_API)
                .enableAutoManage(this, this).build();

        autoCompleteTextView_search.setOnItemClickListener(autocompleteClickListener);
        geoDataClient = Places.getGeoDataClient(this, null);

        placeAutocompleteAdapter = new PlaceAutocompleteAdapter(this, geoDataClient, LAT_LNG_BOUNDS, null);
        autoCompleteTextView_search.setAdapter(placeAutocompleteAdapter);

        imageView_getPlaceInfo = findViewById(R.id.image_getPlaceInfo);
        imageView_getPlaceInfo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    if (marker.isInfoWindowShown()) {
                        marker.hideInfoWindow();
                    } else {
                        marker.showInfoWindow();
                    }
                } catch (NullPointerException e) {
                    Log.e(TAG, "onClick: NullPointerException" + e.getMessage());
                }
            }
        });


        imageView_getNearbyPlaces = findViewById(R.id.image_getNearbyPlaces);
        imageView_getNearbyPlaces.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                PlacePicker.IntentBuilder intentBuilder = new PlacePicker.IntentBuilder();

                try {
                    startActivityForResult(intentBuilder.build(MapActivity.this), PLACE_PICKER_REQUEST);
                } catch (GooglePlayServicesNotAvailableException | GooglePlayServicesRepairableException e) {
                    Log.e(TAG, "onClick: GooglePlayServicesNotAvailableException | GooglePlayServicesRepairableException" + e.getMessage());
                }
            }
        });
    }

    public void getLocationPermissions() {
        Log.d(TAG, "getLocationPermissions: Called");
        String[] permissions = {Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION};

        if (ContextCompat.checkSelfPermission(MapActivity.this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(MapActivity.this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            isLocationPermissionGranted = true;
            initMap();
        } else {
            ActivityCompat.requestPermissions(MapActivity.this, permissions, LOCATION_PERMISSION_REQUEST_CODE);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        Log.d(TAG, "onRequestPermissionsResult: Called");
        isLocationPermissionGranted = false;

        switch (requestCode) {
            case LOCATION_PERMISSION_REQUEST_CODE:

                if (grantResults.length > 0) {

                    for (int grantResult : grantResults) {
                        if (grantResult != PackageManager.PERMISSION_GRANTED) {
                            isLocationPermissionGranted = false;
                            return;
                        }
                    }
                    isLocationPermissionGranted = true;
                    initMap();
                }
        }
    }

    public void initMap() {
        Log.d(TAG, "initMap: Initialize Map");
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        mapFragment.getMapAsync(MapActivity.this);
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        Toast.makeText(this, "Map is Ready", Toast.LENGTH_SHORT).show();
        this.googleMap = googleMap;

        if (isLocationPermissionGranted) {
            getDeviceLocation();
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) !=
                    PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission
                    (this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                return;
            }
            googleMap.setMyLocationEnabled(true);
            googleMap.getUiSettings().setMyLocationButtonEnabled(false);
        }
    }

    private void getDeviceLocation() {
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(MapActivity.this);

        try {
            if (isLocationPermissionGranted) {
                Task location = fusedLocationProviderClient.getLastLocation();
                location.addOnCompleteListener(new OnCompleteListener() {
                    @Override
                    public void onComplete(@NonNull Task task) {
                        if (task.isSuccessful()) {
                            Log.d(TAG, "onComplete: Found Location");
                            Location currentLocation = (Location) task.getResult();
                            moveCamera(new LatLng(currentLocation.getLatitude(), currentLocation.getLongitude()), DEFAULT_ZOOM, true);
                        } else {
                            Log.d(TAG, "onComplete: Current Location Is Null");
                            Toast.makeText(MapActivity.this, "Unable To Get Current Location", Toast.LENGTH_SHORT).show();
                        }
                    }
                });
            }
        } catch (SecurityException e) {
            Log.e(TAG, "getDeviceLocation: SecurityException" + e.getMessage());
        }
    }

    private void moveCamera(LatLng latLng, float zoom, boolean isMyLocation) {
        googleMap.clear();
        Log.d(TAG, "moveCamera: Moving The Camera to Lat: " + latLng.latitude + "   Lng: " + latLng.longitude);
        googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, zoom));

        if (!isMyLocation) {
            MarkerOptions markerOptions = new MarkerOptions().position(latLng);
            googleMap.addMarker(markerOptions);
        }
    }

    private void moveCamera(LatLng latLng, float zoom, PlaceInfo placeInfo) {
        Log.d(TAG, "moveCamera: Moving The Camera to Lat: " + latLng.latitude + "   Lng: " + latLng.longitude);
        googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, zoom));

        googleMap.clear();

        googleMap.setInfoWindowAdapter(new CustomInfoWindowAdapter(MapActivity.this));
        if (null != placeInfo) {
            try {
                String snippet = "Address: " + placeInfo.getAddress() + "\n" +
                        "Phone Number: " + placeInfo.getPhoneNumber() + "\n" +
                        "Website: " + placeInfo.getWebsiteUri() + "\n" +
                        "Place Rating: " + placeInfo.getRating() + "\n";

                MarkerOptions markerOptions = new MarkerOptions()
                        .position(latLng)
                        .title(placeInfo.getName())
                        .snippet(snippet);

                marker = googleMap.addMarker(markerOptions);


            } catch (NullPointerException e) {
                Log.e(TAG, "moveCamera: NullPointerException" + e.getMessage());
            }

        } else {
            googleMap.addMarker(new MarkerOptions()
                    .position(latLng));
        }
    }

    private void geoLocate() {
        String searchString = autoCompleteTextView_search.getText().toString();
        Geocoder geocoder = new Geocoder(MapActivity.this);
        List<Address> addressResultList = new ArrayList<>();
        try {
            addressResultList = geocoder.getFromLocationName(searchString, 1);
        } catch (IOException e) {
            Log.e(TAG, "geoLocate: IOException: " + e.getMessage());
        }
        if (addressResultList.size() > 0) {
            Address address = addressResultList.get(0);
            Log.d(TAG, "geoLocate: Location Found " + address.toString());
            moveCamera(new LatLng(address.getLatitude(), address.getLongitude()), DEFAULT_ZOOM, false);
            hideSoftKeyboard();
        }
    }

    private void hideSoftKeyboard() {
        InputMethodManager inputManager = (InputMethodManager) this.getSystemService(Context.INPUT_METHOD_SERVICE);
        inputManager.hideSoftInputFromWindow(autoCompleteTextView_search.getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);
    }

    private void onClickGetDeviceLocationImageView() {
        imageView_getDeviceLocation.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getDeviceLocation();
            }
        });
    }

    private AdapterView.OnItemClickListener autocompleteClickListener = new AdapterView.OnItemClickListener() {
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            hideSoftKeyboard();
            final AutocompletePrediction item = placeAutocompleteAdapter.getItem(position);

            final String placeId = item.getPlaceId();

            PendingResult<PlaceBuffer> placeResult = Places.GeoDataApi
                    .getPlaceById(googleApiClient, placeId);

            placeResult.setResultCallback(updatePlaceDetailsCallback);
        }

        protected void onActivityResult(int requestCode, int resultCode, Intent data) {
            if (requestCode == PLACE_PICKER_REQUEST) {
                if (resultCode == RESULT_OK) {
                    Place place = PlacePicker.getPlace(MapActivity.this, data);

                    PendingResult<PlaceBuffer> placeResult = Places.GeoDataApi
                            .getPlaceById(googleApiClient, place.getId());

                    placeResult.setResultCallback(updatePlaceDetailsCallback);
                }
            }
        }

        private ResultCallback<PlaceBuffer> updatePlaceDetailsCallback = new ResultCallback<PlaceBuffer>() {
            @Override
            public void onResult(@NonNull PlaceBuffer places) {
                if (!places.getStatus().isSuccess()) {
                    places.release();
                    return;
                }

                final Place place = places.get(0);
                placeInfo = new PlaceInfo();

                try {
                    placeInfo.setName(place.getName().toString());
                    placeInfo.setAddress(place.getAddress().toString());
                    placeInfo.setId(place.getId());
                    placeInfo.setLatLng(place.getLatLng());
                    placeInfo.setRating(place.getRating());
                    placeInfo.setPhoneNumber(place.getPhoneNumber().toString());
                    placeInfo.setWebsiteUri(place.getWebsiteUri());
                } catch (NullPointerException e) {
                    Log.e(TAG, "onResult: NullPointerException" + e.getMessage());
                }
                moveCamera(new LatLng(place.getViewport().getCenter().latitude, place.getViewport().getCenter().longitude)
                        , DEFAULT_ZOOM, placeInfo);
                places.release();
            }
        };
    };

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

    }


}
