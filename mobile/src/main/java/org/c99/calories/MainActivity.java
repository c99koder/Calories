package org.c99.calories;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.SystemClock;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.CookieManager;
import android.webkit.CookieSyncManager;
import android.widget.BaseAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.wearable.DataApi;
import com.google.android.gms.wearable.DataEvent;
import com.google.android.gms.wearable.DataEventBuffer;
import com.google.android.gms.wearable.DataItem;
import com.google.android.gms.wearable.DataItemBuffer;
import com.google.android.gms.wearable.DataMap;
import com.google.android.gms.wearable.DataMapItem;
import com.google.android.gms.wearable.Wearable;
import com.jawbone.upplatformsdk.api.ApiManager;
import com.jawbone.upplatformsdk.api.response.OauthAccessTokenResponse;
import com.jawbone.upplatformsdk.oauth.OauthUtils;
import com.jawbone.upplatformsdk.oauth.OauthWebViewActivity;
import com.jawbone.upplatformsdk.utils.UpPlatformSdkConstants;

import org.c99.calories.nutritionix.Item;
import org.c99.calories.nutritionix.Nutritionix;
import org.c99.calories.nutritionix.SearchResponse;
import org.c99.calories.nutritionix.SearchResult;

import java.util.ArrayList;
import java.util.List;

import retrofit.Callback;
import retrofit.RetrofitError;
import retrofit.client.Response;

public class MainActivity extends AppCompatActivity implements
        DataApi.DataListener,
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener {
    public final static String JAWBONE_APP_ID = "";
    public final static String JAWBONE_SECRET = "";
    public final static String NUTRITIONIX_APP_ID = "";
    public final static String NUTRITIONIX_SECRET = "";

    private GoogleApiClient mGoogleApiClient;
    private TextView calories;
    private EditText search;
    private ListView listView;

    private PendingIntent alarmIntent;

    public static class Adapter extends BaseAdapter {
        private class ViewHolder {
            TextView name;
            TextView brand;
            TextView data;
        }

        private Context context;
        private List<SearchResult> data;

        public Adapter(Context ctx, SearchResponse response) {
            context = ctx;
            data = response.hits;
        }

        @Override
        public int getCount() {
            return data.size();
        }

        @Override
        public Object getItem(int i) {
            return data.get(i);
        }

        @Override
        public long getItemId(int i) {
            return i;
        }

        @Override
        public View getView(int i, View view, ViewGroup viewGroup) {
            ViewHolder holder;
            if(view == null) {
                view = LayoutInflater.from(context).inflate(R.layout.row_food, viewGroup, false);
                holder = new ViewHolder();
                holder.name = (TextView) view.findViewById(R.id.name);
                holder.brand = (TextView) view.findViewById(R.id.brand);
                holder.data = (TextView) view.findViewById(R.id.data);
                view.setTag(holder);
            } else {
                holder = (ViewHolder) view.getTag();
            }

            Item item = data.get(i).fields;
            holder.name.setText(item.item_name);
            holder.brand.setText(item.brand_name);
            holder.data.setText(item.nf_calories + " cal • " + item.nf_total_fat + " fat • " + item.nf_protein + " protein");
            return view;
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        mGoogleApiClient.connect();
    }

    @Override
    protected void onStop() {
        super.onStop();
        Wearable.DataApi.removeListener(mGoogleApiClient, this);
        mGoogleApiClient.disconnect();
    }

    @Override
    public void onConnected(Bundle bundle) {
        Wearable.DataApi.addListener(mGoogleApiClient, this);
        Wearable.DataApi.getDataItems(mGoogleApiClient).setResultCallback(new ResultCallback<DataItemBuffer>() {
            @Override
            public void onResult(DataItemBuffer dataItems) {
                for(DataItem item : dataItems) {
                    if(item.getUri().getPath().compareTo("/calories") == 0) {
                        DataMap dataMap = DataMapItem.fromDataItem(item).getDataMap();
                        update_calories(dataMap.getDouble("value"));
                    }
                }
                dataItems.release();
            }
        });
    }

    @Override
    public void onConnectionSuspended(int i) {
        Log.d("Calories", "onConnectionSuspended: " + i);
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        Log.d("Calories", "onConnectionFailed: " + connectionResult);
    }

    @Override
    public void onDataChanged(DataEventBuffer dataEventBuffer) {
        for(DataEvent event : dataEventBuffer) {
            if(event.getType() == DataEvent.TYPE_CHANGED) {
                DataItem item = event.getDataItem();
                if(item.getUri().getPath().compareTo("/calories") == 0) {
                    DataMap dataMap = DataMapItem.fromDataItem(item).getDataMap();
                    update_calories(dataMap.getDouble("value"));
                }
            }
        }
    }

    private void update_calories(final double c) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                calories.setText("Calories remaining: " + (int)c);
            }
        });
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addApi(Wearable.API)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .build();

        if(savedInstanceState != null) {
            if(savedInstanceState.containsKey("alarmIntent"))
                alarmIntent = savedInstanceState.getParcelable("alarmIntent");
        }

        calories = (TextView) findViewById(R.id.calories);
        search = (EditText) findViewById(R.id.search);
        listView = (ListView) findViewById(R.id.listView);
        findViewById(R.id.searchBtn).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(final View view) {
                search.setEnabled(false);
                view.setEnabled(false);
                Nutritionix.getInterface().search(search.getText().toString(), NUTRITIONIX_APP_ID, NUTRITIONIX_SECRET, new Callback<SearchResponse>() {
                    @Override
                    public void success(SearchResponse searchResponse, Response response) {
                        search.setEnabled(true);
                        view.setEnabled(true);
                        listView.setAdapter(new Adapter(MainActivity.this, searchResponse));
                    }

                    @Override
                    public void failure(RetrofitError retrofitError) {
                        Log.e("Calories", "Error: " + retrofitError);
                        search.setEnabled(true);
                        view.setEnabled(true);
                    }
                });
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        if (prefs.contains(UpPlatformSdkConstants.UP_PLATFORM_ACCESS_TOKEN)) {
            if(alarmIntent != null) {
                AlarmManager am = (AlarmManager) getSystemService(ALARM_SERVICE);
                am.cancel(alarmIntent);
                alarmIntent = null;
            }
            sendBroadcast(new Intent(MainActivity.this, CaloriesSyncReceiver.class));
        } else {
            logout();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();

        AlarmManager am = (AlarmManager) getSystemService(ALARM_SERVICE);
        if(alarmIntent != null)
            am.cancel(alarmIntent);
        alarmIntent = PendingIntent.getBroadcast(this, 0, new Intent(MainActivity.this, CaloriesSyncReceiver.class), 0);
        am.cancel(alarmIntent);
        am.setInexactRepeating(AlarmManager.ELAPSED_REALTIME_WAKEUP, SystemClock.elapsedRealtime(), AlarmManager.INTERVAL_FIFTEEN_MINUTES, alarmIntent);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        if(alarmIntent != null)
            outState.putParcelable("alarmIntent", alarmIntent);
    }

    private void logout() {
        ApiManager.getRequestInterceptor().clearAccessToken();
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
        SharedPreferences.Editor editor = preferences.edit();
        editor.remove(UpPlatformSdkConstants.UP_PLATFORM_REFRESH_TOKEN);
        editor.remove(UpPlatformSdkConstants.UP_PLATFORM_ACCESS_TOKEN);
        editor.remove("calories");
        editor.apply();

        CookieSyncManager.createInstance(this);
        CookieManager cookieManager = CookieManager.getInstance();
        cookieManager.removeAllCookie();
        cookieManager.setAcceptCookie(true);

        List<UpPlatformSdkConstants.UpPlatformAuthScope> authScope = new ArrayList<>();
        authScope.add(UpPlatformSdkConstants.UpPlatformAuthScope.BASIC_READ);
        authScope.add(UpPlatformSdkConstants.UpPlatformAuthScope.MEAL_READ);
        authScope.add(UpPlatformSdkConstants.UpPlatformAuthScope.MEAL_WRITE);
        Uri.Builder builder = OauthUtils.setOauthParameters("9B6z_KBdMtE", "https://localhost/calories?", authScope);

        Intent intent = new Intent(this, OauthWebViewActivity.class);
        intent.putExtra(UpPlatformSdkConstants.AUTH_URI, builder.build());
        startActivityForResult(intent, UpPlatformSdkConstants.JAWBONE_AUTHORIZE_REQUEST_CODE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == UpPlatformSdkConstants.JAWBONE_AUTHORIZE_REQUEST_CODE && resultCode == RESULT_OK) {

            String code = data.getStringExtra(UpPlatformSdkConstants.ACCESS_CODE);
            if (code != null) {
                ApiManager.getRequestInterceptor().clearAccessToken();
                ApiManager.getRestApiInterface().getAccessToken(
                        JAWBONE_APP_ID,
                        JAWBONE_SECRET,
                        code,
                        accessTokenRequestListener);
            }
        }
    }

    private Callback accessTokenRequestListener = new Callback<OauthAccessTokenResponse>() {
        @Override
        public void success(OauthAccessTokenResponse result, Response response) {

            if (result.access_token != null) {
                SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(MainActivity.this);
                SharedPreferences.Editor editor = preferences.edit();
                editor.putString(UpPlatformSdkConstants.UP_PLATFORM_ACCESS_TOKEN, result.access_token);
                editor.putString(UpPlatformSdkConstants.UP_PLATFORM_REFRESH_TOKEN, result.refresh_token);
                editor.commit();

                sendBroadcast(new Intent(MainActivity.this, CaloriesSyncReceiver.class));
            } else {
                Log.e("Calories", "accessToken not returned by Oauth call, exiting...");
            }
        }

        @Override
        public void failure(RetrofitError retrofitError) {
            Log.e("Calories", "failed to get accessToken:" + retrofitError.getMessage());
        }
    };

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
