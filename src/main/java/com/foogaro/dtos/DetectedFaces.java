package com.foogaro.dtos;

public class DetectedFaces {
    private int numberOfFaces;
    private ImageData allDetectedFaces;

    public DetectedFaces(int numberOfFaces, ImageData allDetectedFaces) {
        this.numberOfFaces = numberOfFaces;
        this.allDetectedFaces = allDetectedFaces;
    }

    public int getNumberOfFaces() {
        return numberOfFaces;
    }

    public void setNumberOfFaces(int numberOfFaces) {
        this.numberOfFaces = numberOfFaces;
    }

    public ImageData getAllDetectedFaces() {
        return allDetectedFaces;
    }

    public void setAllDetectedFaces(ImageData allDetectedFaces) {
        this.allDetectedFaces = allDetectedFaces;
    }
}
