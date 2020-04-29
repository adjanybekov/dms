package kg.demirbank.repository;

import kg.demirbank.model.TagEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TagFileRepo extends JpaRepository<TagEntity,Integer> {

    TagEntity findByTagName(String name);
}
