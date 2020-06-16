package com.ritsuanList.logic;

import opennlp.tools.util.StringUtil;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;

import javax.xml.bind.DatatypeConverter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class BinaryToFile {

    public BinaryToFile() {
    }

    public List<String> destFile(String binaryFile, String filePath, String fileName) {
        if (StringUtil.isEmpty(binaryFile) || StringUtil.isEmpty(filePath) || StringUtil.isEmpty(fileName)) {
            return null;
        }

        List<String> listTxt = new ArrayList<>();

        File fileDir = new File(filePath);
        if (!fileDir.exists()) {
            fileDir.mkdirs();
        }

        File destFile = new File(filePath + fileName);

        byte[] b64dec = DatatypeConverter.parseBase64Binary(binaryFile);

        try {
            FileOutputStream fos = new FileOutputStream(destFile);
            fos.write(b64dec);
            listTxt = pdfToText(destFile);
            fos.close();
        } catch (IOException e) {
            System.out.println("Exception position : FileUtil - binaryToFile(String binaryFile, String filePath, String fileName)");
        } finally {
            destFile.delete();
            fileDir.delete();
        }

        return listTxt;
    }

    protected List<String> pdfToText(File file) throws IOException {
        PDDocument document = PDDocument.load(file);
        PDFTextStripper stripper = new PDFTextStripper();
        stripper.setSortByPosition(true);
        String data = stripper.getText(document);
        String[] dataArray = data.split("\\r\\n");
        List<String> list = Arrays.asList(dataArray);
        List<String> newList = list.stream().filter(i -> !i.isEmpty()).collect(Collectors.toList());
        return newList;
    }
}
