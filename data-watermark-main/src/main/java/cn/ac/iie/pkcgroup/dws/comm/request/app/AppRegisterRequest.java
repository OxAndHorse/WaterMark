package cn.ac.iie.pkcgroup.dws.comm.request.app;

import lombok.Data;

import javax.validation.constraints.NotNull;

@Data
public class AppRegisterRequest {
    @NotNull(message = "systemName should not be null.")
    String systemName;
    String systemNickname;
    @NotNull(message = "dwUsage should not be null.")
    int dwUsage; // 0: watermark, 1: fingerprint, 2: all
    @NotNull(message = "functions should not be null.")
    int functions; // 0: file, 1: db, 2: all
    DBParams dbParams;

    @Data
    public static class DBParams {
        String dbName;
        String dbUser;
        String dbPassword;
        String dbIP;
        String dbPort;
        String dbType;
    }
}
