package com.foogaro.services;

import ai.djl.inference.Predictor;
import ai.djl.modality.cv.Image;
import ai.djl.modality.cv.ImageFactory;
import ai.djl.modality.cv.output.BoundingBox;
import ai.djl.modality.cv.output.DetectedObjects;
import ai.djl.modality.cv.output.Rectangle;
import ai.djl.repository.zoo.Criteria;
import ai.djl.repository.zoo.ZooModel;
import ai.djl.translate.Pipeline;
import ai.djl.translate.TranslateException;
import ai.djl.translate.Translator;
import com.foogaro.dtos.DetectedFaces;
import com.foogaro.dtos.ImageData;
import com.foogaro.dtos.ImageData$;
import com.foogaro.repositories.ImageDataRepository;
import com.redis.om.spring.search.stream.EntityStream;
import com.redis.om.spring.search.stream.SearchStream;
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

import static com.redis.om.spring.util.ObjectUtils.floatArrayToByteArray;

@Service
public class BestOfMatchService {

    private Logger logger = LoggerFactory.getLogger(BestOfMatchService.class);

    @Autowired
    private ImageDataRepository imageDataRepository;

    @Autowired
    private EntityStream entityStream;

    @Autowired
    public ZooModel<Image, float[]> faceEmbeddingModel;

    private static int K = 6;

    public ImageData match(byte[] image)  {

        try (Predictor<Image, float[]> predictor = faceEmbeddingModel.newPredictor()) {
            ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(image);
            Image img = ImageFactory.getInstance().fromInputStream(byteArrayInputStream);
            float[] embedding = predictor.predict(img);
            System.out.println(Arrays.toString(embedding));
            System.out.println(embedding.length);
            byte[] embeddingAsByteArray = floatArrayToByteArray(embedding);
            System.out.println(embeddingAsByteArray.length);

            SearchStream<ImageData> stream = entityStream.of(ImageData.class);
            List<Pair<ImageData,Double>> matchWithScore = stream //
                    .filter(ImageData$.IMAGE_EMBEDDING.knn(K, embeddingAsByteArray)) //
//                    .sorted(ImageData$._IMAGE_EMBEDDING_SCORE) //
                    .sorted(ImageData$.SCORE) //
                    .limit(1) //
                    .map(Fields.of(ImageData$._THIS, ImageData$.SCORE)) //
//                    .findFirst();
                    .collect(Collectors.toList());

            Optional<ImageData> match = matchWithScore.stream().map(Pair::getFirst).findFirst();
            Optional<Double> score = matchWithScore.stream().map(Pair::getSecond).map(d -> 100.0 * (1 - d/2)).findFirst();

            if (match.isPresent()) {
                ImageData imageData = match.get();
                imageData.setScore(score.get());
                logger.info(imageData.toString());

                return imageData;
            }
            return null;
        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (TranslateException e) {
            throw new RuntimeException(e);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

    }

}
