package cn.ac.iie.pkcgroup.dws.route.db;

import cn.ac.iie.pkcgroup.dws.comm.request.db.DBEmbedRequest;
import cn.ac.iie.pkcgroup.dws.comm.request.db.DBExtractRequest;
import cn.ac.iie.pkcgroup.dws.comm.request.db.SelectedField;
import cn.ac.iie.pkcgroup.dws.comm.response.ExtractResponse;
import cn.ac.iie.pkcgroup.dws.comm.response.StatusCodes;
import cn.ac.iie.pkcgroup.dws.comm.response.db.*;
import cn.ac.iie.pkcgroup.dws.comm.response.db.entity.AlgorithmInfo;
import cn.ac.iie.pkcgroup.dws.core.db.Constants;
import cn.ac.iie.pkcgroup.dws.core.db.DBHandler;
import cn.ac.iie.pkcgroup.dws.core.db.model.EmbeddingInfo;
import cn.ac.iie.pkcgroup.dws.core.db.model.ExtractInfo;
import cn.ac.iie.pkcgroup.dws.core.db.response.DBResponse;
import cn.ac.iie.pkcgroup.dws.response.BasicResponse;
import cn.ac.iie.pkcgroup.dws.service.DatabaseService;
import cn.ac.iie.pkcgroup.dws.service.TokenService;
import cn.ac.iie.pkcgroup.dws.service.response.DBBasicResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.validation.Valid;

import java.util.ArrayList;
import java.util.Arrays;

import static cn.ac.iie.pkcgroup.dws.Constants.FILE_NAME_HEADER;
import static cn.ac.iie.pkcgroup.dws.Constants.NORMAL_CODE;

@CrossOrigin(value = "*")
@RestController
@Slf4j
@RequestMapping("/dws/db")
public class DatabaseController {
    //    private TableMap tableMap;
    private DBHandler dbHandler;
    private TokenService tokenService;
    private DatabaseService databaseService;
    private static final String OWNER_PREFIX = "版权 ";
    private static final String USER_PREFIX = "导出者 ";

//    @Autowired
//    public void setTableMap(TableMap map) {
//        tableMap = map;
//    }

    @Autowired
    public void setDbHandler(DBHandler handler) {
        dbHandler = handler;
    }

    @Autowired
    public void setTokenService(TokenService service) {
        tokenService = service;
    }

    @Autowired
    public void setDatabaseService(DatabaseService service) {
        databaseService = service;
    }

    private boolean isNormal(int code) {
        return code == NORMAL_CODE;
    }


    @GetMapping(value = "/tableList")
    public BasicResponse getTableList(
            @RequestHeader("X-USER-TOKEN") String token,
            @RequestParam("dbName") String dbName
    ) {
        String systemId = tokenService.parseSystemId(token);
        if (systemId == null)
            return new BasicResponse(StatusCodes.CODE_UNAUTHORIZED_REQUEST, StatusCodes.MSG_UNAUTHORIZED_REQUEST);
        DBBasicResponse dbBasicResponse = databaseService.getTableListByDBName(systemId, dbName);
        if (!isNormal(dbBasicResponse.getStatusCode())) return dbBasicResponse;
        return TableListResponse.builder()
                .statusCode(dbBasicResponse.getStatusCode())
                .message(dbBasicResponse.getMessage())
                .tableInfoList(dbBasicResponse.getSimpleTableInfos())
                .build();
    }

    @GetMapping(value = "/tableData")
    public BasicResponse getTableData(
            @RequestHeader("X-USER-TOKEN") String token,
            @RequestParam("dbName") String dbName,
            @RequestParam("tableName") String tableName,
            @RequestParam("page") int page,
            @RequestParam("pageCount") int pageCount

    ) {
        String systemId = tokenService.parseSystemId(token);
        if (systemId == null)
            return new BasicResponse(StatusCodes.CODE_UNAUTHORIZED_REQUEST, StatusCodes.MSG_UNAUTHORIZED_REQUEST);
        if (page < 0 || pageCount < 0)
            return new BasicResponse(StatusCodes.CODE_ILLEGAL_PARAMS, StatusCodes.MSG_ILLEGAL_PARAMS);
        DBBasicResponse dbBasicResponse = databaseService.getDataByDBNameAndTableName(systemId, dbName, tableName, page, pageCount);
        // TODO: cache table info into selectedTable mapping.
        if (!isNormal(dbBasicResponse.getStatusCode())) return dbBasicResponse;
        return TableDataResponse.builder()
                .statusCode(dbBasicResponse.getStatusCode())
                .message(dbBasicResponse.getMessage())
                .dataCount(dbBasicResponse.getTotalCount())
                .dataList(dbBasicResponse.getPagedDataSet())
                .build();
    }

    @GetMapping(value = "/embeddedTableList")
    public BasicResponse getEmbeddedTableList(
            @RequestHeader("X-USER-TOKEN") String token,
            @RequestParam("selectedSystemId") String selectedSystemId

    ) {
        if (!tokenService.isAdmin(token))
            return new BasicResponse(StatusCodes.CODE_UNAUTHORIZED_REQUEST, StatusCodes.MSG_UNAUTHORIZED_REQUEST);
//        String systemId = tokenService.parseSystemId(token);
//        if (systemId == null) return new BasicResponse(StatusCodes.CODE_UNAUTHORIZED_REQUEST, StatusCodes.MSG_UNAUTHORIZED_REQUEST);
        DBResponse dbResponse = dbHandler.getEmbeddedTableList(selectedSystemId);
        if (!isNormal(dbResponse.getStatusCode())) {
            return dbResponse;
        }
        return EmbeddedTableListResponse.builder()
                .statusCode(StatusCodes.CODE_SUCCESS)
                .message(StatusCodes.MSG_SUCCESS)
                .embeddedTableList(dbResponse.getEmbeddedTableList())
                .build();
    }

    @PostMapping(value = "/embed/full")
    public BasicResponse embedWatermarkForFullDataset(
            @RequestHeader("X-USER-TOKEN") String token,
            @RequestBody @Valid DBEmbedRequest dbEmbedRequest
    ) {
        String systemId = tokenService.parseSystemId(token);
        if (systemId == null)
            return new BasicResponse(StatusCodes.CODE_UNAUTHORIZED_REQUEST, StatusCodes.MSG_UNAUTHORIZED_REQUEST);
        DBEmbedResponse dbEmbedResponse = DBEmbedResponse.builder()
                .statusCode(StatusCodes.CODE_SUCCESS)
                .message(StatusCodes.MSG_SUCCESS)
                .build();
        boolean isOwner = tokenService.isOwner(token);
        SelectedField[] selectedFields = dbEmbedRequest.getSelectedFields();
        String embeddedMessage = dbEmbedRequest.getEmbeddedMessage();
        String message;
        if (embeddedMessage == null) {
            String systemName = tokenService.parseSystemName(token);
            message = isOwner ? OWNER_PREFIX + systemName : USER_PREFIX + systemName;
        } else {
            message = embeddedMessage;
        }
        ArrayList<SelectedField> selectedFieldArrayList = selectedFields != null ? new ArrayList<>(Arrays.asList(selectedFields)) : new ArrayList<>();
        EmbeddingInfo embeddingInfo = new EmbeddingInfo();
        embeddingInfo.setSystemId(systemId);
        embeddingInfo.setDbName(dbEmbedRequest.getDbName());
        embeddingInfo.setTableName(dbEmbedRequest.getTableName());
        embeddingInfo.setSelectedFields(selectedFieldArrayList);
        embeddingInfo.setQuerySql(dbEmbedRequest.getQuerySql());
        embeddingInfo.setEmbeddingMessage(message);
        embeddingInfo.setOwner(isOwner);
        embeddingInfo.setShouldOutputToDB(dbEmbedRequest.isShouldOutputToDB());
        embeddingInfo.setAllowRowExpansion(dbEmbedRequest.isAllowRowExpansion());
        embeddingInfo.setAllowColumnExpansion(dbEmbedRequest.isAllowColumnExpansion());
        String outputTable = dbEmbedRequest.getOutputTable();
        if (outputTable != null)
            embeddingInfo.setOutputTable(outputTable);
        DBResponse dbResponse = dbHandler.embed(embeddingInfo);
        if (!isNormal(dbResponse.getStatusCode())) {
            return dbResponse;
        }
        // TODO: how to process multiple datasets
        dbEmbedResponse.setDataList(dbResponse.getDataSet());
        dbEmbedResponse.setDataCount(dbResponse.getDataSet().size());
        dbEmbedResponse.setDataId(dbResponse.getDataId());
        return dbEmbedResponse;
    }

    @GetMapping(value = "/embed/csv")
    public ResponseEntity<Object> downloadCSVFile(
            @RequestHeader("X-USER-TOKEN") String token,
            @RequestParam("dataId") String dataId
    ) {
        String systemId = tokenService.parseSystemId(token);
        if (systemId == null)
            return new ResponseEntity<>(new BasicResponse(StatusCodes.CODE_UNAUTHORIZED_REQUEST, StatusCodes.MSG_UNAUTHORIZED_REQUEST), HttpStatus.OK);
        DBResponse dbResponse = dbHandler.getCSVFile(systemId, dataId);
        if (isNormal(dbResponse.getStatusCode())) {
            HttpHeaders headers = new HttpHeaders();
            headers.set(FILE_NAME_HEADER, dbResponse.getCsvFileName());
            headers.set("Access-Control-Expose-Headers", FILE_NAME_HEADER);
            return ResponseEntity.status(HttpStatus.OK)
                    .contentType(MediaType.parseMediaType("application/octet-stream"))
                    .headers(headers)
                    .body(dbResponse.getCsvFile());
        }
        return new ResponseEntity<>(dbResponse, HttpStatus.OK);
    }

    /**
     * admin only
     */
    @PostMapping(value = "/extract/table")
    public BasicResponse traceWatermarkFromTable(
            @RequestHeader("X-USER-TOKEN") String token,
            @RequestBody @Valid DBExtractRequest dbExtractRequest
    ) {
        if (!tokenService.isAdmin(token))
            return new BasicResponse(StatusCodes.CODE_UNAUTHORIZED_REQUEST, StatusCodes.MSG_UNAUTHORIZED_REQUEST);
        String systemId = dbExtractRequest.getSystemId();
        ArrayList<String> extractedMessages = new ArrayList<>();
        SelectedField[] selectedFields = dbExtractRequest.getSelectedFields();
        for (SelectedField field :
                selectedFields) {
            ExtractInfo extractInfo = new ExtractInfo();
            extractInfo.setEmbeddingColumnName(field.getFieldName());
            if (field.getAlgorithm() != null)
                extractInfo.setEmbeddingMethod(field.getAlgorithm());
            extractInfo.setSystemId(systemId);
            extractInfo.setDbName(dbExtractRequest.getDbName());
            extractInfo.setTableName(dbExtractRequest.getTableName());

            DBResponse dbResponse = dbHandler.extractFromTable(extractInfo);
            if (!isNormal(dbResponse.getStatusCode())) {
                return dbResponse;
            }
            extractedMessages.add(dbResponse.getExtractedMessage());
        }
        StringBuilder extractedMessage = new StringBuilder();
        for (String message :
                extractedMessages) {
            extractedMessage.append(message).append(",");
        }
        extractedMessage.deleteCharAt(extractedMessage.length() - 1);
        return ExtractResponse.builder()
                .statusCode(StatusCodes.CODE_SUCCESS)
                .message(StatusCodes.MSG_SUCCESS)
                .extractedMessage(extractedMessage.toString())
                .build();
    }

    /**
     * admin only
     */
    @PostMapping(value = "/extract/csv")
    public BasicResponse traceWatermarkFromCSVFile(
            @RequestHeader("X-USER-TOKEN") String token,
            @RequestParam("csvFile") MultipartFile file,
            @RequestParam("tableName") String tableName,
            @RequestParam("dbName") String dbName,
            @RequestParam("systemId") String systemId
    ) {
        if (!tokenService.isAdmin(token))
            return new BasicResponse(StatusCodes.CODE_UNAUTHORIZED_REQUEST, StatusCodes.MSG_UNAUTHORIZED_REQUEST);
        ArrayList<String> extractedMessages = new ArrayList<>();
        ExtractInfo extractInfo = new ExtractInfo();
        extractInfo.setSystemId(systemId);
        extractInfo.setDbName(dbName);
        extractInfo.setTableName(tableName);
        extractInfo.setFile(file);

        DBResponse dbResponse = dbHandler.extractFromCSV(extractInfo);
        if (!isNormal(dbResponse.getStatusCode())) {
            return dbResponse;
        }
        extractedMessages.add(dbResponse.getExtractedMessage());
//        }
        StringBuilder extractedMessage = new StringBuilder();
        for (String message :
                extractedMessages) {
            extractedMessage.append(message).append(",");
        }
        extractedMessage.deleteCharAt(extractedMessage.length() - 1);
        return ExtractResponse.builder()
                .statusCode(StatusCodes.CODE_SUCCESS)
                .message(StatusCodes.MSG_SUCCESS)
                .extractedMessage(extractedMessage.toString())
                .build();
    }

    @Deprecated
    @GetMapping(value = "/algorithms")
    public BasicResponse getWatermarkAlgorithms(
            @RequestHeader("X-USER-TOKEN") String token
    ) {
        if (tokenService.validateToken(token) < 0)
            return new BasicResponse(StatusCodes.CODE_UNAUTHORIZED_REQUEST, StatusCodes.MSG_UNAUTHORIZED_REQUEST);
        ArrayList<AlgorithmInfo> algorithmInfos = Constants.ALGORITHM_INFOS;
        return AlgorithmListResponse.builder()
                .statusCode(StatusCodes.CODE_SUCCESS)
                .message(StatusCodes.MSG_SUCCESS)
                .algorithmCount(algorithmInfos.size())
                .algorithmList(algorithmInfos)
                .build();
    }
}
