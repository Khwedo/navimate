package com.oceanbyte.navimate.export.word;

import android.content.Context;

import com.oceanbyte.navimate.models.JobReport;

import org.apache.poi.xwpf.usermodel.*;

import java.io.File;
import java.io.FileOutputStream;
import java.util.List;

public class WordExportTemplate {

    public static File exportToDocx(Context context, List<JobReport> reports) {
        try {
            XWPFDocument document = new XWPFDocument();

            for (JobReport report : reports) {
                XWPFParagraph title = document.createParagraph();
                XWPFRun run = title.createRun();
                run.setText("Работа: " + report.jobTitle);
                run.setBold(true);
                run.setFontSize(14);

                XWPFParagraph eq = document.createParagraph();
                eq.createRun().setText("Оборудование: " + report.equipmentName);

                XWPFParagraph date = document.createParagraph();
                date.createRun().setText("Дата: " + report.getFormattedDate());

                document.createParagraph().createRun().addBreak(); // Отступ

                // Разделитель
                XWPFParagraph divider = document.createParagraph();
                divider.createRun().setText("--------------------------------------");
            }

            File file = new File(context.getExternalFilesDir(null), "ReportsExport.docx");
            FileOutputStream fos = new FileOutputStream(file);
            document.write(fos);
            fos.close();
            document.close();
            return file;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}
