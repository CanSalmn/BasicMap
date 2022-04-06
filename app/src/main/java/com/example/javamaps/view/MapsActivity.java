package com.example.javamaps.view;

import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.MainThread;
import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentActivity;
import androidx.room.Room;
import androidx.room.RoomDatabase;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import com.example.javamaps.R;
import com.example.javamaps.model.Place;
import com.example.javamaps.roomdb.PlaceDao;
import com.example.javamaps.roomdb.PlaceDatabase;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.example.javamaps.databinding.ActivityMapsBinding;
import com.google.android.material.snackbar.Snackbar;

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.core.Scheduler;
import io.reactivex.rxjava3.disposables.CompositeDisposable;
import io.reactivex.rxjava3.schedulers.Schedulers;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback,GoogleMap.OnMapLongClickListener {

    private GoogleMap mMap;
    private ActivityMapsBinding binding;
    ActivityResultLauncher<String > permissionLauncher;
    LocationManager locationManager;
    LocationListener locationListener;
    double validLatitude;
    double validLongitude;
    PlaceDatabase db ;
    PlaceDao placeDao;
    Place selectedPlace;

    private CompositeDisposable compositeDisposable= new CompositeDisposable();





    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityMapsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
        registerLauncher();

        db = Room.databaseBuilder(getApplicationContext(),PlaceDatabase.class,"Places" ).build();
        placeDao=db.placeDao();

        validLatitude= 0.0;
        validLongitude= 0.0;



    }


    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        mMap.setOnMapLongClickListener(this);

        Intent intent = getIntent();
        String infoFromIntent= intent.getStringExtra("info");


        if ( infoFromIntent.equals("new")){

            locationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);
            //asagidaki kod blogu konum degistiginde bize bildirir
            locationListener = new LocationListener() {
                @Override
                public void onLocationChanged(@NonNull Location location) {
                    //mMap kullanmak icin bir location gerekli ancak static bir location tanımlamak icin yukaridaki locationu
                    //kullanmaliyiz ama bu haliyle kullanilamaz dönüşüm yapilmasi gerekli
                    //LatLng userLocation = new LatLng(location.getLatitude(),location.getLongitude());
                    //mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(userLocation,15));
                }
            };

            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED ){

                if(ActivityCompat.shouldShowRequestPermissionRationale(this,Manifest.permission.ACCESS_FINE_LOCATION)){
                    Snackbar.make(binding.getRoot(),"Permission Needed",Snackbar.LENGTH_INDEFINITE ).setAction("Give Permission", new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            //request permission
                            permissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION);
                        }
                    }).show();

                }else{

                    //request permission
                    permissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION);
                }


            }else{

                //process
                locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER,0,0,locationListener);

                Location lastLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER );//son konumu alan kod

                if ( lastLocation   != null ){
                    LatLng lastUserLacition = new LatLng(lastLocation.getLatitude(),lastLocation.getLongitude());
                    mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(lastUserLacition,15   ));

                }
                mMap.setMyLocationEnabled(true);


            }



        }else{
             mMap.clear();
             selectedPlace = (Place) intent.getSerializableExtra("place");
             LatLng latLng = new LatLng(selectedPlace.latitude,selectedPlace.longitude);
             mMap.addMarker(new MarkerOptions().position(latLng ).title("Selected Place")   );
             mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng,15));
             binding.placeNamePlainText.setText(selectedPlace.name);
             binding.saveButton.setVisibility(View.GONE);
             binding.deleteButton.setVisibility(View.VISIBLE);




        }






/*
        //belirli bir konumu girmek icin yazılan kod blogu
        LatLng Maidens_Tower = new LatLng(41.0211,29.0041);//enlem ve boylamin girildigi alan
        mMap.addMarker(new MarkerOptions().position(Maidens_Tower).title("Maidens_Tower"));//marker
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(Maidens_Tower,20));//konuma zoom yapilmasi icin kod
*/
    }


    private void registerLauncher (){
        permissionLauncher = registerForActivityResult(new ActivityResultContracts.RequestPermission(), new ActivityResultCallback<Boolean>() {
            @Override
            public void onActivityResult(Boolean result) {
                if ( result){
                    //permission granted
                         if (ContextCompat.checkSelfPermission(MapsActivity.this,Manifest.permission.ACCESS_FINE_LOCATION)==PackageManager.PERMISSION_GRANTED){
                        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER,0,0,locationListener);

                    }





                }else{
                    //permission denied
                    Toast.makeText(MapsActivity.this, "Permission Needed", Toast.LENGTH_SHORT).show();

                }


            }
        });



    }


    @Override
        public void onMapLongClick(@NonNull LatLng latLng) {
        mMap.clear();//yapilan markerlar cok görünmemesi icin yazilan kodcuk.
        mMap.addMarker(new MarkerOptions().position(latLng));
        validLongitude= latLng.longitude;
        validLatitude= latLng.latitude;

        binding.saveButton.setEnabled(true);
        binding.deleteButton.setEnabled(true);
    }


    public  void save(View view ){
        Place place = new Place(binding.placeNamePlainText.getText().toString(),validLatitude,validLongitude);


        //placeDao.insert(place).subscribeOn(Schedulers.io()).subscribe();
        //alttaki kod blogu data basede katmaninda calismasini saglar yani on arayuzde degilde uygulamanin arkasinda calisir
        compositeDisposable.add(placeDao.insert(place)
            .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(MapsActivity.this::handleResponse)
        );
    }

    private void handleResponse(){
        Intent intent = new Intent(MapsActivity.this,MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(intent);

    }

    public  void delete(View view ){
                if ( selectedPlace != null){
                    compositeDisposable.add(placeDao.delete(selectedPlace)
                            .subscribeOn(Schedulers.io())
                            .observeOn(AndroidSchedulers.mainThread())
                            .subscribe(MapsActivity.this::handleResponse )
                    );



                }

    }

    @Override
    protected void onDestroy() {

        compositeDisposable.clear();

        super.onDestroy();
    }
}