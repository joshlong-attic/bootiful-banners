package com.example;

import lombok.Data;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;
import org.springframework.util.FileCopyUtils;

import java.io.*;

@SpringBootApplication
@EnableConfigurationProperties
public class BootifulBannersApplication {

    public static void main(String[] args) {
        new SpringApplicationBuilder()
                .sources(BootifulBannersApplication.class)
                .logStartupInfo(false)
                .build()
                .run(args);
//        SpringApplication.run(BootifulBannersApplication.class, args);
    }
}

@Component
class BannerRunner implements ApplicationRunner {

    @Autowired
    private BannerProperties properties;

    @Override
    public void run(ApplicationArguments args) throws Exception {
        ImageBanner imageBanner = new ImageBanner(
                this.resolveInputImage());

        int maxWidth = this.properties.getMaxWidth();
        double aspectRatio = this.properties.getAspectRatio();
        boolean invert = this.properties.isInvert();
        Resource output = this.properties.getOutput();

        String banner = imageBanner.printBanner(maxWidth, aspectRatio, invert);
        if (output != null) {
            try (PrintWriter pw = new PrintWriter(output.getFile(), "UTF-8")) {
                pw.println(banner);
            }
        } else {
            System.out.println(banner);
        }
    }

    private File resolveInputImage() throws Exception {

        if (this.properties.getInput() != null) {
            return this.properties.getInput().getFile();
        }

        File tmp = File.createTempFile("banner-input-image", ".jpg");
        tmp.deleteOnExit();
        try (
                BufferedInputStream in = new BufferedInputStream(System.in);
                BufferedOutputStream out = new BufferedOutputStream(new FileOutputStream(tmp))) {
            FileCopyUtils.copy(in, out);
        }
        return tmp;

    }
}

@Data
@Component
@ConfigurationProperties(prefix = "banner")
class BannerProperties {

    private int maxWidth = 72;

    private double aspectRatio = 0.5;

    private Resource output;

    private boolean invert;

    private Resource input;
}
