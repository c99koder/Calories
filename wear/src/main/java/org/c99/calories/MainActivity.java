package org.c99.calories;

import android.app.Fragment;
import android.app.FragmentManager;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.wearable.activity.WearableActivity;
import android.support.wearable.view.BoxInsetLayout;
import android.support.wearable.view.DotsPageIndicator;
import android.support.wearable.view.FragmentGridPagerAdapter;
import android.support.wearable.view.GridPagerAdapter;
import android.support.wearable.view.GridViewPager;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowInsets;

public class MainActivity extends WearableActivity implements ActionFragment.OnActionClickedListener {
    private class Adapter extends FragmentGridPagerAdapter {

        public Adapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getFragment(int row, int col) {
            switch (col) {
                case 0:
                    return mCaloriesFragment;
                case 1:
                    return mAddActionFragment;
                default:
                    return null;
            }
        }

        @Override
        public int getRowCount() {
            return 1;
        }

        @Override
        public int getColumnCount(int i) {
            return 2;
        }

        @Override
        public Drawable getBackgroundForPage(int row, int column) {
            return GridPagerAdapter.BACKGROUND_NONE;
        }
    }

    private BoxInsetLayout mContainerView;
    private GridViewPager mPager;
    private CaloriesFragment mCaloriesFragment;
    private ActionFragment mAddActionFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        setAmbientEnabled();
        mCaloriesFragment = new CaloriesFragment();
        mAddActionFragment = ActionFragment.newInstance(R.drawable.ic_content_add, "Add Meal");

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

        mPager = (GridViewPager)findViewById(R.id.pager);
        mPager.setAdapter(new Adapter(getFragmentManager()));

        DotsPageIndicator dots = (DotsPageIndicator) findViewById(R.id.dots);
        dots.setPager(mPager);
    }

    @Override
    protected void onResume() {
        super.onResume();
        mPager.setCurrentItem(0, 0);
    }

    @Override
    public void onEnterAmbient(Bundle ambientDetails) {
        super.onEnterAmbient(ambientDetails);
        mCaloriesFragment.enterAmbient();
        mPager.setCurrentItem(0, 0);
        updateDisplay();
    }

    @Override
    public void onUpdateAmbient() {
        super.onUpdateAmbient();
        updateDisplay();
    }

    @Override
    public void onExitAmbient() {
        mCaloriesFragment.exitAmbient();
        updateDisplay();
        super.onExitAmbient();
    }

    private void updateDisplay() {
        if (isAmbient()) {
            mContainerView.setBackgroundColor(getResources().getColor(android.R.color.black));
        } else {
            mContainerView.setBackgroundColor(getResources().getColor(R.color.background));
        }
    }

    @Override
    public void onActionClicked(String label) {
        if(label.equals("Add Meal")) {
            startActivity(new Intent(this, AddMealActivity.class));
        }
    }
}
