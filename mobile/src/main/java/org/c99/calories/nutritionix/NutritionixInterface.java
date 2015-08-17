package org.c99.calories.nutritionix;

import retrofit.Callback;
import retrofit.http.GET;
import retrofit.http.Path;
import retrofit.http.Query;

/**
 * Created by sam on 8/15/15.
 */
public interface NutritionixInterface {
    @GET("/search/{query}?fields=*")
    void search(@Path("query") String query, @Query("appId") String appId, @Query("appKey") String appKey, Callback<SearchResponse> cb);
}
