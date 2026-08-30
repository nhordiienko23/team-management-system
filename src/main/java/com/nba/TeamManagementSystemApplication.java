package com.nba;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;


import java.util.TimeZone;

@SpringBootApplication
@EnableCaching
public class TeamManagementSystemApplication {
    public static void main(String[] args) {
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
        SpringApplication.run(TeamManagementSystemApplication.class, args);
    }


}