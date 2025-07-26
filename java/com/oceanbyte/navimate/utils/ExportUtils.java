package com.oceanbyte.navimate.utils;

import android.content.Context;
import android.graphics.pdf.PdfDocument;
import android.util.Log;
import android.widget.Toast;


import com.itextpdf.text.Document;
import com.itextpdf.text.Paragraph;
import com.itextpdf.text.pdf.PdfWriter;
import com.oceanbyte.navimate.models.ContractEntity;
import com.oceanbyte.navimate.models.JobReport;
import com.oceanbyte.navimate.models.UserProfile;

import org.apache.poi.xwpf.usermodel.*;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.*;

public class ExportUtils {

    /** Папка с пользовательскими шаблонами */
    public static File getTemplateDirectory(Context context) {
        File dir = new File(context.getExternalFilesDir(null), "templates");
        if (!dir.exists()) dir.mkdirs();
        return dir;
    }

    /** Папка для экспортированных отчётов */
    public static File getExportDirectory(Context context) {
        File dir = new File(context.getExternalFilesDir(null), "exports");
        if (!dir.exists()) dir.mkdirs();
        return dir;
    }

    /** Получение пользовательского шаблона DOCX */
    public static InputStream getUserTemplateStream(Context context) {
        File file = new File(getTemplateDirectory(context), "template.docx");
        if (file.exists()) {
            try {
                return new FileInputStream(file);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return null;
    }

    /** Экспорт отчётов в DOCX по шаблону */
    public static void exportToDocx(Context context, List<JobReport> reports, UserProfile profile, ContractEntity contract) {
        if (profile == null || profile.fullName == null || profile.fullName.trim().isEmpty()) {
            Toast.makeText(context, "Профиль не заполнен. Укажите имя в настройках.", Toast.LENGTH_LONG).show();
            return;
        }
        try {
            InputStream templateStream = getUserTemplateStream(context);
            if (templateStream == null) {
                Toast.makeText(context, "Шаблон не найден. Загрузите его в настройках.", Toast.LENGTH_LONG).show();
                return;
            }

            XWPFDocument document = new XWPFDocument(templateStream);

            String weekLabel = getWeekLabel(reports);
            String dateRange = getDateRange(reports);

            // Подстановка переменных в текст
            for (XWPFParagraph paragraph : document.getParagraphs()) {
                for (XWPFRun run : paragraph.getRuns()) {
                    String text = run.getText(0);
                    if (text != null) {
                        text = text.replace("${name}", profile.fullName)
                                .replace("${position}", contract.position)
                                .replace("${vessel}", contract.vesselName)
                                .replace("${week}", weekLabel)
                                .replace("${dateRange}", dateRange);
                        run.setText(text, 0);
                    }
                }
            }

            // Вставка таблицы с отчётами
            XWPFTable table = document.createTable();
            XWPFTableRow header = table.getRow(0);
            header.getCell(0).setText("Дата");
            header.addNewTableCell().setText("Оборудование");
            header.addNewTableCell().setText("Название работы");

            for (JobReport report : reports) {
                XWPFTableRow row = table.createRow();
                row.getCell(0).setText(report.getFormattedDate());
                row.getCell(1).setText(report.equipmentName);
                row.getCell(2).setText(report.jobTitle);
            }

            // Сохранение итогового файла
            String fileName = "Report_" + System.currentTimeMillis() + ".docx";
            File outFile = new File(getExportDirectory(context), fileName);
            try (FileOutputStream fos = new FileOutputStream(outFile)) {
                document.write(fos);
            }

            document.close();
            templateStream.close();

            Toast.makeText(context, "Файл сохранён: " + outFile.getAbsolutePath(), Toast.LENGTH_LONG).show();

        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(context, "Ошибка экспорта: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    /** Получить заголовок недели */
    private static String getWeekLabel(List<JobReport> reports) {
        if (reports.isEmpty()) return "Unknown Week";
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            Calendar calendar = Calendar.getInstance();
            Date firstDate = sdf.parse(reports.get(0).getFormattedDate());
            calendar.setTime(firstDate);
            int week = calendar.get(Calendar.WEEK_OF_YEAR);
            int year = calendar.get(Calendar.YEAR);
            return year + "-W" + String.format(Locale.getDefault(), "%02d", week);
        } catch (Exception e) {
            return "Unknown Week";
        }
    }

    /** Получить диапазон дат отчётов */
    private static String getDateRange(List<JobReport> reports) {
        if (reports.isEmpty()) return "";
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            SimpleDateFormat outFormat = new SimpleDateFormat("dd MMM yyyy", Locale.getDefault());
            List<Date> dates = new ArrayList<>();

            for (JobReport r : reports) {
                dates.add(sdf.parse(r.getFormattedDate()));
            }

            Collections.sort(dates);
            return outFormat.format(dates.get(0)) + " — " + outFormat.format(dates.get(dates.size() - 1));

        } catch (Exception e) {
            return "";
        }
    }
    public static void exportToPdf(Context context, List<JobReport> reports, UserProfile profile) {
        if (profile == null || profile.fullName == null || profile.fullName.trim().isEmpty()) {
            Toast.makeText(context, "Профиль не заполнен. Укажите имя в настройках.", Toast.LENGTH_LONG).show();
            return;
        }
        try {
            File exportDir = new File(context.getExternalFilesDir(null), "exports");
            if (!exportDir.exists()) exportDir.mkdirs();

            String fileName = "report_" + new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date()) + ".pdf";
            File file = new File(exportDir, fileName);

            Document document = new Document();
            PdfWriter.getInstance(document, new FileOutputStream(file));
            document.open();

            document.add(new Paragraph("NaviMate - Exported Report"));
            document.add(new Paragraph(" "));

            if (profile != null) {
                document.add(new Paragraph("User: " + profile.fullName));
                document.add(new Paragraph(" "));
            }

            for (JobReport report : reports) {
                document.add(new Paragraph("────────────────────────────"));
                document.add(new Paragraph("Date: " + report.getFormattedDate()));
                document.add(new Paragraph("Equipment: " + report.equipmentName));
                document.add(new Paragraph("Job: " + report.jobTitle));
                document.add(new Paragraph(" "));
            }

            document.close();
            Log.d("ExportUtils", "PDF exported to: " + file.getAbsolutePath());

        } catch (Exception e) {
            Log.e("ExportUtils", "PDF export failed", e);
        }
    }


}
