package com.prarxbdha.jreview;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;

/** Starts the console application. There is no web server and no browser page. */
@SpringBootApplication
public class JReviewApplication {

    public static void main(String[] args) {
        SpringApplication app = new SpringApplication(JReviewApplication.class);
        app.setWebApplicationType(WebApplicationType.NONE);

        // The CLI is started explicitly so tests can load the context without
        // blocking on System.in.
        try (ConfigurableApplicationContext context = app.run(args)) {
            context.getBean(ReviewCli.class).run();
        }
    }
}
