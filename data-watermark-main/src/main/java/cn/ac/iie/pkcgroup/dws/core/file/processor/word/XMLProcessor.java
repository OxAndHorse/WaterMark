package cn.ac.iie.pkcgroup.dws.core.file.processor.word;

import com.spire.doc.Document;
import com.spire.doc.FileFormat;
import lombok.extern.slf4j.Slf4j;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;

import static cn.ac.iie.pkcgroup.dws.core.file.Constants.WORD_ONE;
import static cn.ac.iie.pkcgroup.dws.core.file.Constants.WORD_ZERO;

@Slf4j
public class XMLProcessor {
    public static void transferDocxToXML(InputStream docxInputStream, String xmlFilePath) {
        Document document = new Document();
        document.loadFromStream(docxInputStream, FileFormat.Docx);
        document.saveToFile(xmlFilePath, FileFormat.Word_Xml);
        document.dispose();
    }

    public static void transferXMLToDocx(String xmlFilePath, String docxFile) {
        Document document = new Document();
        document.loadFromFile(xmlFilePath);
        document.saveToFile(docxFile, FileFormat.Docx);
        document.dispose();
    }

    private static boolean isInnerEncoding(String message) {
        char[] chars = new char[message.length()];
        if (message.isEmpty()) return false;
        message.getChars(0, message.length(), chars, 0);
        return WORD_ZERO.equals(chars[0]) || WORD_ONE.equals(chars[0]);
    }

    public static ArrayList<Node> findTargetNode(NodeList children, boolean isEmbedding) {
        ArrayList<Node> nodeArrayList = new ArrayList<>(1);
        for (int i = 0; i < children.getLength(); ++i) {
            Node node = children.item(i);
            if (node != null && node.getNodeName().equals("pkg:part") && node.hasChildNodes()) {
                NamedNodeMap nodeMap = node.getAttributes();
                if (nodeMap == null || nodeMap.getLength() == 0) continue;
                Node pkgNameNode = nodeMap.getNamedItem("pkg:name");
                if (pkgNameNode == null) continue;
                if (!pkgNameNode.getNodeValue().contains("itemProps")) continue;
                NodeList pkgPartChildNodes = node.getChildNodes();
                int ii = -1;
                for (int j = 0; j < pkgPartChildNodes.getLength(); ++j) {
                    if (pkgPartChildNodes.item(j).getNodeName().contains("pkg:xmlData")) {
                        ii = j;
                        break;
                    }
                }
                if (ii < 0) break;
                NodeList pkgXmlDataChildNodes = pkgPartChildNodes.item(ii).getChildNodes();
                if (pkgPartChildNodes.getLength() == 0) break;
                for (int k = 0; k < pkgXmlDataChildNodes.getLength(); ++k) {
                    if (pkgXmlDataChildNodes.item(k).getNodeName().contains("ds:datastoreItem")) {
                        NamedNodeMap dataStoreItemAttrs = pkgXmlDataChildNodes.item(k).getAttributes();
                        Node dsItemID = dataStoreItemAttrs.getNamedItem("ds:itemID");
                        String itemId = dsItemID.getNodeValue();
                        if (isEmbedding && itemId.equals("")) {
                            nodeArrayList.add(dsItemID); // Only one empty itemId is required.
                            return nodeArrayList;
                        }
                        else if (!isEmbedding && isInnerEncoding(itemId)) {
                            nodeArrayList.add(dsItemID);
                        }
                    }
                }
            }
        }
        return nodeArrayList;
    }
}
