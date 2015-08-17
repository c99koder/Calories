package org.c99.calories;

import android.content.Intent;
import android.os.Bundle;
import android.speech.RecognizerIntent;
import android.support.wearable.activity.ConfirmationActivity;
import android.support.wearable.activity.WearableActivity;
import android.support.wearable.view.BoxInsetLayout;
import android.support.wearable.view.CircularButton;
import android.support.wearable.view.WearableListView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowInsets;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.wearable.CapabilityApi;
import com.google.android.gms.wearable.CapabilityInfo;
import com.google.android.gms.wearable.DataMap;
import com.google.android.gms.wearable.MessageApi;
import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.Wearable;

import org.c99.calories.nutritionix.Item;
import org.c99.calories.nutritionix.SearchResponse;
import org.c99.calories.nutritionix.SearchResult;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.List;

public class AddMealActivity extends WearableActivity implements MessageApi.MessageListener {
    private static class ViewHolder extends WearableListView.ViewHolder {
        TextView name;
        TextView brand;
        Item item;

        public ViewHolder(View itemView) {
            super(itemView);
            name = (TextView) itemView.findViewById(R.id.name);
            brand = (TextView) itemView.findViewById(R.id.brand);
        }
    }

    private class Adapter extends WearableListView.Adapter {
        private List<SearchResult> data;

        public Adapter(SearchResponse response) {
            data = response.hits;
        }

        @Override
        public WearableListView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.row_food, parent, false);
            return new ViewHolder(v);
        }

        @Override
        public void onBindViewHolder(WearableListView.ViewHolder viewholder, int position) {
            ViewHolder holder = (ViewHolder)viewholder;
            holder.item = data.get(position).fields;
            holder.name.setText(holder.item.item_name);
            holder.brand.setText(holder.item.brand_name);
        }

        @Override
        public int getItemCount() {
            return data.size();
        }
    }

    private BoxInsetLayout mContainerView;
    private ProgressBar mProgress;
    private WearableListView mList;
    private LinearLayout mAnythingElse;

    private GoogleApiClient mGoogleApiClient;

    private static final int SPEECH_REQUEST_CODE = 0;
    private static final int QUANTITY_REQUEST_CODE = 1;

    private ArrayList<Item> meal = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_meal);

        mContainerView = (BoxInsetLayout) findViewById(R.id.container);
        mContainerView.setOnApplyWindowInsetsListener(new View.OnApplyWindowInsetsListener() {
            @Override
            public WindowInsets onApplyWindowInsets(View view, WindowInsets windowInsets) {
                ViewGroup.MarginLayoutParams lp = (ViewGroup.MarginLayoutParams) mContainerView.getLayoutParams();
                lp.setMargins(lp.leftMargin, lp.topMargin, lp.rightMargin, windowInsets.getSystemWindowInsetBottom());
                mContainerView.setLayoutParams(lp);
                return windowInsets;
            }
        });
        mAnythingElse = (LinearLayout) findViewById(R.id.anythingElse);
        mProgress = (ProgressBar) findViewById(R.id.progress);
        mList = (WearableListView) findViewById(R.id.list);
        mList.setClickListener(new WearableListView.ClickListener() {
            @Override
            public void onClick(WearableListView.ViewHolder viewHolder) {
                Intent i = new Intent(AddMealActivity.this, QuantityActivity.class);
                i.putExtra("item", ((ViewHolder)viewHolder).item);
                startActivityForResult(i, QUANTITY_REQUEST_CODE);
            }

            @Override
            public void onTopEmptyRegionClick() {

            }
        });

        CircularButton b = (CircularButton) findViewById(R.id.cancel);
        b.setImageResource(R.drawable.ic_action_cancel);
        b.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Wearable.CapabilityApi.getCapability(mGoogleApiClient, "add_meal", CapabilityApi.FILTER_REACHABLE).setResultCallback(new ResultCallback<CapabilityApi.GetCapabilityResult>() {
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

                        Log.d("Calories", "Sending /add-meal to node ID " + nodeId);
                        ByteArrayOutputStream bos = new ByteArrayOutputStream();
                        ObjectOutput out = null;
                        try {
                            out = new ObjectOutputStream(bos);
                            out.writeObject(meal);
                            Wearable.MessageApi.sendMessage(mGoogleApiClient, nodeId, "/add-meal", bos.toByteArray()).setResultCallback(new ResultCallback<MessageApi.SendMessageResult>() {
                                @Override
                                public void onResult(MessageApi.SendMessageResult sendMessageResult) {
                                    Intent intent = new Intent(AddMealActivity.this, ConfirmationActivity.class);
                                    if(sendMessageResult.getStatus().isSuccess()) {
                                        intent.putExtra(ConfirmationActivity.EXTRA_ANIMATION_TYPE, ConfirmationActivity.SUCCESS_ANIMATION);
                                        intent.putExtra(ConfirmationActivity.EXTRA_MESSAGE, "Meal Saved");
                                    } else {
                                        intent.putExtra(ConfirmationActivity.EXTRA_ANIMATION_TYPE, ConfirmationActivity.FAILURE_ANIMATION);
                                        intent.putExtra(ConfirmationActivity.EXTRA_MESSAGE, "Meal Not Saved");
                                        Log.e("Calories", "Message not sent: " + sendMessageResult);
                                    }
                                    startActivity(intent);
                                    finish();
                                }
                            });
                        } catch (Exception e) {
                            Log.e("Calories", "Failure: " + e);
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
                });
            }
        });

        b = (CircularButton) findViewById(R.id.confirm);
        b.setImageResource(R.drawable.ic_action_confirm);
        b.setColor(getResources().getColor(android.R.color.holo_blue_light));
        b.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                request_speech();
            }
        });

        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(new GoogleApiClient.ConnectionCallbacks() {
                    @Override
                    public void onConnected(Bundle connectionHint) {
                        Log.e("Calories", "Play services connected, registering message listener");
                        Wearable.MessageApi.addListener(mGoogleApiClient, AddMealActivity.this);
                    }
                    @Override
                    public void onConnectionSuspended(int cause) {
                    }
                })
                .addOnConnectionFailedListener(new GoogleApiClient.OnConnectionFailedListener() {
                    @Override
                    public void onConnectionFailed(ConnectionResult result) {
                    }
                })
                .addApi(Wearable.API)
                .build();

        request_speech();
    }

    private void request_speech() {
        mAnythingElse.setVisibility(View.GONE);
        mList.setAdapter(null);
        mProgress.setVisibility(View.VISIBLE);
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_PROMPT, "What did you eat?");
        startActivityForResult(intent, SPEECH_REQUEST_CODE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == SPEECH_REQUEST_CODE && resultCode == RESULT_OK) {
            final List<String> results = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
            Wearable.CapabilityApi.getCapability(mGoogleApiClient, "food_search", CapabilityApi.FILTER_REACHABLE).setResultCallback(new ResultCallback<CapabilityApi.GetCapabilityResult>() {
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

                    Log.d("Calories", "Sending query '" + results.get(0) + "' to node " + nodeId);
                    DataMap map = new DataMap();
                    map.putString("query", results.get(0));
                    Wearable.MessageApi.sendMessage(mGoogleApiClient, nodeId, "/search", map.toByteArray()).setResultCallback(new ResultCallback<MessageApi.SendMessageResult>() {
                        @Override
                        public void onResult(MessageApi.SendMessageResult sendMessageResult) {
                            if(!sendMessageResult.getStatus().isSuccess()) {
                                Log.e("Calories", "Message failed");
                            }
                        }
                    });
                }
            });
        }
        if (requestCode == QUANTITY_REQUEST_CODE && resultCode == RESULT_OK) {
            meal.add((Item) data.getSerializableExtra("item"));
            mAnythingElse.setVisibility(View.VISIBLE);
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    protected void onStart() {
        Log.e("Calories-Wear", "onStart");
        super.onStart();
        mGoogleApiClient.connect();
    }

    @Override
    protected void onStop() {
        Log.e("Calories-Wear", "onStop");
        super.onStop();
        Wearable.MessageApi.removeListener(mGoogleApiClient, this);
        mGoogleApiClient.disconnect();
    }

    @Override
    public void onMessageReceived(MessageEvent messageEvent) {
        Log.d("Calories", "Got " + messageEvent.getPath() + " from node " + messageEvent.getSourceNodeId());
        if(messageEvent.getPath().equals("/search-results")) {
            DataMap map = DataMap.fromByteArray(messageEvent.getData());

            ByteArrayInputStream bis = new ByteArrayInputStream(map.getByteArray("results"));
            ObjectInput in = null;
            try {
                in = new ObjectInputStream(bis);
                final SearchResponse searchResponse = (SearchResponse)in.readObject();

                Log.d("Calories", "Got results for query '" + map.getString("query") + "': " + searchResponse.hits);
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        mList.setAdapter(new Adapter(searchResponse));
                        mProgress.setVisibility(View.GONE);
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
        } else if(messageEvent.getPath().equals("/search-failed")) {
            Log.e("Calories", "Search failed");
        }
    }
}
