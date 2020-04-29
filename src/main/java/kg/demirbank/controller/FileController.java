package kg.demirbank.controller;

import com.sun.org.apache.xpath.internal.operations.Bool;
import io.swagger.annotations.ApiParam;
import kg.demirbank.dto.FileCopyDto;
import kg.demirbank.model.FileEntity;
import kg.demirbank.service.FileStorageService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.HandlerMapping;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@RestController
public class FileController {

	private static final Logger logger = LoggerFactory.getLogger(FileController.class);

    @Autowired
    private FileStorageService fileStorageService;

    @PostMapping(value = {"/uploadFile/**"})
    public ResponseEntity uploadFile(@RequestPart("file") MultipartFile file, @RequestPart(value="tags",required=false) List<String> tags, HttpServletRequest request) {
        String directory = getDirectoryFromRequest(request);
        if(tags==null){
            tags=new ArrayList<>();
        }
        FileEntity fileEntity = fileStorageService.storeFile(file, tags,directory);

        return new ResponseEntity(fileEntity, HttpStatus.OK);
    }


    @PostMapping(value = {"/uploadFileToTmp/**"})
    public ResponseEntity<Map> uploadFileToTmp(@RequestPart("file") MultipartFile file, HttpServletRequest request) {
        String directory = getDirectoryFromRequest(request);
        ResponseEntity<Map> saved = fileStorageService.uploadFileToTmp(file, directory);
        return saved;
    }


    @PostMapping({"/uploadMultipleFiles/**"})
    public List<ResponseEntity> uploadMultipleFiles(@RequestPart("files") MultipartFile[] files,@RequestPart("tags") List<String>[] tagsArray, HttpServletRequest request) {

        return IntStream.range(0, Math.min(files.length,tagsArray.length))
                .mapToObj(i-> uploadFile(files[i],tagsArray[i],request))
                .collect(Collectors.toList());

    }

    @PostMapping({"/uploadMultipleFilesToTmp/**"})
    public List<ResponseEntity> uploadMultipleFilesToTmp(@RequestPart("files") MultipartFile[] files , HttpServletRequest request) {

        return IntStream.range(0, files.length)
                .mapToObj(i-> uploadFileToTmp(files[i],request))
                .collect(Collectors.toList());
    }

    @GetMapping("/downloadFile/{fileName:.+}")
    public ResponseEntity<Resource> downloadFile(@PathVariable String fileName, HttpServletRequest request) {

        return fileStorageService.downloadFileByName(fileName,request);
    }

    @GetMapping("/downloadFileById/{fileId}")
    public ResponseEntity<Resource> downloadFile(@PathVariable int fileId,HttpServletRequest request) {
        // Load file from database
        return fileStorageService.downloadFileById(fileId,request);
    }

    @GetMapping("/getAllFiles")
    public List<FileEntity> getAllFiles(){
        return fileStorageService.getAllFiles();
    }

    @PostMapping(value = "/copyFileFromTmpToFolderById/{id}/{name}",consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<FileEntity> copyFileFromTmpToFolderById(@PathVariable("id") String id,@PathVariable("name") String name,@RequestBody Map<String,Object> map){
        return fileStorageService.copyFileFromTmpToFolderById(id,name,map);
    }

    @PostMapping("/findFilesByNameLike/{id}")
    public ResponseEntity<String[]> findFilesByNameLike(@PathVariable("id") String id,@RequestBody String finalPath){
        return new ResponseEntity<>(fileStorageService.findFilesByNameLike(id,finalPath),HttpStatus.OK);
    }


    @DeleteMapping("/deleteFileById/{id}")
    public ResponseEntity<FileEntity> deleteFileById(@PathVariable("id") Integer id){
//        what is it going to do in case of failure ?
        return fileStorageService.deleteFileById(id);
    }

    @GetMapping("/getFilesByPathName/**")
    public List<FileEntity> getFilesByPathName(HttpServletRequest request){
//        what is it going to do in case of failure ?
       String path = getDirectoryFromRequest(request);
       return fileStorageService.getFilesDataByPathName(path);
    }

    @GetMapping("/getTagsByFileId/{id}")
    public ResponseEntity getTagsByFileId(@PathVariable("id") int id){
//        what is it going to do in case of failure ?
        return fileStorageService.getTagsByFileId(id);
    }
//      для тестирования announcementapi
    @GetMapping("/getHtmlFileContent/{id}")
    public String getHtmlFileContent(@PathVariable("id") Integer id) throws IOException {
        return fileStorageService.getHtmlFileContent(id);
    }
    @GetMapping("/getLastId")
    public Integer getLastId() throws IOException {
        return fileStorageService.getLastId();
    }

    @GetMapping("/getFilesByTagName/{tagName}")
    public List<FileEntity> getFilesByTagName(@PathVariable("tagName") String tagName){
        return fileStorageService.getFilesByTagName(tagName);
    }

//    @GetMapping("/getFilesByTags")
//    public List<FileEntity> getFilesByTags(@RequestParam("tags") List<String> tags,HttpServletRequest request){
////        what is it going to do in case of failure ?
//
//        return fileStorageService.getFilesByTags(tags);
//    }

    @PostMapping("/addTagToFile/{fileId}/{tagName}")
    public ResponseEntity addTagToFile(@PathVariable("fileId") int fileId, @PathVariable("tagName") String tagName,HttpServletRequest request){
        return fileStorageService.addTagToFile(fileId,tagName);
    }

    @PostMapping("/addTagToFileByTagId/{fileId}/{tagId}")
    public ResponseEntity addTagToFileByTagId(@PathVariable("fileId") Integer fileId, @PathVariable("tagId") Integer tagId,HttpServletRequest request){
        return fileStorageService.addTagToFileByTagId(fileId,tagId);
    }

    @DeleteMapping("/removeTagFromFile/{fileId}/{tagName}")
    public ResponseEntity removeTagFromFile(@PathVariable("fileId") int fileId, @PathVariable("tagName") String tagName){
        return fileStorageService.removeTagFromFile(fileId, tagName);
    }


//    @GetMapping("/getFilesByTags/**")
//    public List<FileEntity> getFilesByTags(HttpServletRequest request){
//        String tags = getDirectoryFromRequest(request);
//        return fileStorageService.getFilesByTags(tags);
//    }

    @GetMapping(value = "/viewFileById/{id}", produces = MediaType.IMAGE_JPEG_VALUE)
    public ResponseEntity<byte[]> viewFile(HttpServletRequest request,@PathVariable("id") Integer id) throws IOException {

        ResponseEntity<byte[]> responseEntity = fileStorageService.viewFileById(id);
        return responseEntity;

    }

    @PostMapping(value = {"/getFilesByIds"})
    public ResponseEntity getFilesByIds(@RequestBody List<String> idList){

        return fileStorageService.getFilesByIds(idList);
    }


    private String getDirectoryFromRequest(HttpServletRequest request) {

        String path = (String) request.getAttribute(
                HandlerMapping.PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE);
        String bestMatchPattern = (String) request.getAttribute(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE);

        AntPathMatcher apm = new AntPathMatcher();
        String directory = apm.extractPathWithinPattern(bestMatchPattern, path);
        return directory;
    }

    @GetMapping("/")
    public String index(){
        return "redirect:index";
    }


    @PostMapping(value = "/copyFile",consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<FileEntity> copyFile(@ApiParam(name = "src = tmp, dest=C:, root") @RequestBody FileCopyDto fileCopyDto){
        FileEntity fileEntity = fileStorageService.copyFile(fileCopyDto);
        if(fileEntity==null)return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        return ResponseEntity.ok(fileEntity);
    }
}
