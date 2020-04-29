package kg.demirbank.model;

import kg.demirbank.model.base.FileBase;

import javax.persistence.Entity;


@Entity
public class FileEntity extends FileBase {
    @Override
    public boolean equals(Object o) {
        return super.equals(o);
    }
}