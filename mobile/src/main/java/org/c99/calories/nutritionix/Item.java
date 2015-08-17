package org.c99.calories.nutritionix;

import java.io.Serializable;

public class Item implements Serializable {
    private static final long serialVersionUID = 1L;
    public String item_id;
    public String item_name;
    public String brand_id;
    public String brand_name;
    public String item_description;
    public float nf_calories;
    public float nf_calories_from_fat;
    public float nf_total_fat;
    public float nf_saturated_fat;
    public float nf_trans_fatty_acid;
    public float nf_polyunsaturated_fat;
    public float nf_monounsaturated_fat;
    public float nf_cholesterol;
    public float nf_sodium;
    public float nf_total_carbohydrate;
    public float nf_dietary_fiber;
    public float nf_sugars;
    public float nf_protein;
    public float nf_vitamin_a_dv;
    public float nf_vitamin_c_dv;
    public float nf_calcium_dv;
    public float nf_iron_dv;
    public float nf_refuse_pct;
    public float nf_servings_per_container;
    public float nf_serving_size_qty;
    public String nf_serving_size_unit;
    public float nf_serving_weight_grams;

    public float amount;
}
