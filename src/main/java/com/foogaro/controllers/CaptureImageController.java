package com.foogaro.controllers;

import ai.djl.modality.cv.Image;
import ai.djl.modality.cv.ImageFactory;
import com.foogaro.dtos.BestOfMatch;
import com.foogaro.dtos.CapturedImageData;
import com.foogaro.dtos.DetectedFaces;
import com.foogaro.dtos.ImageData;
import com.foogaro.repositories.CapturedImageDataRepository;
import com.foogaro.services.BestOfMatchService;
import com.foogaro.services.FaceDetection;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Base64;

@RestController
@RequestMapping("/api")
public class CaptureImageController {

    @Autowired
    private FaceDetection faceDetection;
    @Autowired
    private BestOfMatchService bestOfMatchService;
    @Autowired
    private CapturedImageDataRepository capturedImageDataRepository;

    @CrossOrigin
    @PostMapping("/capture")
    public ResponseEntity<?> captureImage(@RequestBody CapturedImageData capturedImageData) {
        String base64Image = capturedImageData.getImage().split(",")[1];
        byte[] imageBytes = java.util.Base64.getDecoder().decode(base64Image);

        CapturedImageData capturedImageDataWithFaces = faceDetection.detectFaces(imageBytes);
        capturedImageDataRepository.save(capturedImageDataWithFaces);

        return ResponseEntity.ok(capturedImageDataWithFaces);
    }

    @CrossOrigin
    @GetMapping(value = "/top1", produces = MediaType.IMAGE_JPEG_VALUE)
    public ResponseEntity<?> top1() {
        return top("1");
    }

    @CrossOrigin
    @GetMapping(value = "/top2", produces = MediaType.IMAGE_JPEG_VALUE)
    public ResponseEntity<?> top2() {
        return top("2");
    }

    @CrossOrigin
    @GetMapping(value = "/top3", produces = MediaType.IMAGE_JPEG_VALUE)
    public ResponseEntity<?> top3() {
        return top("3");
    }

    public ResponseEntity<?> top(String id) {

        BestOfMatch bestOfMatch = new BestOfMatch();

        CapturedImageData capturedImageData = capturedImageDataRepository.findById("foo-"+id).get();
        ImageData match = bestOfMatchService.match(Base64.getDecoder().decode(capturedImageData.getImage()));
        bestOfMatch.addBestOfMatch(match);
        byte[] imageBytes = java.util.Base64.getDecoder().decode(match.getBase64Image());
        Image img = null;
        try {
            img = ImageFactory.getInstance().fromInputStream(new ByteArrayInputStream(imageBytes));
            img.save(Files.newOutputStream(Paths.get("/Users/foogaro/projects/GITHUB/look-alike/images/top-" + id + ".jpg")),"png");
            System.out.println("Saved: /Users/foogaro/projects/GITHUB/look-alike/images/top-" + id + ".jpg");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        String base64 = match.getBase64Image();
        byte[] decodeBase64 = Base64.getDecoder().decode(base64);
        return ResponseEntity.ok(decodeBase64);
    }

}
