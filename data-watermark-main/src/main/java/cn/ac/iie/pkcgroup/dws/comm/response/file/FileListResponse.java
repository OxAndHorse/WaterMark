package cn.ac.iie.pkcgroup.dws.comm.response.file;

import cn.ac.iie.pkcgroup.dws.response.BasicResponse;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.SuperBuilder;

import java.util.ArrayList;

@EqualsAndHashCode(callSuper = true)
@Data
@SuperBuilder
public class FileListResponse extends BasicResponse {
    int fileCount;
    ArrayList<FileMetaData> fileList;

    @Data
    public static class FileMetaData {
        String fileName;
        String fileId;
        String fileType;
    }
}
