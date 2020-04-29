package kg.demirbank.model.base;

import kg.demirbank.model.TagEntity;

import javax.persistence.*;
import java.util.*;

@MappedSuperclass
public abstract class FileBase extends EntityBase {
    public FileBase() {
    }
    private String name;
    private String nameWithExtension;
    private String extension;
    private Date createdDate;
    private String path;
    private String downloadUri;
    private Long size;
    private String uuidNameWithExtension;
    @ManyToMany(cascade = {
            CascadeType.PERSIST,
            CascadeType.MERGE
    },fetch = FetchType.LAZY)
    @JoinTable(name = "file_tags",
            joinColumns = @JoinColumn(name = "file_id"),
            inverseJoinColumns = @JoinColumn(name = "tag_id"))
    private Set<TagEntity> tags;


//todo:
//       private String deleted; //Deleted, Exist,

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getExtension() {
        return extension;
    }

    public void setExtension(String extension) {
        this.extension = extension;
    }

    public Date getCreatedDate() {
        return createdDate;
    }

    public void setCreatedDate(Date createdDate) {
        this.createdDate = createdDate;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public String getDownloadUri() {
        return downloadUri;
    }

    public void setDownloadUri(String downloadUri) {
        this.downloadUri = downloadUri;
    }

    public Long getSize() {
        return size;
    }

    public String getNameWithExtension() {
        return this.getName()+"."+this.getExtension();
    }

    public void setNameWithExtension(String nameWithExtension) {
        this.nameWithExtension = nameWithExtension;
    }

    public void setSize(Long size) {
        this.size = size;
    }


    public String getUuidNameWithExtension() {
        return uuidNameWithExtension;
    }

    public void setUuidNameWithExtension(String uuidNameWithExtension) {
        this.uuidNameWithExtension = uuidNameWithExtension;
    }

    public  String getPathWithFileUuidNameAndExtension(){
        return this.getPath()+"/"+this.getUuidNameWithExtension();
    }


    public Set<TagEntity> getTags() {
        return tags;
    }

    public void setTags(Set<TagEntity> tags) {
        this.tags = tags;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        FileBase fileBase = (FileBase) o;
        return Objects.equals(name, fileBase.name) &&
                Objects.equals(nameWithExtension, fileBase.nameWithExtension) &&
                Objects.equals(extension, fileBase.extension) &&
                Objects.equals(createdDate, fileBase.createdDate) &&
                Objects.equals(path, fileBase.path) &&
                Objects.equals(downloadUri, fileBase.downloadUri) &&
                Objects.equals(size, fileBase.size);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), name, nameWithExtension, extension, createdDate, path, downloadUri, size);
    }

}
