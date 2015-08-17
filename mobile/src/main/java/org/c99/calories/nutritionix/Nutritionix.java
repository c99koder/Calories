package org.c99.calories.nutritionix;

import retrofit.RestAdapter;

/**
 * Created by sam on 8/15/15.
 */
public class Nutritionix {
    private static RestAdapter adapter;
    private static NutritionixInterface nutritionixInterface;

    public static RestAdapter getAdapter() {
        if(adapter == null) {
            adapter = new RestAdapter.Builder()
                    .setEndpoint("https://api.nutritionix.com/v1_1/")
                    .build();
        }
        return adapter;
    }

    public static NutritionixInterface getInterface() {
        if(nutritionixInterface == null)
            nutritionixInterface = getAdapter().create(NutritionixInterface.class);
        return nutritionixInterface;
    }
}
