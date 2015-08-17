package org.c99.calories;

import android.os.Bundle;
import android.support.wearable.activity.WearableActivity;
import android.support.wearable.view.BoxInsetLayout;
import android.support.wearable.view.WearableListView;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowInsets;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.c99.calories.nutritionix.Item;
import org.c99.calories.nutritionix.SearchResponse;
import org.c99.calories.nutritionix.SearchResult;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class QuantityActivity extends WearableActivity {
    private static class ViewHolder extends WearableListView.ViewHolder {
        TextView text1;
        float value;

        public ViewHolder(View itemView) {
            super(itemView);
            text1 = (TextView) itemView.findViewById(android.R.id.text1);
        }
    }

    private class Adapter extends WearableListView.Adapter {

        @Override
        public WearableListView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(android.R.layout.simple_list_item_1, parent, false);
            return new ViewHolder(v);
        }

        @Override
        public void onBindViewHolder(WearableListView.ViewHolder viewholder, int position) {
            ViewHolder holder = (ViewHolder)viewholder;
            holder.value = (float)position * 0.25f;
            holder.text1.setText(String.valueOf(holder.value));
            holder.text1.setGravity(Gravity.RIGHT);
        }

        @Override
        public int getItemCount() {
            return 100;
        }
    }

    private LinearLayout mContainerView;
    private WearableListView mList;
    private TextView mUnits;
    private TextView mCalories;
    private float calories_per_serving = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_quantity);

        mContainerView = (LinearLayout) findViewById(R.id.container);
        mContainerView.setOnApplyWindowInsetsListener(new View.OnApplyWindowInsetsListener() {
            @Override
            public WindowInsets onApplyWindowInsets(View view, WindowInsets windowInsets) {
                ViewGroup.MarginLayoutParams lp = (ViewGroup.MarginLayoutParams) mContainerView.getLayoutParams();
                lp.setMargins(lp.leftMargin, lp.topMargin, lp.rightMargin, windowInsets.getSystemWindowInsetBottom());
                mContainerView.setLayoutParams(lp);
                return windowInsets;
            }
        });
        mList = (WearableListView) findViewById(R.id.list);
        mList.setAdapter(new Adapter());
        mList.addOnCentralPositionChangedListener(new WearableListView.OnCentralPositionChangedListener() {
            @Override
            public void onCentralPositionChanged(int i) {
                float qty = (float)i * 0.25f;
                mCalories.setText(String.valueOf((int)(qty * calories_per_serving)) + " Calories");
            }
        });
        mList.setClickListener(new WearableListView.ClickListener() {
            @Override
            public void onClick(WearableListView.ViewHolder viewHolder) {
                Item item = (Item) getIntent().getSerializableExtra("item");
                item.amount = ((ViewHolder)viewHolder).value;
                getIntent().putExtra("item", item);
                setResult(RESULT_OK, getIntent());
                finish();
            }

            @Override
            public void onTopEmptyRegionClick() {

            }
        });
        mUnits = (TextView) findViewById(R.id.units);
        mCalories = (TextView) findViewById(R.id.calories);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if(getIntent() != null && getIntent().hasExtra("item")) {
            Item item = (Item) getIntent().getSerializableExtra("item");
            mUnits.setText(item.nf_serving_size_unit);
            calories_per_serving = item.nf_calories;
            mList.scrollToPosition(4);
        }
    }
}
