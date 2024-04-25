package com.foogaro;

import ai.djl.Application;
import ai.djl.MalformedModelException;
import ai.djl.ModelException;
import ai.djl.inference.Predictor;
import ai.djl.modality.cv.BufferedImageFactory;
import ai.djl.modality.cv.Image;
import ai.djl.modality.cv.ImageFactory;
import ai.djl.modality.cv.output.BoundingBox;
import ai.djl.modality.cv.output.DetectedObjects;
import ai.djl.modality.cv.output.Rectangle;
import ai.djl.modality.cv.transform.Normalize;
import ai.djl.modality.cv.transform.ToTensor;
import ai.djl.ndarray.NDArray;
import ai.djl.ndarray.NDList;
import ai.djl.repository.zoo.Criteria;
import ai.djl.repository.zoo.ModelNotFoundException;
import ai.djl.repository.zoo.ModelZoo;
import ai.djl.repository.zoo.ZooModel;
import ai.djl.training.util.ProgressBar;
import ai.djl.translate.*;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;

public class FaceDetector {
    public static void main(String[] args) throws IOException, ModelException, TranslateException {

        ModelZoo.listModelZoo().forEach(zoo -> {
            System.out.println("Model Zoo: " + zoo.getGroupId());
            zoo.getSupportedEngines().forEach(s -> {
                System.out.println("\tModel Zoo Supported Engine: " + s);
            });
            zoo.getModelLoaders().forEach(model -> {
                System.out.println("\tModel Application: " + model.getApplication() + ", Artifact: " + model.getArtifactId());
            });
        });


        float[] feature = new FaceDetector().detectFaces();
        if (feature != null) {
            System.out.println(Arrays.toString(feature));
        }

    }

    public void detectedObjects() throws IOException {

//        String imagePath = "/Users/foogaro/projects/GITHUB/look-alike/images/zoom-colleagues.png";
//        String outputImagePath = "/Users/foogaro/projects/GITHUB/look-alike/images/detected-zoom-colleagues.png";
        String imagePath = "/Users/foogaro/projects/GITHUB/look-alike/images/selfie-with-attendants.jpg";
        String outputImagePath = "/Users/foogaro/projects/GITHUB/look-alike/images/detected-selfie-with-attendants.jpg";

        Image img = BufferedImageFactory.getInstance().fromFile(Paths.get(imagePath));


        Criteria<Image, DetectedObjects> criteria = Criteria.builder()
                .optApplication(Application.CV.OBJECT_DETECTION)
                .setTypes(Image.class, DetectedObjects.class)
//                .optFilter("backbone", "resnet50")
//                .optFilter("size", "300")
//                .optFilter("flavor", "v2")
                .optEngine("PyTorch")
                .optArtifactId("yolov8n") //merges people into one square
                .optArtifactId("yolov5s") //detect single people and squared them accordingly
                .optArtifactId("ssd")
                .optProgress(new ProgressBar())
                .build();

        try (ZooModel<Image, DetectedObjects> model = ModelZoo.loadModel(criteria);
             Predictor<Image, DetectedObjects> predictor = model.newPredictor()) {
            DetectedObjects detections = predictor.predict(img);
            System.out.println("Detected objects: " + detections);

            // Draw the detections on the image
            Image newImage = drawBoundingBoxes(img, detections);

            // Save the image with detections
            try (FileOutputStream fos = new FileOutputStream(outputImagePath)) {
                newImage.save(fos, "jpg");
            }
            System.out.println("Detection results saved to " + outputImagePath);
        } catch (ModelNotFoundException e) {
            throw new RuntimeException(e);
        } catch (MalformedModelException e) {
            throw new RuntimeException(e);
        } catch (TranslateException e) {
            throw new RuntimeException(e);
        }
    }

    public float[] detectFaces() throws IOException {

//        String imagePath = "/Users/foogaro/projects/GITHUB/look-alike/images/zoom-colleagues.png";
//        String outputImagePath = "/Users/foogaro/projects/GITHUB/look-alike/images/detected-zoom-colleagues.png";
        String imagePath = "/Users/foogaro/projects/GITHUB/look-alike/images/selfie-with-attendants.jpg";
        String outputImagePath = "/Users/foogaro/projects/GITHUB/look-alike/images/detected-selfie-with-attendants.jpg";

        Image img = BufferedImageFactory.getInstance().fromFile(Paths.get(imagePath));


        img.getWrappedImage();
        Criteria<Image, float[]> criteria =
                Criteria.builder()
                        .setTypes(Image.class, float[].class)
                        .optModelUrls(
                                "https://resources.djl.ai/test-models/pytorch/face_feature.zip")
                        .optModelName("face_feature") // specify model file prefix
                        .optTranslator(new FaceFeatureTranslator())
                        .optProgress(new ProgressBar())
                        .optEngine("PyTorch") // Use PyTorch engine
                        .build();

        try (ZooModel<Image, float[]> model = criteria.loadModel()) {
            Predictor<Image, float[]> predictor = model.newPredictor();
            return predictor.predict(img);
        } catch (ModelNotFoundException e) {
            throw new RuntimeException(e);
        } catch (MalformedModelException | TranslateException e) {
            throw new RuntimeException(e);
        }
    }

    private static Image drawBoundingBoxes(Image img, DetectedObjects detections) {
        BufferedImage bufferedImage = (BufferedImage) img.getWrappedImage();
        Graphics2D g = bufferedImage.createGraphics();

        int stroke = 2;
        g.setStroke(new java.awt.BasicStroke(stroke));

        List<DetectedObjects.DetectedObject> list = detections.items();
        for (DetectedObjects.DetectedObject result : list) {
            BoundingBox box = result.getBoundingBox();
            Rectangle rectangle = box.getBounds();
            int x = (int) (rectangle.getX() * img.getWidth());
            int y = (int) (rectangle.getY() * img.getHeight());
            int width = (int) (rectangle.getWidth() * img.getWidth());
            int height = (int) (rectangle.getHeight() * img.getHeight());

            g.setPaint(Color.RED);
            g.drawRect(x, y, width, height);
            g.drawString(result.getClassName(), x, y);
        }
        g.dispose();

        return ImageFactory.getInstance().fromImage(bufferedImage);
    }


    private static final class FaceFeatureTranslator implements Translator<Image, float[]> {

        FaceFeatureTranslator() {}

        /** {@inheritDoc} */
        @Override
        public NDList processInput(TranslatorContext ctx, Image input) {
            NDArray array = input.toNDArray(ctx.getNDManager(), Image.Flag.COLOR);
            Pipeline pipeline = new Pipeline();
            pipeline
                    // .add(new Resize(160))
                    .add(new ToTensor())
                    .add(
                            new Normalize(
                                    new float[] {127.5f / 255.0f, 127.5f / 255.0f, 127.5f / 255.0f},
                                    new float[] {
                                            128.0f / 255.0f, 128.0f / 255.0f, 128.0f / 255.0f
                                    }));

            return pipeline.transform(new NDList(array));
        }

        /** {@inheritDoc} */
        @Override
        public float[] processOutput(TranslatorContext ctx, NDList list) {
            NDList result = new NDList();
            long numOutputs = list.singletonOrThrow().getShape().get(0);
            for (int i = 0; i < numOutputs; i++) {
                result.add(list.singletonOrThrow().get(i));
            }
            float[][] embeddings =
                    result.stream().map(NDArray::toFloatArray).toArray(float[][]::new);
            float[] feature = new float[embeddings.length];
            for (int i = 0; i < embeddings.length; i++) {
                feature[i] = embeddings[i][0];
            }
            return feature;
        }
    }
}
