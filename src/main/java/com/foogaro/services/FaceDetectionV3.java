package com.foogaro.services;

import ai.djl.MalformedModelException;
import ai.djl.inference.Predictor;
import ai.djl.modality.cv.Image;
import ai.djl.modality.cv.ImageFactory;
import ai.djl.modality.cv.output.BoundingBox;
import ai.djl.modality.cv.output.DetectedObjects;
import ai.djl.modality.cv.output.Rectangle;
import ai.djl.repository.zoo.Criteria;
import ai.djl.repository.zoo.ModelNotFoundException;
import ai.djl.repository.zoo.ZooModel;
import ai.djl.translate.Pipeline;
import ai.djl.translate.TranslateException;
import ai.djl.translate.Translator;
import com.foogaro.dtos.DetectedFaces;
import com.foogaro.dtos.ImageData;
import com.foogaro.dtos.ImageData$;
import com.foogaro.repositories.ImageDataRepository;
import com.redis.om.spring.search.stream.EntityStream;
import com.redis.om.spring.tuple.Fields;
import com.redis.om.spring.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static com.redis.om.spring.util.ObjectUtils.floatArrayToByteArray;

@Service
public class FaceDetectionV3 {

    private Logger logger = LoggerFactory.getLogger(FaceDetectionV3.class);

    @Autowired
    private ZooModel<Image, DetectedObjects> faceDetectionModel;
    @Autowired
    private ZooModel<Image, float[]> faceEmbeddingModel;
    @Autowired
    private ImageDataRepository imageDataRepository;
    @Autowired
    private Translator<Image, DetectedObjects> faceDetectionTranslator;

    @Autowired
    private Criteria<Image, DetectedObjects> faceDetectionModelCriteria;


    @Autowired
    private Translator<Image, float[]> faceEmbeddingTranslator;

    @Autowired
    private Criteria<Image, float[]> faceEmbeddingModelCriteria;


    @Autowired
    private Pipeline defaultImagePipeline;

    public DetectedFaces detectFaces(byte[] image)  {
        try (Predictor<Image, DetectedObjects> faceDetector = faceDetectionModel.newPredictor();
             Predictor<Image, float[]> predictor = faceEmbeddingModel.newPredictor()) {

            Image img = ImageFactory.getInstance().fromInputStream(new ByteArrayInputStream(image));

            DetectedObjects detection = faceDetector.predict(img);

            AtomicInteger index = new AtomicInteger();

            //    Stream<DetectedObjects.DetectedObject> maybeFace = IntStream.range(0, detection.getNumberOfObjects())
            IntStream.range(0, detection.getNumberOfObjects())
                .mapToObj(i -> (DetectedObjects.DetectedObject)detection.item(i)) //
                .filter(detectedObject -> detectedObject.getClassName().equals("Face"))
                .forEach(face -> {
                    Image faceImg = getSubImage(img, face.getBoundingBox());
                    logger.info("😁️ :: Face Detected :: w → {}, h → {}", faceImg.getWidth(), faceImg.getHeight());

                    float[] embedding = null;
                    try {
                        embedding = predictor.predict(faceImg);
                    } catch (TranslateException e) {
                        throw new RuntimeException(e);
                    }
                    System.out.println(Arrays.toString(embedding));
                    System.out.println(embedding.length);
                    byte[] embeddingAsByteArray = floatArrayToByteArray(embedding);
                    System.out.println(embeddingAsByteArray.length);
                    ImageData imageData = new ImageData();
                    imageData.setId(System.currentTimeMillis()+"");
                    imageData.setName("detected-"+index.incrementAndGet());
                    imageData.setHeight(faceImg.getHeight());
                    imageData.setWidth(faceImg.getWidth());
                    imageData.setImageEmbedding(embeddingAsByteArray);

                    imageDataRepository.save(imageData);
                });
        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (TranslateException e) {
            throw new RuntimeException(e);
        }

        return null;
    }

    private Image getSubImage(Image img, BoundingBox box) {
        Rectangle rect = box.getBounds();
        double[] extended = extendRect(rect.getX(), rect.getY(), rect.getWidth(), rect.getHeight());
        int width = img.getWidth();
        int height = img.getHeight();
        int[] recovered = {
                (int) (extended[0] * width),
                (int) (extended[1] * height),
                (int) (extended[2] * width),
                (int) (extended[3] * height)
        };
        return img.getSubImage(recovered[0], recovered[1], recovered[2], recovered[3]);
    }

    private double[] extendRect(double xmin, double ymin, double width, double height) {
        double centerx = xmin + width / 2;
        double centery = ymin + height / 2;
        if (width > height) {
            width += height * 2.0;
            height *= 3.0;
        } else {
            height += width * 2.0;
            width *= 3.0;
        }
        double newX = centerx - width / 2 < 0 ? 0 : centerx - width / 2;
        double newY = centery - height / 2 < 0 ? 0 : centery - height / 2;
        double newWidth = newX + width > 1 ? 1 - newX : width;
        double newHeight = newY + height > 1 ? 1 - newY : height;
        return new double[] {newX, newY, newWidth, newHeight};
    }
}
