package com.example;

import lombok.Data;
import org.aopalliance.intercept.MethodInterceptor;
import org.springframework.aop.framework.ProxyFactoryBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ImageBanner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.ansi.AnsiPropertySource;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.core.env.Environment;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.core.env.PropertyResolver;
import org.springframework.core.env.PropertySourcesPropertyResolver;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;
import org.springframework.util.FileCopyUtils;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * I have made so many poor life decisions..
 *
 * @author Josh Long
 * @author Dave Syer
 */
@EnableConfigurationProperties
@SpringBootApplication
public class BootifulBannersServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(BootifulBannersServiceApplication.class, args);
	}
}

// curl  -F "image=@/Users/jlong/Desktop/doge.jpg" -H "Content-Type: multipart/form-data"  -F"invert=true"  http://localhost:8080/banner
@RestController
class BannerGeneratorRestController {

	public static final String[] MEDIA_TYPES = {
			MediaType.IMAGE_PNG_VALUE,
			MediaType.IMAGE_JPEG_VALUE,
			MediaType.IMAGE_GIF_VALUE};

	@Autowired
	private BannerProperties properties;

	@Autowired
	private Environment environment;


	@RequestMapping(
			value = "/banner",
			method = RequestMethod.POST,
			produces = MediaType.TEXT_PLAIN_VALUE
	)
	ResponseEntity<String> banner(@RequestParam("image") MultipartFile multipartFile,
	                              @RequestParam(required = false) Integer maxWidth,
	                              @RequestParam(required = false) Boolean invert,
	                              @RequestParam(defaultValue = "false") boolean ansiOutput) throws Exception {
		File image = null;
		try {
			image = this.imageFileFrom(multipartFile);
			ImageBanner imageBanner = new ImageBanner(new FileSystemResource(image));

			if (maxWidth == null) {
				maxWidth = this.properties.getMaxWidth();
			}

			if (invert == null) {
				invert = this.properties.isInvert();
			}

			String banner;

			try (ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
			     PrintStream bannerWriter = new PrintStream(byteArrayOutputStream)) {
				imageBanner.printBanner(this.environmentForImage(maxWidth, invert), null, bannerWriter);
				banner = byteArrayOutputStream.toString();
			}

			if (ansiOutput) {
				MutablePropertySources sources = new MutablePropertySources();
				sources.addFirst(new AnsiPropertySource("ansi", true));
				PropertyResolver ansiResolver = new PropertySourcesPropertyResolver(sources);
				banner = ansiResolver.resolvePlaceholders(banner);
			}

			return ResponseEntity.ok()
					.contentType(MediaType.TEXT_PLAIN)
					.header("Content-Disposition", "attachment; filename=banner.txt;")
					.body(banner);

		} finally {
			if (image != null) {
				if (image.exists())
					Assert.isTrue(image.delete(), String.format("couldn't delete temporary file %s",
							image.getPath()));
			}
		}
	}

	private Environment environmentForImage(int maxWidth, boolean invert) {
		Map<String, Object> specification = new HashMap<>();
		specification.put("banner.image.width", maxWidth);
		specification.put("banner.image.invert", invert);
		ProxyFactoryBean proxyFactoryBean = new ProxyFactoryBean();
		proxyFactoryBean.setInterfaces(Environment.class);
		proxyFactoryBean.addAdvice((MethodInterceptor) invocation -> {
			String containsProperty = "containsProperty";
			String getProperty = "getProperty";
			List<String> toHandle = Arrays.asList(containsProperty, getProperty);
			String methodName = invocation.getMethod().getName();
			if (toHandle.contains(methodName)) {
				String key = String.class.cast(invocation.getArguments()[0]);
				if (methodName.equals(containsProperty)) {
					return (specification.containsKey(key) || this.environment.containsProperty(key));
				}
				if (methodName.equals(getProperty)) {
					return specification.getOrDefault(key, this.environment.getProperty(key));
				}
			}
			return invocation.getMethod().invoke(this.environment, invocation.getArguments());
		});
		return Environment.class.cast(proxyFactoryBean.getObject());
	}

	private File imageFileFrom(MultipartFile file) throws Exception {
		Assert.notNull(file);
		Assert.isTrue(Arrays.asList(MEDIA_TYPES).contains(file.getContentType().toLowerCase()));
		File tmp = File.createTempFile("banner-tmp-",
				"." + file.getContentType().split("/")[1]);
		try (InputStream i = new BufferedInputStream(file.getInputStream());
		     OutputStream o = new BufferedOutputStream(new FileOutputStream(tmp))) {
			FileCopyUtils.copy(i, o);
			return tmp;
		}
	}
}

@Data
@Component
@ConfigurationProperties(prefix = "banner")
class BannerProperties {

	private int maxWidth = 72;

	private boolean invert;

}