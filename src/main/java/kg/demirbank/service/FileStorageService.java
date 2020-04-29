package kg.demirbank.service;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.*;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import io.micrometer.core.instrument.Tags;
import kg.demirbank.config.Common;
import kg.demirbank.dto.FileCopyDto;
import kg.demirbank.model.FileEntity;
import kg.demirbank.model.TagEntity;
import kg.demirbank.repository.FileRepo;
import kg.demirbank.repository.TagFileRepo;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import kg.demirbank.config.FileStorageProperties;
import kg.demirbank.exception.DocumentNotFoundException;
import kg.demirbank.exception.FileStorageException;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import javax.imageio.ImageIO;
import javax.persistence.EntityManager;
import javax.persistence.Query;
import javax.servlet.http.HttpServletRequest;
import javax.transaction.Transactional;

@Service
public class FileStorageService {

    private final Path fileStorageLocation;
    private static final Logger logger = LoggerFactory.getLogger(FileStorageService.class);

    @Autowired
    private FileRepo fileRepo;

    @Autowired
    private TagFileRepo tagFileRepo;

    @Autowired
    private EntityManager em;


    @Autowired
    public FileStorageService(FileStorageProperties fileStorageProperties) {
        this.fileStorageLocation = Paths.get(fileStorageProperties.getUploadDir())
                .toAbsolutePath().normalize();

        try {
            Files.createDirectories(this.fileStorageLocation);
        } catch (Exception ex) {
            throw new FileStorageException("Could not create the directory where the uploaded files will be stored.", ex);
        }
    }

    @Transactional
    public FileEntity storeFile(MultipartFile multipartFile, List<String> tags, String directory) {
        // Normalize file name
        String fullFileName = StringUtils.cleanPath(multipartFile.getOriginalFilename());

        try {
            // Check if the file's name contains invalid characters
            if(fullFileName.contains("..")) {
                throw new FileStorageException("Sorry! Filename contains invalid path sequence " + fullFileName);
            }

            //separate name and extension to change filename in both db and file system


            Path targetLocation = this.fileStorageLocation.resolve(fullFileName).getParent();
            targetLocation  = Paths.get(targetLocation.toString(), Common.getValue(directory,""));

            //create new directories if specified with the file
            createDirectories(targetLocation);

            //adding (i) for those which have copies
            //todo: add unique identification before name
            String name = getFileName(fullFileName);
            String uniqueName = setUniqueName(name);
            String extension = getFileExtension(fullFileName);
            targetLocation = setFileNameWithFullPath(targetLocation,uniqueName,extension);

            //this line saves file to the file system
            Files.copy(multipartFile.getInputStream(), targetLocation);//try saving without any copy options...

            //save fileData with location, original name and other names as well
            FileEntity fileEntity = saveFileEntityToDb(multipartFile,targetLocation,tags);

            return fileEntity;

        } catch (IOException ex) {
            throw new FileStorageException("Could not store file " + fullFileName + ". Please try again!", ex);
        }
    }

    private String setUniqueName(String name) {
        String unique =  String.valueOf(System.currentTimeMillis());
        return unique;
    }

    private void createDirectories(Path targetLocation) {
        File dir  = targetLocation.toFile();
        if (!dir.exists()){
            dir.mkdirs();
        }
    }

    private FileEntity saveFileEntityToDb(MultipartFile multipartFile, Path targetLocation, List<String> tags) {
        FileEntity fileEntity = new FileEntity();
        String newFullFileName  = multipartFile.getOriginalFilename(); //targetLocation.getFileName().toString();
        fileEntity.setName(getFileName(newFullFileName));
        fileEntity.setUuidNameWithExtension(targetLocation.getFileName().toString());
        fileEntity.setExtension(getFileExtension(newFullFileName));
        fileEntity.setPath(targetLocation.getParent().toString());
        String fileDownloadUri = ServletUriComponentsBuilder.fromCurrentContextPath()
                .path("/downloadFile/")
                .path(fileEntity.getPathWithFileUuidNameAndExtension())
                .toUriString();
        fileEntity.setDownloadUri(fileDownloadUri);
        fileEntity.setSize(multipartFile.getSize());
        fileEntity.setCreatedDate(new Date());
        fileEntity.setNameWithExtension(String.format("%s.%s",fileEntity.getName(),fileEntity.getExtension()));
        Set<TagEntity> tagList = new HashSet<>();
        for (String tag:tags) {

            TagEntity tagEntity =tagFileRepo.findByTagName(tag);

            if(tagEntity==null){
                tagEntity = new TagEntity(tag);
                tagFileRepo.save(tagEntity);
            }
            tagList.add(tagEntity);
        }
        fileEntity.setTags(tagList);
        FileEntity save = fileRepo.save(fileEntity);

        return save;
    }


    private FileEntity saveFileToDb(String newName, File file, Path targetLocation, List<String> tags) {
        FileEntity fileEntity = new FileEntity();
        String newFullFileName  = file.getName(); //targetLocation.getFileName().toString();
        fileEntity.setName(newName);
        fileEntity.setUuidNameWithExtension(targetLocation.getFileName().toString());
        fileEntity.setExtension(getFileExtension(newFullFileName));
        fileEntity.setPath(targetLocation.getParent().toString());
//        String fileDownloadUri = ServletUriComponentsBuilder.fromCurrentContextPath()
//                .path("/downloadFile/")
//                .path(fileEntity.getPathWithFileUuidNameAndExtension())
//                .toUriString();
//        fileEntity.setDownloadUri(fileDownloadUri);
        fileEntity.setSize(file.length());
        fileEntity.setCreatedDate(new Date());
        fileEntity.setNameWithExtension(String.format("%s.%s",fileEntity.getName(),fileEntity.getExtension()));
        Set<TagEntity> tagList = new HashSet<>();

        //save tags

        for (int i=0; i<tags.size(); i++) {
                String tag = tags.get(i);

            TagEntity tagEntity =tagFileRepo.findByTagName(tag);

            if(tagEntity==null){
                //check for parent
                tagEntity = new TagEntity(tag);
                tagFileRepo.save(tagEntity);
            }

            tagList.add(tagEntity);
        }

        fileEntity.setTags(tagList);
        FileEntity save = fileRepo.save(fileEntity);

        return save;
    }

    private String getFileName(String fileName) {
        return fileName.substring(0,fileName.lastIndexOf("."));
    }

    private String getFileExtension(String fileName) {
        return  fileName.substring(fileName.lastIndexOf(".")+1);
    }

    public Resource loadFileAsResource(String fileName) {
        try {
            //get name with path
            FileEntity fileEntity = fileRepo.findByUuidNameWithExtension(fileName);
            if(fileEntity!=null){
                Path filePath = this.fileStorageLocation.resolve(fileEntity.getPathWithFileUuidNameAndExtension()).normalize();
                Resource resource = new UrlResource(filePath.toUri());
                if(resource.exists()) {
                    return resource;
                } else {
                    throw new DocumentNotFoundException("File not found " + fileName);
                }
            }else{
                throw new DocumentNotFoundException("File not found in db" + fileName);
            }
        } catch (MalformedURLException ex) {
            throw new DocumentNotFoundException("File not found " + fileName, ex);
        }
    }

    @Transactional
    public ResponseEntity<FileEntity> deleteFileById(Integer id){
        //taking file data from database
        if(fileRepo.existsById(id)){
            FileEntity fileEntity = fileRepo.getOne(id);
            boolean deleted;
            try {
                String path  = fileEntity.getPathWithFileUuidNameAndExtension();
                //delete from file system
                deleted = delete(path);
                if(deleted){
                    //delete from database
                     fileRepo.deleteById(id);
                    return new ResponseEntity(fileEntity,HttpStatus.OK);
                }else{
                    //update history that file doesn't exist on place
                    return new ResponseEntity(fileEntity, HttpStatus.NOT_FOUND);
                }
            } catch (IOException e) {
                e.printStackTrace();
                return new ResponseEntity(fileEntity, HttpStatus.BAD_REQUEST);
            }

        }
        return new ResponseEntity("Not found",HttpStatus.NOT_FOUND);
    }

    private boolean delete(String path) throws IOException {
        File file =  new File(path);
        return file.delete();
    }

    private Path setFileNameWithFullPath(Path dir, String baseName,
                                         String extension)
    {
        Path ret = Paths.get(String.format("%s/%s.%s",dir.toString(), baseName, extension));
        if (!Files.exists(ret))
            return ret;

        for (int i = 1; i < Integer.MAX_VALUE; i++) {
            ret = Paths.get(String.format("%s/%s(%d).%s", dir.toString(),baseName, i, extension));
            if (!Files.exists(ret))
                return ret;
        }
        throw new IllegalStateException("Duplicate file error");
    }
    private String findFileNameUsingDb(Path dir, String baseName,
                                     String extension)
    {
        if(fileRepo.findByName(baseName)!=null){ // check existence of file
            //if inserting second copy then it doesn't have
            int cpNo =1;

            try{
                if(fileRepo.findByName(baseName+String.format("(%d)",1))!=null){
                    while( fileRepo.findByName(baseName+String.format("(%d)",cpNo))!=null){
                        cpNo++;
                    }
                }else{
                    cpNo=1;
                }

            }catch (Exception e){
                cpNo = 1;
            }
            baseName+="("+cpNo+")";
        }

        // Copy file to the target location (Replacing existing file with the same name)
        //we don't want replace existing file, but add duplicate as a new
         String fileName = String.format(baseName+"."+extension);
        return fileName;
    }


    public List<FileEntity> getAllFiles() {
        return fileRepo.findAll();
    }

    public ResponseEntity<Resource> downloadFileById(int fileId, HttpServletRequest request) {
//        Query q = em.createNativeQuery(String.format("select * from file_entity where id= %d", fileId),FileEntity.class);
//        FileEntity fileEntity = (FileEntity) q.getResultList().get(0);

        FileEntity fileEntity = fileRepo.findById(fileId).get();
        return downloadFileByName(fileEntity.getUuidNameWithExtension(),request);

    }

    public List<FileEntity> getFilesDataByPathName(String path) {
        //todo: this is temoprary solution, this will be modified,
        //todo:check pathName for correctness and for existence

        Path targetLocation = this.fileStorageLocation.resolve(path);
//        targetLocation  = Paths.get(targetLocation.toString(), Commons.getOrDefault(directory,""));
        //create new directories if needed
        File dir  = targetLocation.toFile();
        if (!dir.exists()){
            return new ArrayList<>();
        }
        path = path.replace("/","\\\\");
        Query q = em.createNativeQuery(String.format("select * from file_entity where path  like '%s'","%"+path+"%"),FileEntity.class);
        List<FileEntity> resultList = (List<FileEntity>) q.getResultList();

        return resultList;
    }

    public ResponseEntity getTagsByFileId(int id) {

        FileEntity fileEntity = fileRepo.getOne(id);
        return new ResponseEntity<>(fileEntity.getTags(),HttpStatus.OK);
    }

    public ResponseEntity addTagToFile(int fileId, String tagName) {

        FileEntity fileEntity  = fileRepo.getOne(fileId);
        TagEntity tagEntity = tagFileRepo.findByTagName(tagName);
        if(tagEntity==null){
            fileEntity.getTags().add(tagEntity);
            fileEntity.setTags(fileEntity.getTags());
        }

        fileRepo.save(fileEntity);
        return new ResponseEntity(fileEntity,HttpStatus.OK);
    }

    public ResponseEntity removeTagFromFile(int fileId, String tagName) {
        FileEntity fileEntity = fileRepo.getOne(fileId);
        TagEntity tagEntity = tagFileRepo.findByTagName(tagName);

        if(tagEntity!=null){
            fileEntity.getTags().remove(tagEntity);
            fileRepo.save(fileEntity);
            return new ResponseEntity(tagEntity,HttpStatus.OK);
        }
        return new ResponseEntity("Not deleted!",HttpStatus.NOT_FOUND);
    }
//      для тестирования announcementapi
    public String getHtmlFileContent(Integer id) throws IOException {
        FileEntity fileEntity = fileRepo.getOne(id);
        String path = fileEntity.getPath()+"/"+fileEntity.getUuidNameWithExtension();
        String content = new String(Files.readAllBytes(Paths.get(path)));
        return content;
    }

    public Integer getLastId() {
        return fileRepo.getLastId();
    }


//    public List<FileEntity> getFilesByTags(String tagsText) {
//
//
//        String[] tags = tagsText.split("/");
//
//
//        String listOfTags  ="";
//        if(tags.length>0){
//
//            for(int i=0; i<tags.length; i++){
//                if(i!=0){
//                    listOfTags+=",";
//                }
//                listOfTags+=String.format(" '%s'",tags[i]);
//
//            }
//        }
//
//        String query = String.format("select * from file_entity where id in " +
//                "(select file_id from file_tags where tag_id in (select id from tag_entity where tag_name in (%s) ) " +
//                "group by  file_id having count(file_id) = %d )",listOfTags,tags.length);
//
//        Query q = em.createNativeQuery(query,FileEntity.class);
//        List<FileEntity> fileEntity = (List<FileEntity>) q.getResultList();
//        return fileEntity;
//    }

    public ResponseEntity<byte[]> viewFileById(Integer id) throws IOException {
        FileEntity fileEntity = fileRepo.findById(id).get();
        File f = new File(fileEntity.getPath()+"/"+fileEntity.getUuidNameWithExtension());
        FileInputStream in = new FileInputStream(f);
        byte[] media = IOUtils.toByteArray(in);
        HttpHeaders headers = new HttpHeaders();
        ResponseEntity<byte[]> responseEntity = new ResponseEntity<>(media, headers, HttpStatus.OK);
        return responseEntity;
    }
//    private static String encodeFileToBase64Binary(String fileName) throws IOException {
//        File file = new File(fileName);
//        byte[] encoded = Base64.getEncoder().encode(FileUtils.readFileToByteArray(file));
//        return new String(encoded, StandardCharsets.US_ASCII);
//    }

    public ResponseEntity<Resource> downloadFileByName(String fileName, HttpServletRequest request) {

        // Load file as Resource
        Resource resource = this.loadFileAsResource(fileName);

        // Try to determine file's content type
        String contentType = null;
        try {
            contentType = request.getServletContext().getMimeType(resource.getFile().getAbsolutePath());
        } catch (IOException ex) {
            logger.info("Could not determine file type.");
        }

        // Fallback to the default content type if type could not be determined
        if(contentType == null) {
            contentType = "application/octet-stream";
        }

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachments; filename=\"" + resource.getFilename() + "\"")
                .body(resource);


    }

    public List<FileEntity> getFilesByTagName(String tagName) {
//        List<FileTag> fileTags = tagRepo.findFileTagsByFileTagId_TagName(tagName);

        Query q = em.createNativeQuery(
                String.format("select * from file_entity where id in (select file_id from file_tag where tag_name='%s')",tagName), FileEntity.class);
        List<FileEntity> resultList = q.getResultList();
        return resultList;
    }

    public ResponseEntity getFilesByIds(List<String> idList) {

        String query = String.format("select * from file_entity where id in (%s)", String.join(",",idList));
        Query q = em.createNativeQuery(query, FileEntity.class);
        List<FileEntity> fileList = q.getResultList();

        return new ResponseEntity(fileList,HttpStatus.OK);
    }

    public ResponseEntity addTagToFileByTagId(Integer fileId, Integer tagId) {

        FileEntity fileEntity  = fileRepo.getOne(fileId);
        Set<TagEntity> tagEntities = fileEntity.getTags()==null? new HashSet<>(): fileEntity.getTags();
        TagEntity tagEntity = tagFileRepo.getOne(tagId)==null? new TagEntity() :tagFileRepo.getOne(tagId);
        tagEntities.add(tagEntity);
        fileRepo.save(fileEntity);

        return new ResponseEntity(HttpStatus.OK);
    }

    public ResponseEntity<Map> uploadFileToTmp(MultipartFile multipartFile, String directory) {

//        String fullFileName = StringUtils.cleanPath(multipartFile.getOriginalFilename());
        Map<String,String> map = new HashMap<>();
        String fileId = String.valueOf(System.currentTimeMillis());
        String filePath;
        map.put("id",fileId);
        String tempPath = System.getProperty("java.io.tmpdir"); //System.getProperty("java.io.tmpdir")
        try {
            filePath = uploadFile(fileId, tempPath, multipartFile);

            if (Common.getValue(getFileType(Common.getValue(filePath, "")), "").equals("image")) {
                String thumbnailPath = createImageThumbnail(Common.getValue(filePath, ""));
                map.put("thumbNail",imageToString(Common.getValue(thumbnailPath, "")));
            }else{
                String thumbnailPath = createImageThumbnail(Common.getValue("unknown.png", ""));
                map.put("thumbNail",imageToString(Common.getValue(thumbnailPath, "")));
            }
        } catch (IOException e) {
            logger.error("Error creating thumbnail", e);
        }
        return new ResponseEntity<>(map,HttpStatus.OK);
    }


    public String uploadFile(String fileId, String filePath, MultipartFile file) throws IOException {
        File directory = new File(filePath);
        if(!directory.exists()){
            directory.mkdirs();
        }
        Matcher m = Pattern.compile("([\\.][\\w]+)$").matcher(file.getOriginalFilename());
        String extension = m.find() ? m.group(1) : "";
        File newFile = new File(directory, fileId + extension);
        file.transferTo(newFile);
        if (newFile.exists()) {
            return newFile.getAbsolutePath();
        }
        return null;
    }

    public String getRootName(){

        String workingDirectory;
//here, we assign the name of the OS, according to Java, to a variable...
        String OS = (System.getProperty("os.name")).toUpperCase();
//to determine what the workingDirectory is.
//if it is some version of Windows
        if (OS.contains("WIN"))
        {
            //it is simply the location of the "AppData" folder
            workingDirectory = System.getenv("SystemDrive");
        }
//Otherwise, we assume Linux or Mac
        else
        {
            //in either case, we would start in the user's home directory
            workingDirectory = System.getProperty("user.home");
            //if we are on a Mac, we are not done, we look for "Application Support"
//            workingDirectory += "/Library/Application Support";
        }
        return workingDirectory;
    }

    public ResponseEntity<FileEntity> copyFileFromTmpToFolderById(String id, String newName, Map<String, Object> map) {
        String tmpPath = System.getProperty("java.io.tmpdir");
        //here copies of names can come through...
        //how to handle extensions of files ???
        String filePath = map.get("path").toString();

        String root = System.getenv("SystemDrive");

        String names[] = findFilesByNameLike(tmpPath,id);
        String finalPath = null;

        //check if this directory exists

        File directory = new File(root+"/"+filePath);
        if(!directory.exists()){
            directory.mkdirs();
        }

        for(String name: names){
            finalPath = moveFile(tmpPath+"/"+name,directory.toString()+"/"+name);
        }


        //take existing file from path and move to the finla path
        File file = new File(finalPath);
        List<String> tags = (List<String>) map.get("tags");
        Path path  = Paths.get(finalPath);
        FileEntity fileEntity= null;
        if(filePath!=null){
            fileEntity= saveFileToDb(newName,file,path,tags);
        }
        return new ResponseEntity<>(fileEntity,HttpStatus.OK);
    }

    public String[] findFilesByNameLike(String folderPath, String name) {
        File folder = new File(folderPath);
        if (folder.exists()) {
            String names[] = folder.list(new FilenameFilter() {
                @Override
                public boolean accept(File dir, String fileName) {
                    return fileName.matches(name + "\\.[\\w]+");
                }
            });
            return names;
        }
        return null;
    }

    public String moveFile(String filePath, String newPath) {
        File file = new File(filePath);
        if (file.exists()) {
            try {
                Files.copy(Paths.get(file.getAbsolutePath()), Paths.get(newPath));
            } catch (IOException e) {
                return null;
            }
            if (getFile(newPath) != null) {
                return getFile(newPath).getAbsolutePath();
            }
            return null;
        }
        return null;
    }

    public File getFile(String filePath) {
        File file = new File(filePath);
        if (!file.exists()) {
            return null;
        }
        return file;
    }

    public String createImageThumbnail(String filePath) throws IOException {
        Resource rsrc = new UrlResource(Paths.get(filePath).toUri());
        File file = new File(Paths.get(filePath).toUri());
        String fileFullName = file.getName() != "" ? file.getName() : "tempFile";
//        FileInputStream fis = new FileInputStream(file);
        InputStream inputStream = rsrc.getInputStream();
        BufferedImage image = ImageIO.read(inputStream);
        inputStream.close();
        int width = 300;
        int height = (int) (image.getHeight() * ((double) 300 / image.getWidth()));

        Matcher m = Pattern.compile("[\\.]([\\w]+)$").matcher(fileFullName);
        String extension = m.find() ? m.group(1) : "";
        String fileName = m.replaceAll("");

        BufferedImage thumbnail = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = thumbnail.createGraphics();
        graphics.drawImage(image, 0, 0, width, height, null);

        File dest = new File(file.getParent(), fileName + "_th." + extension);
        ImageIO.write(thumbnail, extension.toUpperCase(), dest);
        return dest.getPath();
    }


    public String imageToString(String filePath) throws IOException {
        File file = new File(filePath);
        if (file.exists()) {
            FileInputStream fis = new FileInputStream(file);
            byte[] bytes = new byte[(int) file.length()];
            fis.read(bytes);
            fis.close();
            return Base64.getEncoder().encodeToString(bytes);
        }
        return null;
    }

    public String getFileType(String filePath) {
        File file = new File(filePath);
        String mimetype;
        if (file.exists()) {
            try {
                mimetype = Files.probeContentType(file.toPath());
                return mimetype.split("/")[0];
            } catch (IOException e) {
                return null;
            }
        }
        return null;
    }

    public FileEntity copyFile(FileCopyDto fileCopyDto) {
        String tmpPath = System.getProperty("java.io.tmpdir");

        String root = System.getenv("SystemDrive");

        String src = String.format("%s%s",tmpPath,fileCopyDto.getSourceFile());
        String dest = String.format("%s/%s/%s",root,fileCopyDto.getDestFolder(),
                String.format("%s.%s",String.valueOf(System.currentTimeMillis()),getFileExtension(fileCopyDto.getSourceFile())) );


        try {
            Path path = Files.copy(Paths.get(src), Paths.get(dest));
            // After files are moved save info to database
            File file = new File(path.toUri());

            return persistFile(file,fileCopyDto);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }


    }
    private FileEntity persistFile(File file, FileCopyDto fileCopyDto){

        FileEntity fileEntity = new FileEntity();
        fileEntity.setName(fileCopyDto.getDestFile());
        fileEntity.setCreatedDate(new Date());
        fileEntity.setExtension(getFileExtension(file.getName()));
        fileEntity.setUuidNameWithExtension(file.getName());
        fileEntity.setPath(file.getPath().substring(0,file.getPath().lastIndexOf(file.getName()))); //or get absolute path ??? ant without filename
        fileEntity.setSize(file.length());
        fileEntity.setNameWithExtension(String.format("%s.%s",fileEntity.getName(),fileEntity.getExtension()));

        //save tags
        Set<TagEntity> tagList = new HashSet<>();
        for (int i=0; i<fileCopyDto.getTags().size(); i++) {
            String tag = fileCopyDto.getTags().get(i);

            TagEntity tagEntity =tagFileRepo.findByTagName(tag);

            if(tagEntity==null){
                //check for parent
                tagEntity = new TagEntity(tag);
                tagFileRepo.save(tagEntity);
            }

            tagList.add(tagEntity);
        }

        fileEntity.setTags(tagList);
        FileEntity save = fileRepo.save(fileEntity);

        return save;

    }
}