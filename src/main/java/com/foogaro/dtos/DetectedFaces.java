package com.foogaro.dtos;

import com.redis.om.spring.annotations.Document;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class DetectedFaces implements Serializable {
    private List<CapturedImageData> capturedImageDataList;

    public DetectedFaces() {
    }

    public DetectedFaces(List<CapturedImageData> capturedImageDataList) {
        this.capturedImageDataList = capturedImageDataList;
    }

    public int getNumberOfFaces() {
        return capturedImageDataList != null ? capturedImageDataList.size() : 0;
    }


    public void addCapturedImageData(CapturedImageData capturedImageData)  {
        if (capturedImageDataList == null) this.capturedImageDataList = new ArrayList<>();
        capturedImageDataList.add(capturedImageData);
    }

    public List<CapturedImageData> getCapturedImageDataList() {
        return capturedImageDataList;
    }
}
