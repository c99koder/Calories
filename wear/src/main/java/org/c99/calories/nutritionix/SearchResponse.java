package org.c99.calories.nutritionix;

import java.io.Serializable;
import java.util.List;

public class SearchResponse implements Serializable {
    private static final long serialVersionUID = 1L;
    public int total_hits;
    public float max_score;
    public List<SearchResult> hits;
}
