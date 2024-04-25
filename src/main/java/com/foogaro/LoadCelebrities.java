package com.foogaro;

import com.foogaro.services.DataFetcher;
import com.redis.om.spring.annotations.EnableRedisEnhancedRepositories;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class LoadCelebrities {

    @Autowired
    private DataFetcher dataFetcher;

    @Bean
    CommandLineRunner loadTestData() {
        return args -> {
            //imageDataRepository.deleteAll();

            //dataFetcher.fetchData();
            //dataFetcher.fetchData(0,100);

        };
    }

    public static void main(String[] args) {
        SpringApplication.run(LoadCelebrities.class, args);
    }

}
