package com.foogaro.services;

import ai.djl.inference.Predictor;
import ai.djl.modality.cv.Image;
import ai.djl.modality.cv.ImageFactory;
import ai.djl.repository.zoo.ZooModel;
import ai.djl.translate.TranslateException;
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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

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

    public ImageData bestMatch(byte[] image)  {
        return matchAll(image, 1).get(0);
    }

    public List<ImageData> bestMatches(byte[] image, int numberOfMatches)  {
        return matchAll(image, numberOfMatches);
    }

    private List<ImageData> matchAll(byte[] image, int limit)  {
        List<ImageData> imageDataList = new ArrayList<>();
        try (Predictor<Image, float[]> predictor = faceEmbeddingModel.newPredictor()) {
            ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(image);
            Image img = ImageFactory.getInstance().fromInputStream(byteArrayInputStream);
            float[] embedding = predictor.predict(img);
            logger.info("Embeddings["+embedding.length+"] for example image:");
            logger.info(Arrays.toString(embedding));
            byte[] embeddingAsByteArray = floatArrayToByteArray(embedding);
            logger.info("Embeddings byte array size: " + embeddingAsByteArray.length);

            SearchStream<ImageData> stream = entityStream.of(ImageData.class);
            List<Pair<ImageData,Double>> matchWithScore = stream
                    .filter(ImageData$.IMAGE_EMBEDDING.knn(K, embeddingAsByteArray))
                    .sorted(ImageData$._IMAGE_EMBEDDING_SCORE)
                    .limit(limit)
                    .map(Fields.of(ImageData$._THIS, ImageData$._IMAGE_EMBEDDING_SCORE))
                    .collect(Collectors.toList());

            for (Pair<ImageData,Double> pair : matchWithScore) {
                ImageData imageData = pair.getFirst();
                Double score = pair.getSecond();
                imageData.setScore(100.0 * (1 - score/2));
                imageDataList.add(imageData);
            }
            return imageDataList;

        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (TranslateException e) {
            throw new RuntimeException(e);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

}
