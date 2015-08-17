package org.c99.calories;

import android.app.IntentService;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.webkit.CookieManager;
import android.webkit.CookieSyncManager;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.wearable.PutDataMapRequest;
import com.google.android.gms.wearable.Wearable;
import com.jawbone.upplatformsdk.api.ApiManager;
import com.jawbone.upplatformsdk.utils.UpPlatformSdkConstants;

import java.util.Map;

import retrofit.Callback;
import retrofit.RetrofitError;
import retrofit.client.Response;

public class CaloriesSyncService extends IntentService {
    private static final String TAG = "CaloriesSyncService";
    public static final String ACTION_ADD_MEAL = "org.c99.ADD_MEAL";

    private GoogleApiClient mGoogleApiClient;

    public CaloriesSyncService() {
        super("CaloriesSyncService");
    }

    @Override
    protected void onHandleIntent(final Intent intent) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        if (intent != null && prefs.contains(UpPlatformSdkConstants.UP_PLATFORM_ACCESS_TOKEN)) {
            final String action = intent.getAction();
            ApiManager.getRequestInterceptor().setAccessToken(prefs.getString(UpPlatformSdkConstants.UP_PLATFORM_ACCESS_TOKEN, ""));

            mGoogleApiClient = new GoogleApiClient.Builder(this)
                    .addConnectionCallbacks(new GoogleApiClient.ConnectionCallbacks() {
                        @Override
                        public void onConnected(Bundle connectionHint) {
                            if (ACTION_ADD_MEAL.equals(action)) {
                            } else {
                                ApiManager.getRestApiInterface().getUsersGoals(UpPlatformSdkConstants.API_VERSION_STRING, new Callback<Object>() {
                                    @Override
                                    public void success(Object r, Response response) {
                                        Map<String, Map<String, Object>> result = (Map<String, Map<String, Object>>)r;
                                        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(CaloriesSyncService.this);
                                        if (result.containsKey("meta")) {
                                            Map<String, Object> meta = result.get("meta");
                                            int code = ((Double) meta.get("code")).intValue();
                                            if (code == 200) {
                                                Map<String, Object> data = result.get("data");
                                                Map<String, Object> remaining = (Map<String, Object>) data.get("remaining_for_day");
                                                PutDataMapRequest req = PutDataMapRequest.create("/calories");
                                                req.getDataMap().putDouble("value", ((Double) remaining.get("intake_calories_remaining")));
                                                Wearable.DataApi.putDataItem(mGoogleApiClient, req.asPutDataRequest());
                                            } else if (code == 401) {
                                                ApiManager.getRequestInterceptor().clearAccessToken();
                                                SharedPreferences.Editor editor = preferences.edit();
                                                editor.remove(UpPlatformSdkConstants.UP_PLATFORM_REFRESH_TOKEN);
                                                editor.remove(UpPlatformSdkConstants.UP_PLATFORM_ACCESS_TOKEN);
                                                editor.apply();

                                                CookieSyncManager.createInstance(CaloriesSyncService.this);
                                                CookieManager cookieManager = CookieManager.getInstance();
                                                cookieManager.removeAllCookie();
                                                cookieManager.setAcceptCookie(true);
                                            }
                                        }
                                        CaloriesSyncReceiver.completeWakefulIntent(intent);
                                        mGoogleApiClient.disconnect();
                                    }

                                    @Override
                                    public void failure(RetrofitError retrofitError) {
                                        Log.e("Calories", "failed to get user:" + retrofitError.getMessage());
                                        CaloriesSyncReceiver.completeWakefulIntent(intent);
                                        mGoogleApiClient.disconnect();
                                    }
                                });
                            }
                        }
                        @Override
                        public void onConnectionSuspended(int cause) {
                            Log.d(TAG, "onConnectionSuspended: " + cause);
                        }
                    })
                    .addOnConnectionFailedListener(new GoogleApiClient.OnConnectionFailedListener() {
                        @Override
                        public void onConnectionFailed(ConnectionResult result) {
                            Log.d(TAG, "onConnectionFailed: " + result);
                        }
                    })
                    .addApi(Wearable.API)
                    .build();

            mGoogleApiClient.connect();
        }
    }
}
