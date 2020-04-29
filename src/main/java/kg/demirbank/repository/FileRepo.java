package kg.demirbank.repository;


import kg.demirbank.model.FileEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDate;


public interface FileRepo extends JpaRepository<FileEntity,Integer> {

//    List<Image> findAll();
//
//    Image findById(int id);
//    @Query("SELECT '*' FROM file_entity where name = :name")
    FileEntity findByName(String name);

    @Query(value = "SELECT path FROM FILE_ENTITY  where id = ?1",nativeQuery = true)
    String getPathById(Integer id);

    @Query(value = "select id from file_entity order by id desc limit 1 ",nativeQuery = true)
    Integer getLastId();

    FileEntity findByUuidNameWithExtension(String name);

    boolean existsById(Integer id);
}
