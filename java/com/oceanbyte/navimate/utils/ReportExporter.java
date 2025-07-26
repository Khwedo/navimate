package com.oceanbyte.navimate.utils;

import android.content.Context;

import com.itextpdf.io.image.ImageData;
import com.itextpdf.io.image.ImageDataFactory;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Image;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.property.TextAlignment;

import com.oceanbyte.navimate.models.JobReport;

import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.apache.poi.util.Units;
import org.apache.poi.xwpf.usermodel.ParagraphAlignment;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;


public class ReportExporter {

    // Экспорт в Word (.docx)
    public static File exportToWord(Context context, JobReport report)
            throws IOException, InvalidFormatException {
        XWPFDocument doc = new XWPFDocument();

        // Заголовок
        XWPFParagraph title = doc.createParagraph();
        title.setAlignment(ParagraphAlignment.CENTER);
        XWPFRun titleRun = title.createRun();
        titleRun.setText("NaviMate - Отчёт о проделанной работе");
        titleRun.setBold(true);
        titleRun.setFontSize(18);

        // Дата
        XWPFParagraph date = doc.createParagraph();
        XWPFRun dateRun = date.createRun();
        dateRun.setText("Дата: " + report.getFormattedDate());

        // Оборудование и работа
        XWPFParagraph equip = doc.createParagraph();
        equip.createRun().setText("Оборудование: " + report.equipmentName);

        XWPFParagraph job = doc.createParagraph();
        job.createRun().setText("Работа: " + report.jobTitle);

        // Фото ДО
        XWPFParagraph beforeHeader = doc.createParagraph();
        beforeHeader.createRun().setText("Фото ДО:");
        for (String path : report.beforePhotos) {
            insertImage(doc, path);
        }

        // Фото ПОСЛЕ
        XWPFParagraph afterHeader = doc.createParagraph();
        afterHeader.createRun().setText("Фото ПОСЛЕ:");
        for (String path : report.afterPhotos) {
            insertImage(doc, path);
        }

        File dir = new File(context.getExternalFilesDir(null), "exports");
        if (!dir.exists()) dir.mkdirs();

        File file = new File(dir, "report_" + report.id + ".docx");
        FileOutputStream out = new FileOutputStream(file);
        doc.write(out);
        out.close();
        doc.close();

        return file;
    }



    private static void insertImage(XWPFDocument doc, String imagePath)
            throws IOException, InvalidFormatException {
        try (FileInputStream is = new FileInputStream(imagePath)) {
            XWPFParagraph p = doc.createParagraph();
            XWPFRun run = p.createRun();
            run.addPicture(is,
                    XWPFDocument.PICTURE_TYPE_JPEG,
                    imagePath,
                    Units.toEMU(400),
                    Units.toEMU(300));
        }
    }

    // Экспорт в PDF
    public static File exportToPdf(Context context, JobReport report) throws IOException {
        File dir = new File(context.getExternalFilesDir(null), "exports");
        if (!dir.exists()) dir.mkdirs();

        File file = new File(dir, "report_" + report.id + ".pdf");

        PdfWriter writer = new PdfWriter(file);
        PdfDocument pdf = new PdfDocument(writer);
        Document document = new Document(pdf);

        // Заголовок
        Paragraph title = new Paragraph("NaviMate - Отчёт о проделанной работе")
                .setFontSize(18)
                .setBold()
                .setTextAlignment(TextAlignment.CENTER);
        document.add(title);

        document.add(new Paragraph("Дата: " + report.getFormattedDate()));
        document.add(new Paragraph("Оборудование: " + report.equipmentName));
        document.add(new Paragraph("Работа: " + report.jobTitle));

        // Фото ДО
        document.add(new Paragraph("Фото ДО:").setBold());
        for (String path : report.beforePhotos) {
            addImageToPdf(document, path);
        }

        // Фото ПОСЛЕ
        document.add(new Paragraph("Фото ПОСЛЕ:").setBold());
        for (String path : report.afterPhotos) {
            addImageToPdf(document, path);
        }

        document.close();
        return file;
    }

    private static void addImageToPdf(Document document, String imagePath) {
        try {
            ImageData imageData = ImageDataFactory.create(imagePath);
            Image image = new Image(imageData)
                    .scaleToFit(400, 300)
                    .setMarginBottom(10);
            document.add(image);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
