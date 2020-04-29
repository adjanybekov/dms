package kg.demirbank;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

import kg.demirbank.config.FileStorageProperties;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;

@SpringBootApplication
//@EnableDiscoveryClient
//@EnableFeignClients
@EnableConfigurationProperties({FileStorageProperties.class})
public class DocumentManagementApiApplication {

	public static void main(String[] args) {
		SpringApplication.run(DocumentManagementApiApplication.class, args);
	}
//
//	@Bean
//	public Sampler alwaysSampler() {
//		return Sampler.ALWAYS_SAMPLE;
//	}



}
