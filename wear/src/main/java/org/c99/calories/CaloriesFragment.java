package org.c99.calories;


import android.os.Bundle;
import android.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.wearable.CapabilityApi;
import com.google.android.gms.wearable.CapabilityInfo;
import com.google.android.gms.wearable.DataApi;
import com.google.android.gms.wearable.DataEvent;
import com.google.android.gms.wearable.DataEventBuffer;
import com.google.android.gms.wearable.DataItem;
import com.google.android.gms.wearable.DataItemBuffer;
import com.google.android.gms.wearable.DataMap;
import com.google.android.gms.wearable.DataMapItem;
import com.google.android.gms.wearable.MessageApi;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.Wearable;


public class CaloriesFragment extends Fragment implements DataApi.DataListener,
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener {
    private TextView calories;
    private TextView calories_remaining;
    private ProgressBar progressBar;
    private GoogleApiClient mGoogleApiClient;

    public CaloriesFragment() {
    }

    @Override
    public void onStart() {
        super.onStart();
        mGoogleApiClient.connect();
    }

    @Override
    public void onStop() {
        super.onStop();
        Wearable.DataApi.removeListener(mGoogleApiClient, this);
        mGoogleApiClient.disconnect();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mGoogleApiClient = new GoogleApiClient.Builder(getActivity())
                .addApi(Wearable.API)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .build();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_calories, container, false);
        calories = (TextView) v.findViewById(R.id.calories);
        calories_remaining = (TextView) v.findViewById(R.id.calories_remaining);
        progressBar = (ProgressBar) v.findViewById(R.id.progressBar);
        return v;
    }

    private void update_calories(final double c) {
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                calories.setText(String.valueOf((int) c));
                progressBar.setVisibility(View.GONE);
                calories.setVisibility(View.VISIBLE);
                calories_remaining.setVisibility(View.VISIBLE);
            }
        });
    }

    public void enterAmbient() {
        calories.setTextColor(getResources().getColor(android.R.color.darker_gray));
        calories_remaining.setTextColor(getResources().getColor(android.R.color.darker_gray));
    }

    public void exitAmbient() {
        calories.setTextColor(getResources().getColor(android.R.color.white));
        calories_remaining.setTextColor(getResources().getColor(android.R.color.secondary_text_dark));
    }

    @Override
    public void onConnected(Bundle bundle) {
        Log.d("Calories", "Play Services connected");
        Wearable.DataApi.addListener(mGoogleApiClient, this);
        Wearable.DataApi.getDataItems(mGoogleApiClient).setResultCallback(new ResultCallback<DataItemBuffer>() {
            @Override
            public void onResult(DataItemBuffer dataItems) {
                for (DataItem item : dataItems) {
                    Log.d("Calories", "Data item: " + item.getUri().toString());
                    if (item.getUri().getPath().compareTo("/calories") == 0) {
                        DataMap dataMap = DataMapItem.fromDataItem(item).getDataMap();
                        update_calories(dataMap.getDouble("value"));
                    }
                }
                dataItems.release();
            }
        });
        Wearable.CapabilityApi.getCapability(mGoogleApiClient, "update_calories", CapabilityApi.FILTER_REACHABLE).setResultCallback(new ResultCallback<CapabilityApi.GetCapabilityResult>() {
            @Override
            public void onResult(CapabilityApi.GetCapabilityResult getCapabilityResult) {
                CapabilityInfo info = getCapabilityResult.getCapability();
                String nodeId = null;
                for(Node node : info.getNodes()) {
                    if(node.isNearby()) {
                        nodeId = node.getId();
                        break;
                    }
                    nodeId = node.getId();
                }

                Log.d("Calories", "Sending /update-calories to node ID " + nodeId);

                Wearable.MessageApi.sendMessage(mGoogleApiClient, nodeId, "/update-calories", null).setResultCallback(new ResultCallback<MessageApi.SendMessageResult>() {
                    @Override
                    public void onResult(MessageApi.SendMessageResult sendMessageResult) {
                        if (!sendMessageResult.getStatus().isSuccess()) {
                            Log.e("Calories", "Message failed");
                        }
                    }
                });
            }
        });
    }

    @Override
    public void onConnectionSuspended(int i) {
        Log.d("Calories", "Connection Suspended");
    }

    @Override
    public void onDataChanged(DataEventBuffer dataEventBuffer) {
        for(DataEvent event : dataEventBuffer) {
            if(event.getType() == DataEvent.TYPE_CHANGED) {
                DataItem item = event.getDataItem();
                Log.e("Calories", "Data changed: " + item.getUri());
                if(item.getUri().getPath().compareTo("/calories") == 0) {
                    DataMap dataMap = DataMapItem.fromDataItem(item).getDataMap();
                    update_calories(dataMap.getDouble("value"));
                }
            }
        }
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        Log.e("Calories", "Play Services connection failed: " + connectionResult.toString());
    }
}
