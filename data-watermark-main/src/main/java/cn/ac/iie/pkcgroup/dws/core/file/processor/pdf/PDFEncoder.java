package cn.ac.iie.pkcgroup.dws.core.file.processor.pdf;

import cn.ac.iie.pkcgroup.dws.core.WatermarkException;
import cn.ac.iie.pkcgroup.dws.core.file.interfaces.IEncoder;
import cn.ac.iie.pkcgroup.dws.core.file.model.FileEmbeddingInfo;
import com.itextpdf.text.BaseColor;
import com.itextpdf.text.DocumentException;
import com.itextpdf.text.Element;
import com.itextpdf.text.pdf.*;
import lombok.extern.slf4j.Slf4j;

import java.io.BufferedOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

@Slf4j
public class PDFEncoder implements IEncoder {

    @Override
    public void encode(FileEmbeddingInfo fileEmbeddingInfo) throws WatermarkException {
        String embeddedMessage = fileEmbeddingInfo.getEmbeddingMessage();
        String outputPath = fileEmbeddingInfo.getOutputPath();
        InputStream sourceFile = fileEmbeddingInfo.getSourceFile();
        try {
            FileOutputStream fileOutputStream = new FileOutputStream(outputPath);
            BufferedOutputStream bufferedOutputStream = new BufferedOutputStream(fileOutputStream);
            PdfReader reader = new PdfReader(sourceFile);
            PdfStamper stamper = new PdfStamper(reader, bufferedOutputStream);
            int total = reader.getNumberOfPages() + 1;
            PdfContentByte content;
            BaseFont base = BaseFont.createFont("STSong-Light", "UniGB-UCS2-H", BaseFont.EMBEDDED);
            PdfGState gs = new PdfGState();
            for (int i = 1; i < total; i++) {
                content = stamper.getUnderContent(i);//在内容下方加水印
                gs.setFillOpacity(0.2f);
                // content.setGState(gs);
                content.beginText();
                content.setColorFill(BaseColor.LIGHT_GRAY);
                int rotation = 45;
                int fontSize = 50;
                int x = 300, y = 350;
                if (!fileEmbeddingInfo.isOwner()) {
                    rotation = 0;
                    x = 50;
                    y = 10;
                    fontSize = 10;
                }
                content.setFontAndSize(base, fontSize);
                content.setTextMatrix(70, 200);
                content.showTextAligned(Element.ALIGN_CENTER, embeddedMessage, x, y, rotation);
                content.endText();

            }
            stamper.close();
        } catch (IOException | DocumentException e) {
            log.error("PDF encoder fails to read file.", e);
            throw new WatermarkException("PDF encoder fails to read file.");
        }
    }
}
