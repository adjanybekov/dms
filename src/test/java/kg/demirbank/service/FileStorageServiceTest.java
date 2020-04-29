package kg.demirbank.service;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import static org.junit.Assert.*;


public class FileStorageServiceTest {

    @Autowired
    FileStorageService fileStorageService;

//    @Test
//    public void storeFile() {
//        RestTemplate restTemplate =  new RestTemplate();
//        String url = "http://localhost:8700/getAllFiles";
//        ResponseEntity<String> response =  restTemplate.getForEntity(url,String.class);
//
//        assertEquals(response.getStatusCode(), HttpStatus.OK);
//
//    }

    @Test
    public void loadFileAsResource() {
    }

    @Test
    public void getPath() {
    }

    @Test
    public void deleteImageById() {
    }

    @Test
    public void getAllFiles() {
    }

    @Test
    public void downloadFileById() {
    }

    @Test
    public void getFilesByPathName() {
    }

    @Test
    public void getTagsByFileId() {
    }

    @Test
    public void addNewTagToFile() {
    }

    @Test
    public void addExistingTagToFile() {
    }

    @Test
    public void deleteTagByFileIdAndTagId() {
    }

    @Test
    public void getFilesByTags() {
        /*create file
        add some tags to it
        call get filesByTags
        make sure that all createdFiles by those tags are being returned
         */
    }
}
