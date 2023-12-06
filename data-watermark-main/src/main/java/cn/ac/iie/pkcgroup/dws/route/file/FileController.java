package cn.ac.iie.pkcgroup.dws.route.file;

import cn.ac.iie.pkcgroup.dws.comm.response.ExtractResponse;
import cn.ac.iie.pkcgroup.dws.comm.response.StatusCodes;
import cn.ac.iie.pkcgroup.dws.comm.response.file.FileEmbedResponse;
import cn.ac.iie.pkcgroup.dws.comm.response.file.FileUploadResponse;
import cn.ac.iie.pkcgroup.dws.core.file.FileHandler;
import cn.ac.iie.pkcgroup.dws.core.file.model.FileEmbeddingInfo;
import cn.ac.iie.pkcgroup.dws.core.file.model.FileExtractInfo;
import cn.ac.iie.pkcgroup.dws.core.file.response.FileResponse;
import cn.ac.iie.pkcgroup.dws.response.BasicResponse;
import cn.ac.iie.pkcgroup.dws.service.TokenService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

import static cn.ac.iie.pkcgroup.dws.Constants.NORMAL_CODE;

@CrossOrigin(value = "*")
@RestController
@RequestMapping("/dws/file")
@Slf4j
public class FileController {
    private FileHandler fileHandler;
    private TokenService tokenService;

    @Autowired
    public void setFileHandler(FileHandler handler) {
        fileHandler = handler;
    }

    @Autowired
    public void setTokenService(TokenService service) {
        tokenService = service;
    }

    private boolean isNormal(int code) {
        return code == NORMAL_CODE;
    }

    @PostMapping(value = "/embed")
    public ResponseEntity<Object> embed(
            @RequestHeader("X-USER-TOKEN") String token,
            @RequestParam("file") MultipartFile file,
            @RequestParam("fileName") String fileName,
            @RequestParam("shouldReturnedFile") boolean shouldReturnedFile,
            @RequestParam(value = "embeddedMessage", required = false) String embeddedMessage,
            @RequestParam(value = "filePassword", required = false) String filePassword
    ) {
        String systemId = tokenService.parseSystemId(token);
        if (systemId == null) return new ResponseEntity<>(new BasicResponse(StatusCodes.CODE_UNAUTHORIZED_REQUEST, StatusCodes.MSG_UNAUTHORIZED_REQUEST), HttpStatus.OK);
        FileEmbeddingInfo fileEmbeddingInfo = new FileEmbeddingInfo();
        fileEmbeddingInfo.setFileName(fileName);
        try {
            fileEmbeddingInfo.setSourceFile(file.getInputStream());
        } catch (IOException e) {
            log.error("Uploaded file should not be null.", e);
        }
        boolean isOwner = tokenService.isOwner(token);
        fileEmbeddingInfo.setOwner(isOwner);
        if (embeddedMessage == null) {
            fileEmbeddingInfo.setEmbeddingMessage(tokenService.parseSystemName(token));
        } else {
            fileEmbeddingInfo.setEmbeddingMessage(embeddedMessage);
        }
        if (filePassword != null) fileEmbeddingInfo.setFilePassword(filePassword);
        fileEmbeddingInfo.setSystemId(systemId);
        FileResponse fileResponse = fileHandler.embed(fileEmbeddingInfo);
        if (!isNormal(fileResponse.getStatusCode())) {
            return new ResponseEntity<>(fileResponse, HttpStatus.OK);
        }
        if (shouldReturnedFile) {
            try {
                byte[] dwBytes = Files.readAllBytes(Paths.get(fileEmbeddingInfo.getOutputPath()));
                return ResponseEntity.status(HttpStatus.OK)
                        .contentType(MediaType.parseMediaType("application/octet-stream"))
                        .body(dwBytes);
            } catch (IOException e) {
                log.error("Return dw file fail, due to IO exception.");
                FileEmbedResponse fileEmbedResponse = FileEmbedResponse.builder()
                        .statusCode(fileResponse.getStatusCode())
                        .message(fileResponse.getMessage())
                        .build();
                if (fileResponse.getDwFileId() == null) {
                    log.error("Internal error: cannot generate dw file ID.");
                    return new ResponseEntity<>(fileEmbedResponse, HttpStatus.OK);
                }
                fileEmbedResponse.setDwFileId(fileResponse.getDwFileId());
                return new ResponseEntity<>(fileEmbedResponse, HttpStatus.OK);
            }
        } else {
            FileEmbedResponse fileEmbedResponse = FileEmbedResponse.builder()
                    .statusCode(fileResponse.getStatusCode())
                    .message(fileResponse.getMessage())
                    .build();
            if (fileResponse.getDwFileId() == null) {
                log.error("Internal error: cannot generate dw file ID.");
                return new ResponseEntity<>(fileEmbedResponse, HttpStatus.OK);
            }
            fileEmbedResponse.setDwFileId(fileResponse.getDwFileId());
            return new ResponseEntity<>(fileEmbedResponse, HttpStatus.OK);
        }
    }

    /**
     * admin only
     */
    @PostMapping(value = "/extract")
    public BasicResponse extract(
            @RequestHeader("X-USER-TOKEN") String token,
            @RequestParam("systemId") String systemId,
            @RequestParam("file") MultipartFile file,
            @RequestParam("fileName") String fileName,
            @RequestParam(value = "filePassword", required = false) String filePassword
    ) {
        if (!tokenService.isAdmin(token)) return new BasicResponse(StatusCodes.CODE_UNAUTHORIZED_REQUEST, StatusCodes.MSG_UNAUTHORIZED_REQUEST);
        FileExtractInfo fileExtractInfo = new FileExtractInfo();
        fileExtractInfo.setFileName(fileName);
        try {
            fileExtractInfo.setSourceFile(file.getInputStream());
        } catch (IOException e) {
            log.error("Uploaded file should not be null.", e);
        }
        if (filePassword != null) fileExtractInfo.setFilePassword(filePassword);
        fileExtractInfo.setSystemId(systemId);
        FileResponse fileResponse = fileHandler.extract(fileExtractInfo);
        ExtractResponse extractResponse = ExtractResponse.builder()
                .statusCode(fileResponse.getStatusCode())
                .message(fileResponse.getMessage())
                .build();
        if (!isNormal(fileResponse.getStatusCode())) {
            return extractResponse;
        }
        extractResponse.setExtractedMessage(fileResponse.getExtractedMessage());

        return extractResponse;
    }

    @PostMapping(value = "/upload")
    public BasicResponse upload(
            @RequestHeader("X-USER-TOKEN") String token,
            @RequestParam("file") MultipartFile file,
            @RequestParam("fileName") String fileName
    ) {
        String systemId = tokenService.parseSystemId(token);
        if (systemId == null) return new BasicResponse(StatusCodes.CODE_UNAUTHORIZED_REQUEST, StatusCodes.MSG_UNAUTHORIZED_REQUEST);
        FileResponse fileResponse = fileHandler.saveFile(systemId, fileName, file);
        FileUploadResponse fileUploadResponse = FileUploadResponse.builder()
                .statusCode(fileResponse.getStatusCode())
                .message(fileResponse.getMessage())
                .build();
        if (!isNormal(fileResponse.getStatusCode())) {
            return fileUploadResponse;
        }
        fileUploadResponse.setFileId(fileResponse.getFileId());
        return fileUploadResponse;
    }

    @GetMapping(value = "/download")
    public ResponseEntity<Object> download(
            @RequestHeader("X-USER-TOKEN") String token,
            @RequestParam("fileId") String fileId,
            @RequestParam("fileName") String fileName,
            @RequestParam(value = "filePassword", required = false) String filePassword
    ) {
        String systemId = tokenService.parseSystemId(token);
        if (systemId == null) return new ResponseEntity<>(new BasicResponse(StatusCodes.CODE_UNAUTHORIZED_REQUEST, StatusCodes.MSG_UNAUTHORIZED_REQUEST), HttpStatus.OK);
        String userNickname = tokenService.parseUserNickname(token);
        if (userNickname == null) return new ResponseEntity<>(new BasicResponse(StatusCodes.CODE_INTERNAL_ERROR, StatusCodes.MSG_INTERNAL_ERROR), HttpStatus.OK);

        boolean isOwner = true;
        FileResponse fileResponse;
        // Owner: "", User: nickname
        if (!userNickname.equals("")) {
            isOwner = false;
            FileEmbeddingInfo fileEmbeddingInfo = new FileEmbeddingInfo();
            fileEmbeddingInfo.setFileName(fileName);
            fileEmbeddingInfo.setFileId(fileId);
            fileEmbeddingInfo.setOwner(false);
            fileEmbeddingInfo.setEmbeddingMessage(userNickname);
            if (filePassword != null) fileEmbeddingInfo.setFilePassword(filePassword);
            fileEmbeddingInfo.setSystemId(systemId);
            fileResponse = fileHandler.embed(fileEmbeddingInfo);
        } else {
            fileResponse = fileHandler.getFile(systemId, fileId, fileName);
        }
        if (!isNormal(fileResponse.getStatusCode())) {
            return new ResponseEntity<>(fileResponse, HttpStatus.OK);
        }
        byte[] fileBytes;
        try {
            fileBytes = Files.readAllBytes(Paths.get(fileResponse.getFilePath()));
            if (!isOwner) {
                if (fileHandler.removeTempFile(fileResponse.getFilePath())) {
                    log.info("Finish embedding fingerprint.");
                }
            }
            return ResponseEntity.status(HttpStatus.OK)
                    .contentType(MediaType.parseMediaType("application/octet-stream"))
                    .body(fileBytes);
        } catch (IOException e) {
            log.error("Unhandled IO errors.");
            return new ResponseEntity<>(new BasicResponse(StatusCodes.CODE_INTERNAL_ERROR, StatusCodes.MSG_INTERNAL_ERROR), HttpStatus.OK);
        }
    }

    @GetMapping(value = "/fileList")
    public BasicResponse getFileList(
            @RequestHeader("X-USER-TOKEN") String token
    ) {
        String systemId = tokenService.parseSystemId(token);
        if (systemId == null) return new BasicResponse(StatusCodes.CODE_UNAUTHORIZED_REQUEST, StatusCodes.MSG_UNAUTHORIZED_REQUEST);
        return fileHandler.getFileListBySystemId(systemId);
    }
}
