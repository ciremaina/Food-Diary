package com.randomappsinc.foodjournal.activities;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.widget.Toolbar;
import android.text.Editable;
import android.view.View;
import android.widget.EditText;
import android.widget.ListView;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;
import com.joanzapata.iconify.IconDrawable;
import com.joanzapata.iconify.fonts.IoniconsIcons;
import com.randomappsinc.foodjournal.R;
import com.randomappsinc.foodjournal.adapters.RestaurantSearchResultsAdapter;
import com.randomappsinc.foodjournal.api.RestClient;
import com.randomappsinc.foodjournal.fragments.RestaurantsFragment;
import com.randomappsinc.foodjournal.models.Restaurant;
import com.randomappsinc.foodjournal.models.SavedLocation;
import com.randomappsinc.foodjournal.persistence.DatabaseManager;
import com.randomappsinc.foodjournal.persistence.dbmanagers.LocationsDBManager;
import com.randomappsinc.foodjournal.utils.LocationFetcher;
import com.randomappsinc.foodjournal.utils.PermissionUtils;
import com.randomappsinc.foodjournal.utils.UIUtils;
import com.randomappsinc.foodjournal.views.LocationChooser;

import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import butterknife.OnItemClick;
import butterknife.OnTextChanged;
import io.nlopez.smartlocation.OnLocationUpdatedListener;
import io.nlopez.smartlocation.SmartLocation;

public class FindRestaurantActivity extends StandardActivity implements RestClient.RestaurantResultsHandler {

    private static final int LOCATION_SERVICES_CODE = 1;

    @BindView(R.id.parent) View mParent;
    @BindView(R.id.toolbar) Toolbar mToolbar;
    @BindView(R.id.search_input) EditText mSearchInput;
    @BindView(R.id.clear_search) View mClearSearch;
    @BindView(R.id.restaurants) ListView mRestaurants;
    @BindView(R.id.loading) View mLoading;
    @BindView(R.id.no_results) View mNoResults;
    @BindView(R.id.set_location) FloatingActionButton mSetLocation;

    private final LocationChooser.Callback mLocationChoiceCallback = new LocationChooser.Callback() {
        @Override
        public void onLocationChosen(SavedLocation savedLocation) {
            if (mCurrentLocation.getId() != savedLocation.getId()) {
                // Do a deep-copy so we don't alter the data objects in the location chooser
                mCurrentLocation.loadLocationInfo(savedLocation);

                if (mCurrentLocation.getId() == LocationsDBManager.AUTOMATIC_LOCATION_ID) {
                    fetchCurrentLocation();
                } else {
                    UIUtils.showSnackbar(mParent, getString(R.string.current_location_set));
                    stopFetchingCurrentLocation();
                    fetchRestaurants();
                }
            }
        }
    };

    private RestClient mRestClient;
    private RestaurantSearchResultsAdapter mAdapter;
    private boolean mLocationFetched;
    private Handler mLocationChecker;
    private Runnable mLocationCheckTask;
    private LocationFetcher mLocationFetcher;
    private SavedLocation mCurrentLocation;
    private LocationChooser mLocationChooser;
    private boolean mDenialLock;
    private boolean mPickerMode;
    private MaterialDialog mLocationDenialDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.find_restaurant);
        ButterKnife.bind(this);

        setSupportActionBar(mToolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowTitleEnabled(false);

        mPickerMode = getIntent().getBooleanExtra(RestaurantsActivity.PICKER_MODE_KEY, false);

        mRestClient = RestClient.getInstance();
        mRestClient.registerRestaurantResultsHandler(this);

        mAdapter = new RestaurantSearchResultsAdapter(this);
        mRestaurants.setAdapter(mAdapter);

        mSetLocation.setImageDrawable(new IconDrawable(this, IoniconsIcons.ion_android_map).colorRes(R.color.white));

        mDenialLock = false;
        mLocationFetcher = new LocationFetcher(this);
        mLocationChecker = new Handler();
        mLocationCheckTask = new Runnable() {
            @Override
            public void run() {
                SmartLocation.with(getBaseContext()).location().stop();
                if (!mLocationFetched) {
                    UIUtils.showSnackbar(mParent, getString(R.string.auto_location_fail));
                }
            }
        };

        mLocationDenialDialog = new MaterialDialog.Builder(this)
                .cancelable(false)
                .title(R.string.location_services_needed)
                .content(R.string.location_services_denial)
                .positiveText(R.string.location_services_confirm)
                .negativeText(R.string.enter_location_manually)
                .onPositive(new MaterialDialog.SingleButtonCallback() {
                    @Override
                    public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                        mLocationFetcher.askForLocation(LOCATION_SERVICES_CODE);
                        mDenialLock = false;
                    }
                })
                .onNegative(new MaterialDialog.SingleButtonCallback() {
                    @Override
                    public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                        mLocationChooser.show();
                        mDenialLock = false;
                    }
                })
                .build();

        mLocationChooser = new LocationChooser(this, mLocationChoiceCallback);
        mCurrentLocation = DatabaseManager.get().getLocationsDBManager().getCurrentLocation();

        // Automatically do a search if we have a pre-defined location
        if (mCurrentLocation.getId() != LocationsDBManager.AUTOMATIC_LOCATION_ID) {
            fetchRestaurants();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        // Run this here instead of onCreate() to cover the case where they return from turning on location
        if (mCurrentLocation.getId() == LocationsDBManager.AUTOMATIC_LOCATION_ID && !mDenialLock) {
            fetchCurrentLocation();
        }
    }

    private void fetchCurrentLocation() {
        if (PermissionUtils.isPermissionGranted(Manifest.permission.ACCESS_FINE_LOCATION)) {
            if (SmartLocation.with(this).location().state().locationServicesEnabled()) {
                runLocationFetch();
            } else {
                mLocationFetcher.askForLocation(LOCATION_SERVICES_CODE);
            }
        } else {
            PermissionUtils.requestPermission(this, Manifest.permission.ACCESS_FINE_LOCATION, 1);
        }
    }

    private void runLocationFetch() {
        mLocationFetched = false;
        SmartLocation.with(this).location()
                .oneFix()
                .start(new OnLocationUpdatedListener() {
                    @Override
                    public void onLocationUpdated(Location location) {
                        mLocationChecker.removeCallbacks(mLocationCheckTask);
                        mLocationFetched = true;
                        mCurrentLocation.setId(0);
                        mCurrentLocation.setAddress(String.valueOf(location.getLatitude()) + ", " + String.valueOf(location.getLongitude()));
                        fetchRestaurants();
                    }
                });
        mLocationChecker.postDelayed(mLocationCheckTask, 10000L);
    }

    @OnTextChanged(value = R.id.search_input, callback = OnTextChanged.Callback.AFTER_TEXT_CHANGED)
    public void afterTextChanged(Editable input) {
        mRestaurants.setVisibility(View.GONE);
        mNoResults.setVisibility(View.GONE);
        mLoading.setVisibility(View.VISIBLE);

        if (mCurrentLocation.getId() != LocationsDBManager.AUTOMATIC_LOCATION_ID) {
            fetchRestaurants();
        }

        if (input.length() == 0) {
            mClearSearch.setVisibility(View.GONE);
        } else {
            mClearSearch.setVisibility(View.VISIBLE);
        }
    }

    @OnClick(R.id.clear_search)
    public void clearSearch() {
        mSearchInput.setText("");
    }

    /** Fetches restaurants with the current location and search input */
    private void fetchRestaurants() {
        mRestClient.fetchRestaurants(mSearchInput.getText().toString(), mCurrentLocation.getAddress());
    }

    @Override
    public void processResults(List<Restaurant> results) {
        mLoading.setVisibility(View.GONE);
        if (results.isEmpty()) {
            mNoResults.setVisibility(View.VISIBLE);
        } else {
            mAdapter.setRestaurants(results);
            mRestaurants.setVisibility(View.VISIBLE);
        }
    }

    @OnItemClick(R.id.restaurants)
    public void onRestaurantClicked(int position) {
        Restaurant restaurant = mAdapter.getItem(position);
        if (!mPickerMode && DatabaseManager.get().getRestaurantsDBManager().userAlreadyHasRestaurant(restaurant)) {
            UIUtils.showSnackbar(mParent, getString(R.string.restaurant_already_added));
        } else {
            if (!DatabaseManager.get().getRestaurantsDBManager().userAlreadyHasRestaurant(restaurant)) {
                DatabaseManager.get().getRestaurantsDBManager().addRestaurant(restaurant);
            }

            if (mPickerMode) {
                Intent returnRestaurant = new Intent();
                returnRestaurant.putExtra(RestaurantsFragment.RESTAURANT_KEY, restaurant);
                setResult(RESULT_OK, returnRestaurant);
            } else {
                setResult(RESULT_OK);
            }
            finish();
        }
    }

    @OnClick(R.id.set_location)
    public void setLocation() {
        mLocationChooser.show();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String permissions[], @NonNull int[] grantResults) {
        if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            fetchCurrentLocation();
        }
    }

    private void stopFetchingCurrentLocation() {
        mLocationChecker.removeCallbacks(mLocationCheckTask);
        SmartLocation.with(this).location().stop();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == LOCATION_SERVICES_CODE) {
            if (resultCode == RESULT_OK) {
                UIUtils.showSnackbar(mParent, getString(R.string.location_services_on));
                runLocationFetch();
            } else {
                mDenialLock = true;
                mLocationDenialDialog.show();
            }
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        mLocationFetcher.stop();
        stopFetchingCurrentLocation();

        // Stop listening for restaurant search results
        mRestClient.unregisterRestaurantResultsHandler(this);
        mRestClient.cancelRestaurantFetch();
    }
}
