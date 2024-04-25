package com.foogaro.dtos;

import java.util.ArrayList;
import java.util.List;

public class BestOfMatch {
    private List<ImageData> bestOfMatches = new ArrayList<>();

    public BestOfMatch() {
    }

    public void addBestOfMatch(ImageData imageData) {
        bestOfMatches.add(imageData);
    }

    public List<ImageData> getBestOfMatches() {
        return bestOfMatches;
    }
}
