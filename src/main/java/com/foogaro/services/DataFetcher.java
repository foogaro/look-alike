package com.foogaro.services;

import com.foogaro.dtos.ImageData;
import com.foogaro.repositories.ImageDataRepository;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Base64;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Service
public class DataFetcher {

    private final String SRC_URL = "https://datasets-server.huggingface.co/rows?dataset=ashraq%2Ftmdb-people-image&config=default&split=train";
    private int OFFSET = 0;
    private int LENGTH = 100;
    private int MAXROW = 1;
    private final String QUERY_QM = "?";
    private final String QUERY_AND = "&";
    private final String QUERY_EQUAL = "=";
    private final String QUERY_OFFSET = "offset";
    private final String QUERY_LENGTH = "length";

    private int loaded = Integer.MIN_VALUE;
    private int num_rows_total = Integer.MAX_VALUE;

    @Autowired
    private ImageDataRepository imageDataRepository;

    public void fetchData() {

        while (OFFSET <= MAXROW) {
            OFFSET += fetchData(OFFSET, LENGTH);
        }
    }

    public void fetchData(int maxRows) {

        while (OFFSET <= maxRows) {
            OFFSET += fetchData(OFFSET, LENGTH);
        }
    }

    public int fetchData(int offset, int length) {
        int counter = 0;
        try {
            ExecutorService executor = Executors.newFixedThreadPool(10);
            StringBuilder bURL = new StringBuilder(SRC_URL);
            bURL.append(QUERY_AND).append(QUERY_OFFSET).append(QUERY_EQUAL).append(offset).append(QUERY_AND).append(QUERY_LENGTH).append(QUERY_EQUAL).append(length);
            System.out.println(bURL);

            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(bURL.toString()))
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            JSONObject jsonObject = new JSONObject(response.body());
            JSONArray rows = jsonObject.getJSONArray("rows");
            MAXROW = jsonObject.getInt("num_rows_total");
            int num_rows_per_page = jsonObject.getInt("num_rows_per_page");


            for (int i = 0; i < rows.length(); i++) {
                int index = i;
                executor.submit(() -> {
                    try {
                        JSONObject row = rows.getJSONObject(index).getJSONObject("row");
                        String name = row.getString("name");
                        int id = row.getInt("id");
                        JSONObject image = row.getJSONObject("image");
                        String src = image.getString("src");
                        int height = image.getInt("height");
                        int width = image.getInt("width");

                        ImageData imageData = new ImageData();
                        imageData.setId(id+"");
                        imageData.setName(name);
                        imageData.setImagePath(src);
                        imageData.setHeight(height);
                        imageData.setWidth(width);
                        imageData.setScore(0);
                        imageData.setBase64Image(encodeImageToBase64(src));

                        imageDataRepository.save(imageData);

                        System.out.println("Data stored for " + name);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                });
                counter++;
            }

            executor.shutdown();
            while (!executor.isTerminated()) {
                // wait for all tasks to finish
            }
        } catch (Exception e) {
            e.printStackTrace();
            counter = Integer.MAX_VALUE;
        } finally {
            return counter;
        }
    }

    private String encodeImageToBase64(String imageUrl) {
        try {
            ByteArrayOutputStream os = getByteArrayOutputStream(imageUrl);

            byte[] imageBytes = os.toByteArray();
            String base64Image = Base64.getEncoder().encodeToString(imageBytes);

            return base64Image;
        } catch (Exception e) {
            e.printStackTrace();
            return imageUrl;
        }
    }

    private ByteArrayOutputStream getByteArrayOutputStream(String imageUrl) throws IOException {
        URL url = new URL(imageUrl);
        InputStream is = url.openStream();
        ByteArrayOutputStream os = new ByteArrayOutputStream();

        byte[] byteBuffer = new byte[4096];
        int bytesRead;

        while ((bytesRead = is.read(byteBuffer)) != -1) {
            os.write(byteBuffer, 0, bytesRead);
        }

        is.close();
        os.close();
        return os;
    }
}
