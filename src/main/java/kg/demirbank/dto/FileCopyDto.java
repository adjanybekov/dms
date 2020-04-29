package kg.demirbank.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Accessors(chain = true)
public class FileCopyDto {

    private String sourceFolder;
    private String sourceFile;
    private String destFolder;
    private String destFile;
    private List<String> tags;

}
