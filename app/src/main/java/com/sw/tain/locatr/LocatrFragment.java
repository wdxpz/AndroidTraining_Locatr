package com.sw.tain.locatr;

import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.location.Location;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.MarkerOptions;
import java.util.List;

/**
 * Created by home on 2016/12/16.
 */

public class LocatrFragment extends SupportMapFragment {

    private static String TAG = "LocatrFragment";

    private static final int REQUEST_ERROR = 0;
    private static GoogleApiClient mClient;
    private GoogleMap mMap;
    private Location mCurLocation;
    private GalleryItem mGalleryItem;


    public static LocatrFragment newInstance() {
        return new LocatrFragment();
    }


    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
        setHasOptionsMenu(true);
        mClient = new GoogleApiClient.Builder(getActivity())
                .addApi(LocationServices.API)
                .addConnectionCallbacks(new GoogleApiClient.ConnectionCallbacks() {
                    @Override
                    public void onConnected(@Nullable Bundle bundle) {
                        getActivity().invalidateOptionsMenu();
                    }

                    @Override
                    public void onConnectionSuspended(int i) {

                    }
                })
                .build();
        getMapAsync(new OnMapReadyCallback() {
            @Override
            public void onMapReady(GoogleMap googleMap) {
                mMap = googleMap;
                mMap.setOnCameraIdleListener(new GoogleMap.OnCameraIdleListener() {
                    @Override
                    public void onCameraIdle() {
                        Log.d(TAG, "Camera idle");
                        if(marker1!=null && marker2!=null){
                            mMap.clear();
                            mMap.addMarker(marker1);
                            mMap.addMarker(marker2);
                        }
                    }
                });
            }
        });

    }

    @Override
    public void onResume() {
        super.onResume();

        int errorCode = GooglePlayServicesUtil.isGooglePlayServicesAvailable(getActivity());
        if (errorCode != ConnectionResult.SUCCESS) {
            Dialog dialog = GooglePlayServicesUtil.getErrorDialog(errorCode, getActivity(), REQUEST_ERROR, new DialogInterface.OnCancelListener() {
                @Override
                public void onCancel(DialogInterface dialogInterface) {
                    getActivity().finish();
                }
            });
            dialog.show();
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        mClient.connect();
        getActivity().invalidateOptionsMenu();
    }

    @Override
    public void onStop() {
        super.onStop();
        mClient.disconnect();
    }


    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.menu_fragment_locatr, menu);
        menu.findItem(R.id.action_locate).setEnabled(mClient.isConnected());
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()){
            case R.id.action_locate:
                findImage();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    public void findImage() {

        LocationRequest locationRequest = new LocationRequest();
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        locationRequest.setNumUpdates(1);
        locationRequest.setInterval(0);

        if (ActivityCompat.checkSelfPermission(getActivity(), android.Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(getActivity(), android.Manifest.permission.ACCESS_COARSE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(getActivity(),
                    new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION, android.Manifest.permission.ACCESS_COARSE_LOCATION},
                    1);
            return;
        }
        LocationServices.FusedLocationApi
                .requestLocationUpdates(mClient, locationRequest, new LocationListener() {
                    @Override
                    public void onLocationChanged(Location location) {
                        Log.d(TAG, "get location: " + location);
                        mCurLocation = location;

                        new SearchImageTask().execute(location);
                    }
                });

    }

    private Bitmap mBitmap;
    private MarkerOptions marker1;
    private MarkerOptions marker2;
    private class SearchImageTask extends AsyncTask<Location, Integer, Void>{




        private ProgressDialog dialog;

        @Override
        protected Void doInBackground(Location... locations) {
            FlickerFetcher fetcher = new FlickerFetcher();

            publishProgress(0);
            List<GalleryItem> list = fetcher.searchItem(locations[0]);

            if(list==null || list.size()==0) {
                publishProgress(2);
                return null;
            }

            mGalleryItem = list.get(0);

            publishProgress(1);
            mBitmap = fetcher.getBitmap(mGalleryItem.getUrl());

            publishProgress(2);

            return null;
        }

        @Override
        protected void onProgressUpdate(Integer... values) {
            super.onProgressUpdate(values);



            if(values[0]==0){
                if(dialog!=null) dialog.cancel();
                dialog = new ProgressDialog(getActivity());
                dialog.setTitle("Locating Images ...");
                dialog.show();
            }
            if(values[0]==1){
                if(dialog!=null) dialog.cancel();
                dialog = new ProgressDialog(getActivity());
                dialog.setTitle("Retrieving Images ...");
                dialog.show();
            }
            if(values[0]==2){
                {
                    if(dialog!=null) dialog.cancel();
                }
            }
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
            UpdateUI();

        }
    }

    private void UpdateUI(){

        if(mCurLocation==null) return;
        LatLng cameraPosition = new LatLng(mCurLocation.getLatitude(), mCurLocation.getLongitude());
        CameraUpdate camera = CameraUpdateFactory.newLatLngZoom(cameraPosition, 18);
        mMap.animateCamera(camera);

        if(mGalleryItem==null) return;
        LatLng picPostition = new LatLng(mGalleryItem.getLatitude(), mGalleryItem.getLongtitude());
        LatLngBounds bounds = new LatLngBounds.Builder()
                .include(cameraPosition)
                .include(picPostition)
                .build();
        int margin = getResources().getDimensionPixelOffset(R.dimen.map_inset_margin);
        camera = CameraUpdateFactory.newLatLngBounds(bounds, margin);
        mMap.animateCamera(camera);

        marker1 = new MarkerOptions().position(cameraPosition);
        BitmapDescriptor bitmapDescriptor = BitmapDescriptorFactory.fromBitmap(mBitmap);
        marker2 = new MarkerOptions().icon(bitmapDescriptor).position(picPostition);

    }
}
