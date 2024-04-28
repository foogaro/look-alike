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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

@RestController
@RequestMapping("/api")
public class CaptureImageController {

    private static final Logger log = LoggerFactory.getLogger(CaptureImageController.class);
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

        capturedImageDataRepository.deleteAll();
        CapturedImageData capturedImageDataWithFaces = faceDetection.detectFaces(imageBytes);
//        capturedImageDataRepository.save(capturedImageDataWithFaces);

        return ResponseEntity.ok(capturedImageDataWithFaces);
    }

    @CrossOrigin
    @GetMapping(value = "/top1", produces = MediaType.IMAGE_JPEG_VALUE)
    public ResponseEntity<?> top1() { return top("com.foogaro.dtos.CapturedImageData:foo-1"); }

    @CrossOrigin
    @GetMapping(value = "/top2", produces = MediaType.IMAGE_JPEG_VALUE)
    public ResponseEntity<?> top2() {
        return top("com.foogaro.dtos.CapturedImageData:foo-2");
    }

    @CrossOrigin
    @GetMapping(value = "/top3", produces = MediaType.IMAGE_JPEG_VALUE)
    public ResponseEntity<?> top3() {
        return top("com.foogaro.dtos.CapturedImageData:foo-3");
    }

    public ResponseEntity<?> top(String id) {

//        CapturedImageData capturedImageData = capturedImageDataRepository.findById("com.foogaro.dtos.CapturedImageData:foo-"+id).get();
        CapturedImageData capturedImageData = capturedImageDataRepository.findById(id).get();
        ImageData match = bestOfMatchService.match(Base64.getDecoder().decode(capturedImageData.getImage()));
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

    @CrossOrigin
    @GetMapping(value = "/results")
    public ResponseEntity<?> results() {
        List<CapturedImageData> capturedImageDataList = capturedImageDataRepository.findAll();
        return ResponseEntity.ok(capturedImageDataList);
    }

    @CrossOrigin
    @GetMapping(value = "/look-alike/{id}")
    public ResponseEntity<?> lookAlike(@PathVariable String id) {
        log.info("CapturedImageData ID: " + id);
        CapturedImageData capturedImageData = capturedImageDataRepository.findById(id).get();
        //ImageData match = bestOfMatchService.match(Base64.getDecoder().decode(capturedImageData.getImage()));
        List<ImageData> matches = bestOfMatchService.matchAll(Base64.getDecoder().decode(capturedImageData.getImage()));
//        byte[] imageBytes = java.util.Base64.getDecoder().decode(match.getBase64Image());
//        Image img = null;
//        try {
//            img = ImageFactory.getInstance().fromInputStream(new ByteArrayInputStream(imageBytes));
//            img.save(Files.newOutputStream(Paths.get("/Users/foogaro/projects/GITHUB/look-alike/images/" + id + ".jpg")),"png");
//            System.out.println("Saved: /Users/foogaro/projects/GITHUB/look-alike/images/" + id + ".jpg");
//        } catch (IOException e) {
//            throw new RuntimeException(e);
//        }

        List<CapturedImageData> matchedList = new ArrayList<>();
        matches.forEach(match -> {
            CapturedImageData matched = new CapturedImageData();
            matched.setImage(match.getBase64Image());
            matched.setName(match.getName());
            matched.setId(match.getScore()+"");
            matched.setWidth(match.getWidth());
            matched.setHeight(match.getHeight());
            matchedList.add(matched);
        });
        return ResponseEntity.ok(matchedList);
//        String base64 = match.getBase64Image();
//        byte[] decodeBase64 = Base64.getDecoder().decode(base64);
//        return ResponseEntity.ok(decodeBase64);
    }

}
