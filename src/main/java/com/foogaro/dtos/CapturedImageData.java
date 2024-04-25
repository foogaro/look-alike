package com.foogaro.dtos;

import com.redis.om.spring.annotations.Document;
import com.redis.om.spring.annotations.Indexed;
import org.springframework.data.annotation.Id;

@Document()
public class CapturedImageData {
    @Id
    private String id;
    @Indexed
    private String name;
    private String image;
    @Indexed
    private int height;
    @Indexed
    private int width;

    public CapturedImageData() {}

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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        CapturedImageData imageData = (CapturedImageData) o;
        return id.equals(imageData.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }
}
