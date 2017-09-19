package com.pooja.vidya;

import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.support.v4.view.MenuItemCompat;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SearchView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.Toast;


import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class ListFragment extends AppCompatActivity implements SearchView.OnQueryTextListener,
        SwipeRefreshLayout.OnRefreshListener, SharedPreferences.OnSharedPreferenceChangeListener {
    private final String TAG = ListFragment.class.getSimpleName();
    private List<ATM> atmList;
    private RecyclerView recyclerView;
    private LinearLayout emptyView;
    private ATMAdapter atmAdapter;
    private DataProvider dataProvider;
    private String latitude, lat_dest;
    private String longitude, lng_dest;
    private String atmName, atmAddress;
    private Float distanceInKms;
    private int noOfATMs;
    private String RADIUS;
    private ATM atmObj;
    private SearchView searchView;
    private SwipeRefreshLayout swipeRefreshLayout;
    private int previousSelected = -1;
SharedPreferences sharedPreferences;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.fragment_atmlist_view);

        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);

        dataProvider = new DataProvider(this);
        recyclerView = (RecyclerView) findViewById(R.id.recycler_view);
        emptyView = (LinearLayout) findViewById(R.id.empty_view);
        swipeRefreshLayout = (SwipeRefreshLayout) findViewById(R.id.swipe_refresh_layout);
        swipeRefreshLayout.setOnRefreshListener(this);


        atmList = new ArrayList<ATM>();
        atmAdapter = new ATMAdapter(atmList,this);

        RecyclerView.LayoutManager mLayoutManager = new LinearLayoutManager(this);
        recyclerView.setLayoutManager(mLayoutManager);
        recyclerView.setItemAnimator(new DefaultItemAnimator());
        recyclerView.addItemDecoration(new RecyclerViewDecoration(this, LinearLayoutManager.VERTICAL));

        recyclerView.setAdapter(atmAdapter);
        prepareATMList();
        atmAdapter.notifyDataSetChanged();
    }




    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        return  true;
    }

    @Override
    public void onResume() {
        super.onResume();
        sharedPreferences.registerOnSharedPreferenceChangeListener(this);

    }

    @Override
    public void onPause() {
        super.onPause();
    }

    @Override
    public void onStop() {
        super.onStop();
        sharedPreferences.unregisterOnSharedPreferenceChangeListener(this);
    }

    private void prepareATMList() {
        /*Getting the current location*/
        latitude = sharedPreferences.getString("LATITUDE", null);
        longitude = sharedPreferences.getString("LONGITUDE", null);
        RADIUS = sharedPreferences.getString("RADIUS", "1000");

        Log.v(TAG + " Current Location:", latitude + longitude);

        new ATMData().execute(latitude, longitude, RADIUS);
    }

    @Override
    public boolean onQueryTextSubmit(String query) {
        return false;
    }

    @Override
    public boolean onQueryTextChange(String newText) {

        newText = newText.toString().toLowerCase();
        final List<ATM> filteredList = new ArrayList<>();
        for (int i = 0; i < atmList.size(); i++) {
            final String text = atmList.get(i).getAtmName().toLowerCase();
            if (text.contains(newText)) {
                filteredList.add(atmList.get(i));
            }
        }
        atmAdapter = new ATMAdapter(filteredList,this);
        recyclerView.setAdapter(atmAdapter);
        atmAdapter.notifyDataSetChanged();
        return false;
    }

    @Override
    public void onRefresh() {
        prepareATMList();
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        prepareATMList();
        atmAdapter = new ATMAdapter(atmList,this);
        recyclerView.setAdapter(atmAdapter);
        atmAdapter.notifyDataSetChanged();
    }

    private class ATMData extends AsyncTask<String, Void, String> {
        ProgressDialog pd = null;

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            pd = new ProgressDialog(ListFragment.this);
            pd.setMessage("loading");
            pd.show();
        }

        @Override
        protected String doInBackground(String... params) {
            String lat = params[0];
            String lng = params[1];
            String radius = params[2];
            return dataProvider.ATMData(lat, lng, radius);
        }

        @Override
        protected void onPostExecute(String result) {
            super.onPostExecute(result);
            pd.dismiss();
            addToATMList(result);
            if (atmList.isEmpty()) {
                recyclerView.setVisibility(View.GONE);
                emptyView.setVisibility(View.VISIBLE);
            } else {
                recyclerView.setVisibility(View.VISIBLE);
                emptyView.setVisibility(View.GONE);
            }
            swipeRefreshLayout.setRefreshing(false);
        }
    }

    private void addToATMList(String result) {
        if (atmList.size() > 0) {
            atmList.clear();
        }
        if (result != null) {
            try {
                JSONObject jsonObj = new JSONObject(result);
                JSONArray jsonArray = jsonObj.getJSONArray("results");
                noOfATMs = jsonArray.length();

                for (int i = 0; i < noOfATMs; i++) {

                    atmName = jsonArray.getJSONObject(i).getString("name");
                    atmAddress = jsonArray.getJSONObject(i).getString("vicinity");
                    lat_dest = jsonArray.getJSONObject(i).getJSONObject("geometry").getJSONObject("location").getString("lat");
                    lng_dest = jsonArray.getJSONObject(i).getJSONObject("geometry").getJSONObject("location").getString("lng");

                    /*Adding ATM details to ATM Object*/
                    atmObj = new ATM();
                    atmObj.setAtmName(atmName);
                    atmObj.setAtmAddress(atmAddress);
                    atmObj.setLatitude(Double.parseDouble(lat_dest));
                    atmObj.setLongitude(Double.parseDouble(lng_dest));

                    distanceInKms = dataProvider.distanceInKm(latitude, longitude, lat_dest, lng_dest);
                    atmObj.setDistance(distanceInKms);

                    /*Adding ATM Object to ATM list*/
                    if (!atmList.contains(atmObj))
                        atmList.add(atmObj);
                }
                Collections.sort(atmList, new Comparator<ATM>() {
                    @Override
                    public int compare(ATM atm1, ATM atm2) {
                        return atm1.getDistance().compareTo(atm2.getDistance());
                    }
                });
                atmAdapter.notifyDataSetChanged();
                Log.v(TAG + " MAP_ARRAY", atmList.toString());

            } catch (JSONException e) {
                Log.e(TAG, e.getMessage());
            } catch (Exception e) {
                Log.e(TAG, e.getMessage());
            }
        }
    }
}
