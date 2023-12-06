package cn.ac.iie.pkcgroup.dws.core.file.model;

import lombok.Data;

import java.io.InputStream;

@Data
public class FileExtractInfo {
    String systemId;
    String outputPath;
    String tmpRootPath;
    String fileName;
    String filePassword;
    InputStream sourceFile;
    String embeddingMethod; // reserve
}
