package com.foogaro;

import ai.djl.ModelException;
import ai.djl.inference.Predictor;
import ai.djl.modality.cv.BufferedImageFactory;
import ai.djl.modality.cv.Image;
import ai.djl.modality.cv.ImageFactory;
import ai.djl.modality.cv.output.DetectedObjects;
import ai.djl.modality.cv.output.Rectangle;
import ai.djl.repository.zoo.Criteria;
import ai.djl.repository.zoo.ZooModel;
import ai.djl.training.util.ProgressBar;
import ai.djl.translate.TranslateException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.image.BufferedImage;
import java.io.FileOutputStream;
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
public final class RetinaFaceDetection {

    private static final Logger logger = LoggerFactory.getLogger(RetinaFaceDetection.class);

    private RetinaFaceDetection() {}

    public static void main(String[] args) throws IOException, ModelException, TranslateException {
        DetectedObjects detection = RetinaFaceDetection.predict();
        logger.info("{}", detection);
    }

    public static DetectedObjects predict() throws IOException, ModelException, TranslateException {

        String imagePath = "/Users/foogaro/projects/GITHUB/look-alike/images/selfie-with-attendants.jpg";
        String outputImagePath = "/Users/foogaro/projects/GITHUB/look-alike/images/detected-selfie-with-attendants.jpg";

        Image img = ImageFactory.getInstance().fromFile(Paths.get(imagePath));

        double confThresh = 0.85f;
        double nmsThresh = 0.45f;
        double[] variance = {0.1f, 0.2f};
        int topK = 5000;
        int[][] scales = {{16, 32}, {64, 128}, {256, 512}};
        int[] steps = {8, 16, 32};
        FaceDetectionTranslator translator =
                new FaceDetectionTranslator(confThresh, nmsThresh, variance, topK, scales, steps);

        Criteria<Image, DetectedObjects> criteria =
                Criteria.builder()
                        .setTypes(Image.class, DetectedObjects.class)
                        .optModelUrls("https://resources.djl.ai/test-models/pytorch/retinaface.zip")
                        // Load model from local file, e.g:
                        .optModelName("retinaface") // specify model file prefix
                        .optTranslator(translator)
                        .optProgress(new ProgressBar())
                        .optEngine("PyTorch") // Use PyTorch engine
                        .build();

        try (ZooModel<Image, DetectedObjects> model = criteria.loadModel();
             Predictor<Image, DetectedObjects> predictor = model.newPredictor()) {
            DetectedObjects detection = predictor.predict(img);

//            saveBoundingBoxImage(img, detection);
            img.drawBoundingBoxes(detection);

            img.save(Files.newOutputStream(Paths.get(outputImagePath)), "png");
            logger.info("Face detection result image has been saved in: {}", outputImagePath);

            BufferedImage bufferedImage = (BufferedImage) img.getWrappedImage();

            int numberOfObjects = detection.getNumberOfObjects();
            for (int i = 0; i < numberOfObjects; i++) {
                DetectedObjects.DetectedObject detectedObject = detection.item(i);
                Rectangle rect = detectedObject.getBoundingBox().getBounds();
                int x = (int) (rect.getX() * bufferedImage.getWidth());
                int y = (int) (rect.getY() * bufferedImage.getHeight());
                int width = (int) (rect.getWidth() * bufferedImage.getWidth());
                int height = (int) (rect.getHeight() * bufferedImage.getHeight());

                BufferedImage subImage = bufferedImage.getSubimage(x, y, width, height);
                Image subImageDjl = BufferedImageFactory.getInstance().fromImage(subImage);

                subImageDjl.save(Files.newOutputStream(Paths.get("/Users/foogaro/projects/GITHUB/look-alike/images/detected-"+i+".jpg")),"png");
                System.out.println("Saved: /Users/foogaro/projects/GITHUB/look-alike/images/detected-"+i);
            }

            return detection;
        }
    }

    private static void saveBoundingBoxImage(Image img, DetectedObjects detection)
            throws IOException {
        Path outputDir = Paths.get("build/output");
        Files.createDirectories(outputDir);

        img.drawBoundingBoxes(detection);

        Path imagePath = outputDir.resolve("retinaface_detected.png");
        img.save(Files.newOutputStream(imagePath), "png");
        logger.info("Face detection result image has been saved in: {}", imagePath);
    }
}