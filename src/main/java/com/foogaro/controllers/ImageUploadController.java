package com.foogaro.controllers;

import com.foogaro.dtos.DetectedFaces;
import com.foogaro.dtos.ImageData;
import com.foogaro.repositories.ImageDataRepository;
import com.foogaro.services.FaceDetection;
import com.foogaro.services.FaceDetectionV3;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.CrossOrigin;

@RestController
@RequestMapping("/api")
public class ImageUploadController {

    @Autowired
    private FaceDetectionV3 faceDetection;
    @Autowired
    private ImageDataRepository imageDataRepository;

    @CrossOrigin // Enable CORS if your frontend is served from a different port during development
    @PostMapping("/upload")
    public ResponseEntity<?> uploadImage(@RequestBody ImageData imageData) {
        String base64Image = imageData.getImage().split(",")[1];
        byte[] imageBytes = java.util.Base64.getDecoder().decode(base64Image);

        DetectedFaces detectedFaces = faceDetection.detectFaces(imageBytes);

        //imageDataRepository.save(detectedFaces.getAllDetectedFaces());

        return ResponseEntity.ok(detectedFaces);
    }
}
