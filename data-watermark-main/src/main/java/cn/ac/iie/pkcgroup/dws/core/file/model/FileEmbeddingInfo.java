package cn.ac.iie.pkcgroup.dws.core.file.model;

import lombok.Data;

import java.io.InputStream;

@Data
public class FileEmbeddingInfo {
    String systemId;
    String embeddingMessage;
    String outputPath;
    String tmpRootPath;
    String fileName;
    String filePassword;
    String fileId; // used for fingerprint
    InputStream sourceFile;
    boolean isOwner;
    String embeddingMethod; // reserve
    boolean useVisibleWatermark; // default true
}
