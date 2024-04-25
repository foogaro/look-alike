package com.foogaro.services;

import ai.djl.MalformedModelException;
import ai.djl.inference.Predictor;
import ai.djl.modality.cv.BufferedImageFactory;
import ai.djl.modality.cv.Image;
import ai.djl.modality.cv.ImageFactory;
import ai.djl.modality.cv.output.BoundingBox;
import ai.djl.modality.cv.output.DetectedObjects;
import ai.djl.modality.cv.output.Rectangle;
import ai.djl.repository.zoo.Criteria;
import ai.djl.repository.zoo.ModelNotFoundException;
import ai.djl.repository.zoo.ZooModel;
import ai.djl.training.util.ProgressBar;
import ai.djl.translate.TranslateException;
import com.foogaro.FaceDetectionTranslator;
import com.foogaro.dtos.CapturedImageData;
import com.foogaro.dtos.DetectedFaces;
import com.foogaro.dtos.ImageData;
import com.foogaro.repositories.CapturedImageDataRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import static org.springframework.util.FileCopyUtils.BUFFER_SIZE;

@Service
public class FaceDetection {


    @Autowired
    private CapturedImageDataRepository capturedImageDataRepository;

    public CapturedImageData detectFaces(byte[] image)  {
        Image img = null;
        try (ZooModel<Image, DetectedObjects> model = criteria().loadModel();
             Predictor<Image, DetectedObjects> predictor = model.newPredictor()) {
            img = ImageFactory.getInstance().fromInputStream(new ByteArrayInputStream(image));

            DetectedObjects detection = predictor.predict(img);

            List<DetectedObjects.DetectedObject> faces = new ArrayList<>(detection.items());
            faces.sort(Comparator.comparingDouble(DetectedObjects.DetectedObject::getProbability).reversed());
            //faces = faces.subList(0, Math.min(Integer.MAX_VALUE, faces.size()));
            faces = faces.subList(0, Math.min(3, faces.size()));

            BufferedImage bufferedImage = (BufferedImage) img.getWrappedImage();
            AtomicInteger i = new AtomicInteger();
            faces.forEach(face -> {
                Rectangle rect = face.getBoundingBox().getBounds();
                int x = (int) (rect.getX() * bufferedImage.getWidth());
                int y = (int) (rect.getY() * bufferedImage.getHeight());
                int width = (int) (rect.getWidth() * bufferedImage.getWidth());
                int height = (int) (rect.getHeight() * bufferedImage.getHeight());

                BufferedImage subImage = bufferedImage.getSubimage(x, y, width, height);
                Image subImageDjl = BufferedImageFactory.getInstance().fromImage(subImage);

                try {
                    subImageDjl.save(Files.newOutputStream(Paths.get("/Users/foogaro/projects/GITHUB/look-alike/images/detected-"+i.incrementAndGet()+".jpg")),"png");
                    System.out.println("Saved: /Users/foogaro/projects/GITHUB/look-alike/images/detected-"+i);

                    CapturedImageData capturedImageData = new CapturedImageData();
                    capturedImageData.setId("foo-"+i.get());
                    capturedImageData.setImage(imageToBase64(subImage));
                    capturedImageData.setHeight(height);
                    capturedImageData.setWidth(width);
                    capturedImageDataRepository.save(capturedImageData);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            });

            Image newImage = drawBoundingBoxesNew(img, faces);
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            newImage.save(outputStream, "png");

            CapturedImageData capturedImageData = new CapturedImageData();
            capturedImageData.setId("foo");
            //capturedImageData.setImage(Base64.getEncoder().encodeToString(outputStream.toByteArray()));
            capturedImageData.setImage(imageToBase64((BufferedImage) newImage.getWrappedImage()));
            return capturedImageData;
        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (TranslateException e) {
            throw new RuntimeException(e);
        } catch (ModelNotFoundException e) {
            throw new RuntimeException(e);
        } catch (MalformedModelException e) {
            throw new RuntimeException(e);
        }
    }

    private FaceDetectionTranslator faceDetectionTranslator() {
        double confThresh = 0.85f;
        double nmsThresh = 0.45f;
        double[] variance = {0.1f, 0.2f};
        int topK = 5000;
        int[][] scales = {{16, 32}, {64, 128}, {256, 512}};
        int[] steps = {8, 16, 32};
        return new FaceDetectionTranslator(confThresh, nmsThresh, variance, topK, scales, steps);
    }

    private Criteria<Image, DetectedObjects> criteria() {
        return Criteria.builder()
                .setTypes(Image.class, DetectedObjects.class)
                .optModelUrls("https://resources.djl.ai/test-models/pytorch/retinaface.zip")
                // Load model from local file, e.g:
                .optModelName("retinaface") // specify model file prefix
                .optTranslator(faceDetectionTranslator())
                .optProgress(new ProgressBar())
                .optEngine("PyTorch") // Use PyTorch engine
                .build();
    }

    private Image drawBoundingBoxesNew(Image image, List<DetectedObjects.DetectedObject> faces) {
        BufferedImage bufferedImage = (BufferedImage) image.getWrappedImage();
        Graphics2D g = (Graphics2D) bufferedImage.getGraphics();
        int stroke = 2;
        g.setStroke(new BasicStroke(stroke));
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        int imageWidth = image.getWidth();
        int imageHeight = image.getHeight();

        int k = 10;
        Map<String, Integer> classNumberTable = new ConcurrentHashMap<>();
        for (DetectedObjects.DetectedObject result : faces) {
            String className = result.getClassName();
            BoundingBox box = result.getBoundingBox();
            if (classNumberTable.containsKey(className)) {
                g.setPaint(new Color(classNumberTable.get(className)));
            } else {
                g.setPaint(new Color(k));
                classNumberTable.put(className, k);
                k = (k + 100) % 255;
            }

            Rectangle rectangle = box.getBounds();
            int x = (int) (rectangle.getX() * imageWidth);
            int y = (int) (rectangle.getY() * imageHeight);
            g.drawRect(
                    x,
                    y,
                    (int) (rectangle.getWidth() * imageWidth),
                    (int) (rectangle.getHeight() * imageHeight));
            drawText(g, className, x, y, stroke, 4);
        }
        g.dispose();
        bufferedImage.flush();
        return ImageFactory.getInstance().fromImage(bufferedImage);
    }

    private void drawText(Graphics2D g, String text, int x, int y, int stroke, int padding) {
        FontMetrics metrics = g.getFontMetrics();
        x += stroke / 2;
        y += stroke / 2;
        int width = metrics.stringWidth(text) + padding * 2 - stroke / 2;
        int height = metrics.getHeight() + metrics.getDescent();
        int ascent = metrics.getAscent();
        java.awt.Rectangle background = new java.awt.Rectangle(x, y, width, height);
        g.fill(background);
        g.setPaint(Color.WHITE);
        g.drawString(text, x + padding, y + ascent);
    }

    private Image drawBoundingBoxes(Image img, List<DetectedObjects.DetectedObject> faces) {
        BufferedImage bufferedImage = (BufferedImage) img.getWrappedImage();
        Graphics2D g = bufferedImage.createGraphics();

        g.setColor(Color.RED);
        g.setStroke(new BasicStroke(2));

        for (DetectedObjects.DetectedObject face : faces) {
            Rectangle rect = face.getBoundingBox().getBounds();
            g.drawRect((int) rect.getX(), (int) rect.getY(), (int) rect.getWidth(), (int) rect.getHeight());
            g.drawString(String.format("%.2f", face.getProbability()), (int) rect.getX(), (int) rect.getY() - 5);
        }

        g.dispose();
        bufferedImage.flush();
        return ImageFactory.getInstance().fromImage(bufferedImage);
    }

    private void saveImage(Image image, String fileName) throws IOException {
        File output = new File(fileName);
        ImageIO.write((BufferedImage) image.getWrappedImage(), "jpg", output);
    }

    private String imageToBase64(BufferedImage image) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream(BUFFER_SIZE);
        ImageIO.write(image, "png", out);
        return Base64.getEncoder().encodeToString(out.toByteArray());
    }
}
