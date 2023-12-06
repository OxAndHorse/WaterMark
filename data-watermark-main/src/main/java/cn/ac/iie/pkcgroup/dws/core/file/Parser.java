package cn.ac.iie.pkcgroup.dws.core.file;

import cn.ac.iie.pkcgroup.dws.core.file.interfaces.IDecoder;
import cn.ac.iie.pkcgroup.dws.core.file.interfaces.IEncoder;
import cn.ac.iie.pkcgroup.dws.core.file.model.FileEmbeddingInfo;
import cn.ac.iie.pkcgroup.dws.core.file.model.FileExtractInfo;
import cn.ac.iie.pkcgroup.dws.core.file.processor.pdf.PDFEncoder;
import cn.ac.iie.pkcgroup.dws.core.file.processor.word.WordDecoder;
import cn.ac.iie.pkcgroup.dws.core.file.processor.word.WordEncoder;

public class Parser {
    // TODO: Should be more accurate.
    public static String checkType(String fileName) {
        String[] list = fileName.split("\\.");
        String suffix = list[list.length - 1];
        switch (suffix) {
            case "pdf":
            case "PDF":
                return Constants.TYPE_PDF;
            case "docx":
            case "DOCX":
                return Constants.TYPE_WORD_X;
            default:
                return Constants.TYPE_NOT_SUPPORTED;
        }
    }

    public static IEncoder parseEncoder(FileEmbeddingInfo fileEmbeddingInfo) {
        String type = checkType(fileEmbeddingInfo.getFileName());
        switch (type) {
            case Constants.TYPE_PDF:
                return new PDFEncoder();
            case Constants.TYPE_WORD_X:
                return new WordEncoder();
            case Constants.TYPE_NOT_SUPPORTED:
            default:
                return null;
        }
    }

    public static IDecoder parseDecoder(FileExtractInfo fileExtractInfo) {
        String type = checkType(fileExtractInfo.getFileName());
        switch (type) {
            case Constants.TYPE_WORD_X:
                return new WordDecoder();
            case Constants.TYPE_NOT_SUPPORTED:
            default:
                return null;
        }
    }
}
