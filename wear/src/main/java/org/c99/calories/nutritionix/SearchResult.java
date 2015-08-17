package org.c99.calories.nutritionix;

import java.io.Serializable;

public class SearchResult implements Serializable {
    private static final long serialVersionUID = 1L;
    public String _index;
    public String _type;
    public String _id;
    public float _score;
    public Item fields;
}
