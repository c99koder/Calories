package org.c99.calories;

import android.content.Intent;
import android.preference.PreferenceManager;
import android.util.Log;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.wearable.DataMap;
import com.google.android.gms.wearable.MessageApi;
import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.Wearable;
import com.google.android.gms.wearable.WearableListenerService;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.jawbone.upplatformsdk.api.ApiManager;
import com.jawbone.upplatformsdk.utils.UpPlatformSdkConstants;

import org.c99.calories.nutritionix.Item;
import org.c99.calories.nutritionix.Nutritionix;
import org.c99.calories.nutritionix.SearchResponse;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import retrofit.Callback;
import retrofit.RetrofitError;
import retrofit.client.Response;

public class MessageListenerService extends WearableListenerService {
    private final static String TAG = "Calories-Message";

    public MessageListenerService() {
    }

    @Override
    public void onMessageReceived(final MessageEvent messageEvent) {
        Log.e(TAG, "Got message " + messageEvent.getPath() + " from node " + messageEvent.getSourceNodeId());
        if(messageEvent.getPath().equals("/update-calories")) {
            sendBroadcast(new Intent(this, CaloriesSyncReceiver.class));
            return;
        }

        final GoogleApiClient googleApiClient = new GoogleApiClient.Builder(this)
                .addApi(Wearable.API)
                .build();

        ConnectionResult connectionResult =
                googleApiClient.blockingConnect(30, TimeUnit.SECONDS);

        if (!connectionResult.isSuccess()) {
            Log.e(TAG, "Failed to connect to GoogleApiClient.");
            return;
        }

        if(messageEvent.getPath().equals("/add-meal")) {
            ByteArrayInputStream bis = new ByteArrayInputStream(messageEvent.getData());
            ObjectInput in = null;
            try {
                in = new ObjectInputStream(bis);
                final ArrayList<Item> meal = (ArrayList<Item>)in.readObject();

                Log.d("Calories", "Got items for meal: " + meal);
                HashMap<String, Object> map = new HashMap<>();
                JsonArray items = new JsonArray();
                String note = "";
                for(Item item : meal) {
                    if(note.length() > 0)
                        note += ", ";
                    note += item.item_name;
                    JsonObject o = new JsonObject();
                    o.addProperty("name", item.item_name);
                    o.addProperty("amount", item.amount);
                    o.addProperty("measurement", item.nf_serving_size_unit);
                    o.addProperty("calories", item.nf_calories * item.amount);
                    o.addProperty("carbohydrate", item.nf_total_carbohydrate * item.amount);
                    o.addProperty("cholesterol", item.nf_cholesterol * item.amount);
                    o.addProperty("fiber", item.nf_dietary_fiber * item.amount);
                    o.addProperty("protein", item.nf_protein * item.amount);
                    o.addProperty("saturated_fat", item.nf_saturated_fat * item.amount);
                    o.addProperty("sodium", item.nf_sodium * item.amount);
                    o.addProperty("sugar", item.nf_sugars * item.amount);
                    items.add(o);
                }
                map.put("note", note);
                map.put("items", items.toString());

                ApiManager.getRequestInterceptor().setAccessToken(PreferenceManager.getDefaultSharedPreferences(this).getString(UpPlatformSdkConstants.UP_PLATFORM_ACCESS_TOKEN, ""));
                ApiManager.getRestApiInterface().createMealEvent(UpPlatformSdkConstants.API_VERSION_STRING, map, new Callback<Object>() {
                    @Override
                    public void success(Object r, Response response) {
                        Map<String, Map<String, Object>> result = (Map<String, Map<String, Object>>)r;
                        if (result.containsKey("meta")) {
                            Map<String, Object> meta = result.get("meta");
                            int code = ((Double) meta.get("code")).intValue();
                            if (code == 201) {
                                Log.d(TAG, "Success!");
                                sendBroadcast(new Intent(MessageListenerService.this, CaloriesSyncReceiver.class));
                            } else {
                                Log.e(TAG, "Failure");
                            }
                        }
                    }

                    @Override
                    public void failure(RetrofitError retrofitError) {
                        Log.e(TAG, "Failure: " + retrofitError);
                    }
                });
            } catch (Exception e) {
                Log.e("Calories", "Failed to parse results: " + e);
            } finally {
                try {
                    bis.close();
                } catch (IOException ex) {
                    // ignore close exception
                }
                try {
                    if (in != null) {
                        in.close();
                    }
                } catch (IOException ex) {
                    // ignore close exception
                }
            }
        } else if(messageEvent.getPath().equals("/search")) {
            final DataMap map = DataMap.fromByteArray(messageEvent.getData());
            Nutritionix.getInterface().search(map.getString("query"), MainActivity.NUTRITIONIX_APP_ID, MainActivity.NUTRITIONIX_SECRET, new Callback<SearchResponse>() {
                @Override
                public void success(SearchResponse searchResponse, Response response) {
                    ByteArrayOutputStream bos = new ByteArrayOutputStream();
                    ObjectOutput out = null;
                    try {
                        out = new ObjectOutputStream(bos);
                        out.writeObject(searchResponse);
                        map.putByteArray("results", bos.toByteArray());
                        Log.d(TAG, "Sending results for query '" + map.getString("query") + "' to node " + messageEvent.getSourceNodeId() + ": " + searchResponse.hits);
                        Wearable.MessageApi.sendMessage(googleApiClient, messageEvent.getSourceNodeId(), "/search-results", map.toByteArray()).setResultCallback(new ResultCallback<MessageApi.SendMessageResult>() {
                            @Override
                            public void onResult(MessageApi.SendMessageResult sendMessageResult) {
                                if(!sendMessageResult.getStatus().isSuccess())
                                    Log.e("Calories", "Message not sent: " + sendMessageResult);
                                googleApiClient.disconnect();
                            }
                        });
                    } catch (Exception e) {
                        Log.e("Calories", "Failure: " + e);
                        Wearable.MessageApi.sendMessage(googleApiClient, messageEvent.getSourceNodeId(), "/search-failed", null).setResultCallback(new ResultCallback<MessageApi.SendMessageResult>() {
                            @Override
                            public void onResult(MessageApi.SendMessageResult sendMessageResult) {
                                if (!sendMessageResult.getStatus().isSuccess())
                                    Log.e("Calories", "Message not sent: " + sendMessageResult);
                                googleApiClient.disconnect();
                            }
                        });
                    } finally {
                        try {
                            if (out != null) {
                                out.close();
                            }
                        } catch (IOException ex) {
                        }
                        try {
                            bos.close();
                        } catch (IOException ex) {
                        }
                    }
                }

                @Override
                public void failure(RetrofitError retrofitError) {
                    Log.e(TAG, "Error: " + retrofitError);
                    Wearable.MessageApi.sendMessage(googleApiClient, messageEvent.getSourceNodeId(), "/search-failed", null).await();
                    googleApiClient.disconnect();
                }
            });

        }
    }
}
