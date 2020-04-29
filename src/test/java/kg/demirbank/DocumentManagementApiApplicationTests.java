//package kg.demirbank;
//
//import kg.demirbank.model.FileEntity;
//import kg.demirbank.service.FileStorageService;
//
//import org.junit.Test;
//import org.junit.runner.RunWith;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.beans.factory.annotation.Value;
//import org.springframework.boot.test.context.SpringBootTest;
//import org.springframework.mock.web.MockMultipartFile;
//import org.springframework.stereotype.Component;
//import org.springframework.test.context.junit4.SpringRunner;
//import org.springframework.web.multipart.MultipartFile;
//
//import java.io.*;
//import java.nio.file.Files;
//import java.nio.file.Path;
//import java.nio.file.Paths;
//import static org.junit.Assert.*;
//
//@RunWith(SpringRunner.class)
//@SpringBootTest
//public class DocumentManagementApiApplicationTests {
//
//	@Autowired
//	FileStorageService storageService;
//
//	@Value("${meduza}")
//	private String imageUrl;
//
//	@Value("${format.jpeg}")
//	private String formatJpeg;
//
//	@Test
//	public void contextLoads() {
//	}
//
//	@Test
//	public void deleteImageByIdTest() throws IOException {
//
//
//		Path path = Paths.get(imageUrl);
//		String name = path.normalize().toString();
//		String originalFileName = name;
//		String contentType = formatJpeg;
//		byte[] content = null;
//		try {
//			content = Files.readAllBytes(path);
//		} catch (final IOException e) {
//		}
//		MultipartFile multipartFile = new MockMultipartFile(name,
//				originalFileName, contentType, content);
//		FileEntity fileEntity = storageService.storeFile(multipartFile, "", "");
//		FileEntity fileEntity1  = storageService.deleteImageById(fileEntity.getId()).getBody();
//		assertEquals(fileEntity,fileEntity1);
//		//this function must take an existing file,or upload it, and then delete by using delete
//	}
//
//
//}
