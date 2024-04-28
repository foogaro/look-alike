package com.foogaro;

import com.foogaro.controllers.CaptureImageController;
import com.foogaro.services.DataFetcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class LoadCelebrities {

    private static final Logger log = LoggerFactory.getLogger(LoadCelebrities.class);

    @Autowired
    private DataFetcher dataFetcher;

    @Bean
    CommandLineRunner loadTestData() {
        return args -> {
            log.info("Starting loading celebrities...");
            //imageDataRepository.deleteAll();

            //dataFetcher.fetchData();

            //dataFetcher.fetchData(20_000);
            log.info("...done loading celebrities!");
        };
    }

    public static void main(String[] args) {
        SpringApplication.run(LoadCelebrities.class, args);
    }

}
