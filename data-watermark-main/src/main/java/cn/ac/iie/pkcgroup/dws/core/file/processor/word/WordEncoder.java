package cn.ac.iie.pkcgroup.dws.core.file.processor.word;

import cn.ac.iie.pkcgroup.dws.core.WatermarkException;
import cn.ac.iie.pkcgroup.dws.core.file.Constants;
import cn.ac.iie.pkcgroup.dws.core.file.interfaces.IEncoder;
import cn.ac.iie.pkcgroup.dws.core.file.model.FileEmbeddingInfo;
import cn.ac.iie.pkcgroup.dws.core.file.utils.TempFileUtils;
import com.spire.doc.*;
import com.spire.doc.documents.MarginsF;
import com.spire.doc.documents.Paragraph;
import com.spire.doc.documents.ShapeLineStyle;
import com.spire.doc.documents.ShapeType;
import com.spire.doc.fields.ShapeObject;
import lombok.extern.slf4j.Slf4j;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.awt.*;
import java.io.File;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;

import static cn.ac.iie.pkcgroup.dws.core.file.Constants.*;
import static cn.ac.iie.pkcgroup.dws.core.file.processor.word.XMLProcessor.*;

@Slf4j
public class WordEncoder implements IEncoder {
    private final String[] encodingMap = Constants.WORD_ENCODING_MAP;
    private final int unit = Constants.WORD_UNIT;

    private String generateWatermark(String material) {
        byte[] bytes = material.getBytes(StandardCharsets.UTF_8);
        StringBuilder sb = new StringBuilder();
        for (byte aByte : bytes) {
            int b = aByte & 0xff;
            int rotation = 0x01; // 00000011
            for (int j = 0; j < Byte.SIZE / unit; ++j) {
                int bb = b & rotation;
                sb.append(encodingMap[bb]);
                b = b >>> unit;
            }
        }
        return sb.toString();
    }

    private void addVisibleWatermark(String wordPath, String watermark, boolean isOwner) {
        int wordCount = watermark.length();
        Document doc = new Document();
        doc.loadFromFile(wordPath);

        Section section;
        HeaderFooter headerFooter;
        for (int n = 0; n < doc.getSections().getCount(); n++) {
            section = doc.getSections().get(n);
            PageSetup pageSetup = section.getPageSetup();
            double pageWidth = pageSetup.getPageSize().getWidth();
            double pageHeight = pageSetup.getPageSize().getHeight();
            MarginsF margins = pageSetup.getMargins();
            ShapeObject shape;
            Paragraph paragraph;
            //获取section的页眉
            if (isOwner) {
                headerFooter = section.getHeadersFooters().getHeader();
                if (headerFooter.getLinkToPrevious()) { // 链接到前一节
                    continue;
                }

                paragraph = headerFooter.addParagraph();
                //添加艺术字并设置大小
                float width = wordCount * WORD_VISIBLE_WORD_WIDTH;
                float height = WORD_VISIBLE_WORD_HEIGHT;
                shape = paragraph.appendShape(width, height, ShapeType.Text_Plain_Text);
                // 居中
                float verticalPos = (float) ((pageHeight - height) / 2 - margins.getTop());
                float horizontalPos = (float) ((pageWidth - width) / 2 - margins.getLeft());
                //设置艺术字文本内容、位置及样式
                shape.setVerticalPosition(verticalPos);
                shape.setHorizontalPosition(horizontalPos);
                shape.setRotation(315);
                shape.setLineStyle(ShapeLineStyle.Single);
            } else {
                headerFooter = section.getHeadersFooters().getFooter();
                if (headerFooter.getLinkToPrevious()) { // 链接到前一节
                    continue;
                }
                paragraph = headerFooter.addParagraph();
                float width = wordCount * WORD_VISIBLE_WORD_FOOT_WIDTH;
                float height = WORD_VISIBLE_WORD_FOOT_HEIGHT;
                shape = paragraph.appendShape(width, height, ShapeType.Text_Plain_Text);
                shape.getWordArt().setSize(WORD_VISIBLE_WORD_FOOT_TEXT_SIZE);
            }
            shape.getWordArt().setFontFamily("宋体");
            shape.getWordArt().setText(watermark);
            shape.setFillColor(Color.gray);
            shape.setStrokeColor(new Color(192, 192, 192, 255));
            shape.setStrokeWeight(1);
        }
        //保存文档
        doc.saveToFile(wordPath, FileFormat.Docx);
    }

    @Override
    public void encode(FileEmbeddingInfo fileEmbeddingInfo) throws WatermarkException {
        String sourceXMLPath = TempFileUtils.generateTempFilePath(fileEmbeddingInfo.getTmpRootPath(), ".xml");
        String embeddedXMLPath = TempFileUtils.generateTempFilePath(fileEmbeddingInfo.getTmpRootPath(), ".xml");

        InputStream inputStream = fileEmbeddingInfo.getSourceFile();
        transferDocxToXML(inputStream, sourceXMLPath);
        String watermark = generateWatermark(fileEmbeddingInfo.getEmbeddingMessage());

        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        try {
            DocumentBuilder db = dbf.newDocumentBuilder();
            org.w3c.dom.Document document = db.parse(new File(sourceXMLPath));

            Element root = document.getDocumentElement();
            NodeList children = root.getChildNodes();
            ArrayList<Node> nodeArrayList = findTargetNode(children, true);
            if (nodeArrayList.isEmpty()) throw new WatermarkException("No matched DOM node.");
            Node dsItemID = nodeArrayList.get(0);
            String targetVal = dsItemID.getNodeValue();
            String newVal = targetVal + watermark;
            dsItemID.setNodeValue(newVal);
            TransformerFactory tff = TransformerFactory.newInstance();
            Transformer tf = tff.newTransformer();
            // 输出内容是否使用换行
            tf.setOutputProperty(OutputKeys.INDENT, "yes");
            // 创建xml文件并写入内容
            tf.transform(new DOMSource(document), new StreamResult(new File(embeddedXMLPath)));
            transferXMLToDocx(embeddedXMLPath, fileEmbeddingInfo.getOutputPath());

            if (fileEmbeddingInfo.isUseVisibleWatermark()) {
                addVisibleWatermark(fileEmbeddingInfo.getOutputPath(), fileEmbeddingInfo.getEmbeddingMessage(), fileEmbeddingInfo.isOwner());
            }

            // clean temp files
            if (new File(sourceXMLPath).delete() && new File(embeddedXMLPath).delete()) {
                log.info("Clean temp files.");
            }
        } catch (Exception e) {
            log.error("Embed error.", e);
            throw new WatermarkException("Embed error: " + e.getMessage());
        }
    }
}
