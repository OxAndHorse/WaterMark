package cn.ac.iie.pkcgroup.dws.core.file.processor.word;

import cn.ac.iie.pkcgroup.dws.core.WatermarkException;
import cn.ac.iie.pkcgroup.dws.core.file.Constants;
import cn.ac.iie.pkcgroup.dws.core.file.interfaces.IDecoder;
import cn.ac.iie.pkcgroup.dws.core.file.model.FileExtractInfo;
import cn.ac.iie.pkcgroup.dws.core.file.utils.TempFileUtils;
import lombok.extern.slf4j.Slf4j;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.File;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Map;

import static cn.ac.iie.pkcgroup.dws.core.file.processor.word.XMLProcessor.findTargetNode;
import static cn.ac.iie.pkcgroup.dws.core.file.processor.word.XMLProcessor.transferDocxToXML;

@Slf4j
public class WordDecoder implements IDecoder {
    private final Map<Character, Integer> decodingMap = Constants.WORD_DECODING_MAP;
    private final int unit = Constants.WORD_UNIT;

    private String parseWatermark(String watermark) {
        char[] watermarkChars = new char[watermark.length()];
        watermark.getChars(0, watermark.length(), watermarkChars, 0);

        ByteBuffer byteBuffer = ByteBuffer.allocate(watermark.length() * unit / Byte.SIZE);
        int lowByte = 0;
        int i;
        int boundary = Byte.SIZE / unit;
        for (int j = 0; j < watermarkChars.length; ++j) {
            i = j % boundary;
            lowByte += decodingMap.get(watermarkChars[j]) << (i * unit);
            if (i == boundary - 1) {
                byteBuffer.put((byte) lowByte);
                lowByte = 0;
            }
        }
        return new String(byteBuffer.array(), StandardCharsets.UTF_8);
    }

    @Override
    public String decode(FileExtractInfo fileExtractInfo) throws WatermarkException {
        String xmlPath = TempFileUtils.generateTempFilePath(fileExtractInfo.getTmpRootPath(), ".xml");
        transferDocxToXML(fileExtractInfo.getSourceFile(), xmlPath);
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        try {
            DocumentBuilder db = dbf.newDocumentBuilder();
            org.w3c.dom.Document document = db.parse(new File(xmlPath));

            Element root = document.getDocumentElement();
            NodeList children = root.getChildNodes();
            ArrayList<Node> nodeArrayList = findTargetNode(children, false);
            if (nodeArrayList.isEmpty()) throw new WatermarkException("No matched DOM node.");
            StringBuilder sb = new StringBuilder();
            for (Node dsItemID:
                 nodeArrayList) {
                String watermark = dsItemID.getNodeValue();
                if (watermark == null) throw new WatermarkException("No watermark found.");
                sb.append(parseWatermark(watermark)).append(",");
            }
            if (new File(xmlPath).delete()) {
                log.info("Clean temp files.");
            }
            sb.deleteCharAt(sb.length() - 1);
            return sb.toString();
        } catch (Exception e) {
            log.error("Extract error.", e);
            throw new WatermarkException("Extract error: " + e.getMessage());
        }
    }
}
