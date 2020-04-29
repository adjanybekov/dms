package kg.demirbank.model;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import kg.demirbank.model.base.EntityBase;
import org.hibernate.annotations.BatchSize;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;
import org.hibernate.annotations.JoinFormula;

import javax.persistence.*;
import java.util.Objects;
import java.util.Set;

@Entity
public class TagEntity extends EntityBase {
    private String tagName;

    @ManyToMany(mappedBy = "tags",fetch = FetchType.LAZY)
    @JsonIgnore
    private Set<FileEntity> fileEntityList;

    public TagEntity() {
    }

    public TagEntity(String tagName) {
        this.tagName = tagName;
    }

    public String getTagName() {
        return tagName;
    }

    public void setTagName(String tagName) {
        this.tagName = tagName;
    }

    public Set<FileEntity> getFileEntityList() {
        return fileEntityList;
    }

    public void setFileEntityList(Set<FileEntity> fileEntityList) {
        this.fileEntityList = fileEntityList;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        TagEntity tagEntity = (TagEntity) o;
        return Objects.equals(tagName, tagEntity.tagName) &&
                Objects.equals(fileEntityList, tagEntity.fileEntityList);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), tagName, fileEntityList);
    }
//
//    public TagEntity getParent() {
//        return parent;
//    }
//
//    public void setParent(TagEntity parent) {
//        this.parent = parent;
//    }
}
