package com.foogaro.dtos;

import com.redis.om.spring.DistanceMetric;
import com.redis.om.spring.VectorType;
import com.redis.om.spring.annotations.*;
import org.springframework.data.annotation.Id;
import redis.clients.jedis.search.schemafields.VectorField;

@Document()
public class ImageData {
    @Id
    private String id;
    @Indexed
    private String name;
//    @Vectorize(destination = "imageEmbedding", embeddingType = EmbeddingType.IMAGE)
    private String image;
    @Indexed
    private int height;
    @Indexed
    private int width;
    @Indexed(schemaFieldType = SchemaFieldType.VECTOR,
            algorithm = VectorField.VectorAlgorithm.HNSW,
            type = VectorType.FLOAT32,
            dimension = 512,
            distanceMetric = DistanceMetric.COSINE,
            initialCapacity = 10)
    private byte[] imageEmbedding;
    @Vectorize(destination = "imageEmbedding", embeddingType = EmbeddingType.FACE)
    private String imagePath;
    @Indexed
    private double score = 0;

    private String base64Image;

    public ImageData() {}

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getImage() {
        return image;
    }

    public void setImage(String image) {
        this.image = image;
    }

    public int getHeight() {
        return height;
    }

    public void setHeight(int height) {
        this.height = height;
    }

    public int getWidth() {
        return width;
    }

    public void setWidth(int width) {
        this.width = width;
    }

    public byte[] getImageEmbedding() {
        return imageEmbedding;
    }

    public void setImageEmbedding(byte[] imageEmbedding) {
        this.imageEmbedding = imageEmbedding;
    }

    public String getImagePath() {
        return imagePath;
    }

    public void setImagePath(String imagePath) {
        this.imagePath = imagePath;
    }

    public double getScore() {
        return score;
    }

    public void setScore(double score) {
        this.score = score;
    }

    public String getBase64Image() {
        return base64Image;
    }

    public void setBase64Image(String base64Image) {
        this.base64Image = base64Image;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ImageData imageData = (ImageData) o;
        return id.equals(imageData.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("ImageData{");
        sb.append("id='").append(id).append('\'');
        sb.append(", name='").append(name).append('\'');
        sb.append(", height=").append(height);
        sb.append(", width=").append(width);
        sb.append(", imagePath='").append(imagePath).append('\'');
        sb.append(", score=").append(score);
        sb.append('}');
        return sb.toString();
    }
}
