package cn.ac.iie.pkcgroup.dws.core.db.utils;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.csv.CSVRecord;
import org.apache.commons.io.input.BOMInputStream;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

@Data
@Slf4j
@NoArgsConstructor
public class CSVUtils {
    final static char DELIMITER = ',';
    private String fileName;
    private ArrayList<String> headerNames;

    public CSVUtils(String fileName) {
        this.fileName = fileName;
    }

    public CSVUtils(String fileName, ArrayList<String> headerNames) {
        this.fileName = fileName;
        this.headerNames = headerNames;
    }

    public boolean exportCSV(ArrayList<ArrayList<String>> collection, String[] headers, boolean isAppend) {
        try {
            FileOutputStream fileOutputStream = new FileOutputStream(fileName, isAppend);
            OutputStreamWriter outputStreamWriter = new OutputStreamWriter(fileOutputStream, StandardCharsets.UTF_8);
            CSVFormat csvFormat;
            if (!isAppend) {
                csvFormat = CSVFormat.EXCEL.withDelimiter(DELIMITER).withHeader(headers);
            } else {
                csvFormat = CSVFormat.EXCEL.withDelimiter(DELIMITER);
            }
            CSVPrinter csvPrinter = new CSVPrinter(outputStreamWriter, csvFormat);
            for (ArrayList<String> record:
                 collection) {
                csvPrinter.printRecord(record);
            }
            csvPrinter.close();
        } catch (FileNotFoundException e) {
            log.error("CSV path does not exist.");
            return false;
        } catch (IOException e) {
            log.error("Cannot open CSV printer due to IO Exception.");
            return false;
        }
        return true;
    }

    public CSVInfo parseCSV(MultipartFile csvFile) {
        CSVFormat csvFormat = CSVFormat.EXCEL.withDelimiter(DELIMITER).withFirstRecordAsHeader();
        try {
            CSVInfo csvInfo = new CSVInfo();
            InputStreamReader inputStreamReader = new InputStreamReader(new BOMInputStream(csvFile.getInputStream()));
            CSVParser csvParser = CSVParser.parse(inputStreamReader, csvFormat);
            List<CSVRecord> records = csvParser.getRecords();
            ArrayList<String> headers = new ArrayList<>(csvParser.getHeaderMap().keySet());
            csvInfo.setHeaders(headers);

            ArrayList<ArrayList<String>> data = new ArrayList<>();
            for (CSVRecord record:
                 records) {
                ArrayList<String> singleRecord = new ArrayList<>();
                for (String s : record) {
                    singleRecord.add(s);
                }
                data.add(singleRecord);
            }
            csvInfo.setData(data);
            return csvInfo;
        } catch (IOException e) {
            log.error("Parse CSV error. ", e);
            return null;
        }
    }

    @Data
    public static class CSVInfo {
        ArrayList<String> headers;
        ArrayList<ArrayList<String>> data;
    }
}
