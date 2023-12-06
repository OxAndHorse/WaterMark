package cn.ac.iie.pkcgroup.dws.core.file;

import cn.ac.iie.pkcgroup.dws.comm.response.StatusCodes;
import cn.ac.iie.pkcgroup.dws.comm.response.file.FileListResponse;
import cn.ac.iie.pkcgroup.dws.core.WatermarkException;
import cn.ac.iie.pkcgroup.dws.core.file.interfaces.IDecoder;
import cn.ac.iie.pkcgroup.dws.core.file.interfaces.IEncoder;
import cn.ac.iie.pkcgroup.dws.core.file.model.FileEmbeddingInfo;
import cn.ac.iie.pkcgroup.dws.core.file.model.FileExtractInfo;
import cn.ac.iie.pkcgroup.dws.core.file.response.FileResponse;
import cn.ac.iie.pkcgroup.dws.core.file.response.FileStatusCodes;
import cn.ac.iie.pkcgroup.dws.utils.StringUtils;
import lombok.extern.slf4j.Slf4j;
import org.iq80.leveldb.DB;
import org.iq80.leveldb.DBIterator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Map;

import static cn.ac.iie.pkcgroup.dws.core.file.Constants.DW_FILE_SUFFIX;

@Component
@Slf4j
public class FileHandler {
    private DB mappingDB;
    private final String DIVIDER = "-";
    private static final String OWNER_PREFIX = "版权所有者：";
    private static final String USER_PREFIX = "导出用户：";

    @Value("${conf.fileRoot}")
    private String fileRoot;

    @Autowired
    public void setMappingDB(@Qualifier(value = "fileMapping") DB db) {
        mappingDB = db;
    }

    private String generateFileOutputPath(String systemId, String fileName) {
        StringBuilder newFileName = new StringBuilder();
        newFileName.append(fileRoot).append("/").append(systemId).append("/").append(fileName);
        // Check whether directory exists.
        File filePath = new File(newFileName.toString());
        if (!filePath.exists()) {
            if (!filePath.mkdirs()) {
                log.error("Cannot make output directory.");
                return null;
            }
        }
        return newFileName.toString();
    }

    private String generateDWFileName(StringBuilder newFileName, String fileName, String fileId) {
        String pat = "_";
        int dotIndex = fileName.lastIndexOf(".");
        if (dotIndex < 0) {
            log.error("Illegal file name.");
            return null;
        }
        newFileName.append(fileName, 0, dotIndex).append(DW_FILE_SUFFIX);
        newFileName.append(pat).append(fileId).append(fileName.substring(dotIndex));

        return newFileName.toString();
    }

    private String generateDWFileOutputPath(String systemId, String fileName, String fileId) {
        StringBuilder newFileName = new StringBuilder();
        newFileName.append(fileRoot).append("/").append(systemId).append("/dw/");
        // Check whether directory exists.
        File filePath = new File(newFileName.toString());
        if (!filePath.exists()) {
            if (!filePath.mkdirs()) {
                log.error("Cannot make output directory.");
                return null;
            }
        }
        return generateDWFileName(newFileName, fileName, fileId);
    }

    private String generateMappingKey(String systemId, String fileId) {
        return fileId + DIVIDER + systemId;
    }

    private String parseFileIdFromMappingKey(String key) {
        String[] list = key.split(DIVIDER);
        if (list.length <= 1) return null;
        return list[0];
    }

    private boolean saveMapping(String fileId, String filePath) {
        if (fileId == null || fileId.isEmpty()) {
            log.error("Fail to record mapping: Empty file id or file size.");
            return false;
        }
        mappingDB.put(fileId.getBytes(StandardCharsets.UTF_8), filePath.getBytes(StandardCharsets.UTF_8));
        return true;
    }

    private String getMappingByFileId(String fileId) {
        if (fileId == null || fileId.isEmpty()) {
            log.error("Fail to get mapping: Empty file id.");
            return null;
        }
        byte[] filePath = mappingDB.get(fileId.getBytes(StandardCharsets.UTF_8));
        if (filePath == null || filePath.length == 0) {
            log.error("Get empty file path from the mapping.");
            return null;
        }
        return new String(filePath, StandardCharsets.UTF_8);
    }

    public boolean removeTempFile(String tempFilePath) {
        File tempFile = new File(tempFilePath);
        if (tempFile.exists()) {
            return tempFile.delete();
        } else {
            log.info("No temp file exists.");
            return false;
        }
    }

    public FileResponse embed(FileEmbeddingInfo fileEmbeddingInfo) {
        IEncoder encoder = Parser.parseEncoder(fileEmbeddingInfo);
        if (encoder == null) {
            return new FileResponse(FileStatusCodes.CODE_INIT_ENCODER_FAIL, FileStatusCodes.MSG_INIT_ENCODER_FAIL);
        }

        fileEmbeddingInfo.setUseVisibleWatermark(true); // default true
        try {
            String tmpRootPath = fileRoot + "/" + fileEmbeddingInfo.getSystemId() + "/";
            String fileId = fileEmbeddingInfo.getFileId();
            // In case: fingerprint for user
            if (fileId != null) {
                String mapKey = generateMappingKey(fileEmbeddingInfo.getSystemId(), fileId);
                byte[] path = mappingDB.get(mapKey.getBytes(StandardCharsets.UTF_8));
                if (path == null)
                    return new FileResponse(FileStatusCodes.CODE_NO_SUCH_FILE, FileStatusCodes.MSG_NO_SUCH_FILE);
                String filePath = new String(path, StandardCharsets.UTF_8);
                File file = new File(filePath);
                InputStream inputStream = new FileInputStream(file);
                fileEmbeddingInfo.setSourceFile(inputStream);
            }
            String dwFileId = StringUtils.generateDWFileId(fileEmbeddingInfo.getSystemId(), fileEmbeddingInfo.getFileName());
            String outputPath = generateDWFileOutputPath(fileEmbeddingInfo.getSystemId(), fileEmbeddingInfo.getFileName(), dwFileId);
            fileEmbeddingInfo.setOutputPath(outputPath);
            fileEmbeddingInfo.setTmpRootPath(tmpRootPath);
            String message = fileEmbeddingInfo.isOwner() ? OWNER_PREFIX + fileEmbeddingInfo.getEmbeddingMessage() : USER_PREFIX + fileEmbeddingInfo.getEmbeddingMessage();
            fileEmbeddingInfo.setEmbeddingMessage(message);

            encoder.encode(fileEmbeddingInfo);
            FileResponse fileResponse = FileResponse.builder()
                    .statusCode(FileStatusCodes.CODE_SUCCESS)
                    .message(FileStatusCodes.MSG_SUCCESS)
                    .build();
            String dwFileName = generateDWFileName(new StringBuilder(), fileEmbeddingInfo.getFileName(), dwFileId);
//            if (dwFileId != null) {
            if (!fileEmbeddingInfo.isOwner() || fileEmbeddingInfo.isOwner() && saveMapping(generateMappingKey(fileEmbeddingInfo.getSystemId(), dwFileId), outputPath)) {
                fileResponse.setDwFileId(dwFileId);
                fileResponse.setFileName(dwFileName);
            } else {
                return new FileResponse(FileStatusCodes.CODE_SAVE_MAPPING_FAIL, FileStatusCodes.MSG_SAVE_MAPPING_FAIL);
            }
            fileResponse.setFilePath(outputPath);
//            } else {
//                return new FileResponse(FileStatusCodes.CODE_ILLEGAL_FILENAME, FileStatusCodes.MSG_ILLEGAL_FILENAME);
//            }
            return fileResponse;
        } catch (WatermarkException e) {
            return new FileResponse(FileStatusCodes.CODE_EMBEDDING_FAIL, FileStatusCodes.MSG_EMBEDDING_FAIL);
        } catch (FileNotFoundException e) {
            return new FileResponse(FileStatusCodes.CODE_NO_SUCH_FILE, FileStatusCodes.MSG_NO_SUCH_FILE);
        }
    }

    // TODO: should handle DOUBLE embedding
    public FileResponse extract(FileExtractInfo fileExtractInfo) {
        IDecoder decoder = Parser.parseDecoder(fileExtractInfo);
        if (decoder == null) {
            return new FileResponse(FileStatusCodes.CODE_INIT_DECODER_FAIL, FileStatusCodes.MSG_INIT_DECODER_FAIL);
        }
        try {
            String tmpRootPath = fileRoot + "/" + fileExtractInfo.getSystemId() + "/";
//            String outputPath = generateDWFileOutputPath(fileExtractInfo.getSystemId(), fileExtractInfo.getFileName(), null);
//            fileExtractInfo.setOutputPath(outputPath);
            fileExtractInfo.setTmpRootPath(tmpRootPath);
            String message = decoder.decode(fileExtractInfo);
            log.info("Extract message: " + message);
            FileResponse fileResponse = new FileResponse(FileStatusCodes.CODE_SUCCESS, FileStatusCodes.MSG_SUCCESS);
            fileResponse.setExtractedMessage(message);
            return fileResponse;
        } catch (WatermarkException e) {
            return new FileResponse(FileStatusCodes.CODE_EXTRACTING_FAIL, FileStatusCodes.MSG_EXTRACTING_FAIL);
        }
    }

    /**
     * Save file to the server
     *
     * @param fileName file name
     * @param file     file
     * @return FileResponse response
     */
    public FileResponse saveFile(String systemId, String fileName, MultipartFile file) {
        String fileId = StringUtils.generateFileId(file);
        String outputPath = generateFileOutputPath(systemId, fileName);
        if (outputPath == null)
            return new FileResponse(FileStatusCodes.CODE_SAVE_MAPPING_FAIL, FileStatusCodes.MSG_SAVE_MAPPING_FAIL);
        try {
            file.transferTo(new File(outputPath));
        } catch (IOException e) {
            log.error("Fail to save file to the server due to IO exception.");
            return new FileResponse(FileStatusCodes.CODE_SAVE_MAPPING_FAIL, FileStatusCodes.MSG_SAVE_MAPPING_FAIL);
        }
        // file mapping key: systemId-fileId
        if (saveMapping(generateMappingKey(systemId, fileId), outputPath)) {
            return FileResponse.builder().statusCode(FileStatusCodes.CODE_SUCCESS).message(FileStatusCodes.MSG_SUCCESS).fileId(fileId).build();
        } else {
            return new FileResponse(FileStatusCodes.CODE_SAVE_MAPPING_FAIL, FileStatusCodes.MSG_SAVE_MAPPING_FAIL);
        }
    }

    public FileResponse getFile(String systemId, String fileId, String fileName) {
        String filePath = getMappingByFileId(generateMappingKey(systemId, fileId));
        if (filePath == null || !filePath.contains(systemId) || !filePath.contains(fileName)) {
            return new FileResponse(FileStatusCodes.CODE_GET_MAPPING_FAIL, FileStatusCodes.MSG_GET_MAPPING_FAIL);
        }
        File file = new File(filePath);
        if (!file.exists()) {
            log.info("Cannot find file according to the path: " + filePath);
            return new FileResponse(FileStatusCodes.CODE_NO_SUCH_FILE, FileStatusCodes.MSG_NO_SUCH_FILE);
        }
        return FileResponse.builder()
                .statusCode(FileStatusCodes.CODE_SUCCESS)
                .message(FileStatusCodes.MSG_SUCCESS)
                .filePath(filePath)
                .build();
    }

    public FileListResponse getFileListBySystemId(String systemId) {
        DBIterator iterator = mappingDB.iterator();
        ArrayList<FileListResponse.FileMetaData> fileList = new ArrayList<>();
        while (iterator.hasNext()) {
            Map.Entry<byte[], byte[]> kv = iterator.next();
            String key = new String(kv.getKey(), StandardCharsets.UTF_8);
            if (key.contains(systemId)) {
                FileListResponse.FileMetaData fileMetaData = new FileListResponse.FileMetaData();
                String fileId = parseFileIdFromMappingKey(key);
                if (fileId == null) {
                    return FileListResponse.builder().statusCode(StatusCodes.CODE_INTERNAL_ERROR).message(StatusCodes.MSG_INTERNAL_ERROR).build();
                }
                fileMetaData.setFileId(fileId);
                String filePath = new String(kv.getValue(), StandardCharsets.UTF_8);
                fileMetaData.setFileName(StringUtils.parseFileName(filePath));
                fileMetaData.setFileType(StringUtils.parseFileType(filePath));
                fileList.add(fileMetaData);
            }
        }
        return FileListResponse.builder()
                .statusCode(StatusCodes.CODE_SUCCESS)
                .message(StatusCodes.MSG_SUCCESS)
                .fileList(fileList)
                .fileCount(fileList.size())
                .build();
    }
}
