package cn.ac.iie.pkcgroup.dws.core.file.response;

import cn.ac.iie.pkcgroup.dws.response.BasicResponse;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.experimental.SuperBuilder;

import java.io.File;

@AllArgsConstructor
@Data
@SuperBuilder
public class FileResponse extends BasicResponse {
    String extractedMessage;
    String fileId;
    String dwFileId;
    String filePath;
    String fileName;

    public FileResponse(int code, String msg) {
        super(code, msg);
    }
}
