package com.foogaro;

import ai.djl.ModelException;
import ai.djl.inference.Predictor;
import ai.djl.modality.cv.BufferedImageFactory;
import ai.djl.modality.cv.Image;
import ai.djl.modality.cv.ImageFactory;
import ai.djl.modality.cv.output.DetectedObjects;
import ai.djl.modality.cv.output.Rectangle;
//import ai.djl.paddlepaddle.zoo.cv.objectdetection.BoundFinder;
import ai.djl.repository.zoo.Criteria;
import ai.djl.repository.zoo.ZooModel;
import ai.djl.training.util.ProgressBar;
import ai.djl.translate.TranslateException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * An example of inference using a face detection model.
 *
 * <p>See this <a
 * href="https://github.com/deepjavalibrary/djl/blob/master/examples/docs/face_detection.md">doc</a>
 * for information about this example.
 */
public final class LightFaceDetection {

    private static final Logger logger = LoggerFactory.getLogger(LightFaceDetection.class);

    private LightFaceDetection() {}

    public static void main(String[] args) throws IOException, ModelException, TranslateException {
        DetectedObjects detection = LightFaceDetection.predict();
        logger.info("{}", detection);
    }

    public static DetectedObjects predict() throws IOException, ModelException, TranslateException {
        String imagePath = "/Users/foogaro/projects/GITHUB/look-alike/images/zoom-colleagues.png";

        Image img = ImageFactory.getInstance().fromFile(Paths.get(imagePath));

        double confThresh = 0.85f;
        double nmsThresh = 0.45f;
        double[] variance = {0.1f, 0.2f};
        int topK = 5000;
        int[][] scales = {{8, 16, 24}, {32, 48}, {64, 96}, {128, 192, 256}};
        int[] steps = {8, 16, 32, 64};

        FaceDetectionTranslator translator =
                new FaceDetectionTranslator(confThresh, nmsThresh, variance, topK, scales, steps);

        Criteria<Image, DetectedObjects> criteria =
                Criteria.builder()
                        .setTypes(Image.class, DetectedObjects.class)
                        .optModelUrls("https://resources.djl.ai/test-models/pytorch/ultranet.zip")
                        .optTranslator(translator)
                        .optProgress(new ProgressBar())
                        .optEngine("PyTorch") // Use PyTorch engine
                        .build();

        try (ZooModel<Image, DetectedObjects> model = criteria.loadModel()) {
            try (Predictor<Image, DetectedObjects> predictor = model.newPredictor()) {
                DetectedObjects detection = predictor.predict(img);
                saveBoundingBoxImage(img, detection);
                return detection;
            }
        }
    }

    private static void saveBoundingBoxImage(Image img, DetectedObjects detection)
            throws IOException {

        String outputImagePath = "/Users/foogaro/projects/GITHUB/look-alike/images/detected-zoom-colleagues.png";

        BufferedImage duplicate = (BufferedImage) img.getWrappedImage();

        for (int i = 0; i < detection.getNumberOfObjects(); i++) {
            DetectedObjects.DetectedObject dto = detection.item(i);
            Rectangle rect = dto.getBoundingBox().getBounds();
            int x = (int) (rect.getX() * img.getWidth());
            int y = (int) (rect.getY() * img.getHeight());
            int width = (int) (rect.getWidth() * img.getWidth());
            int height = (int) (rect.getHeight() * img.getHeight());

            BufferedImage subImage = duplicate.getSubimage(x, y, width, height);
            Image subImageDjl = BufferedImageFactory.getInstance().fromImage(subImage);

            try {
                subImageDjl.save(Files.newOutputStream(Paths.get("/Users/foogaro/projects/GITHUB/look-alike/images/detected-zoom-colleague-"+System.currentTimeMillis()+".jpg")),"png");
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        //duplicate.drawBoundingBoxes(detection);

//        BufferedImage bufferedImage = (BufferedImage) img.getWrappedImage();
//        int numberOfObjects = detection.getNumberOfObjects();
//        for (int i = 0; i < numberOfObjects; i++) {
//            DetectedObjects.DetectedObject detectedObject = detection.item(i);
//            Rectangle rect = detectedObject.getBoundingBox().getBounds();
//            int x = (int) (rect.getX() * bufferedImage.getWidth());
//            int y = (int) (rect.getY() * bufferedImage.getHeight());
//            int width = (int) (rect.getWidth() * bufferedImage.getWidth());
//            int height = (int) (rect.getHeight() * bufferedImage.getHeight());
//
//            BufferedImage subImage = bufferedImage.getSubimage(x, y, width, height);
//            Image subImageDjl = BufferedImageFactory.getInstance().fromImage(subImage);
//
//            subImageDjl.save(Files.newOutputStream(Paths.get("/Users/foogaro/projects/GITHUB/look-alike/images/detected-light-face-detection-"+i+".jpg")),"png");
//            System.out.println("Saved: /Users/foogaro/projects/GITHUB/look-alike/images/detected-"+i);
//        }

        //duplicate.save(Files.newOutputStream(Paths.get(outputImagePath)), "png");
        logger.info("Light Face detection result image has been saved in: {}", outputImagePath);
    }
}