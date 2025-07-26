// SimplePdfTemplate.java
package com.oceanbyte.navimate.export;

import android.content.Context;
import android.os.Environment;

import com.itextpdf.kernel.pdf.*;
import com.itextpdf.layout.*;
import com.itextpdf.layout.element.*;
import com.itextpdf.layout.property.TextAlignment;
import com.oceanbyte.navimate.models.JobReport;

import java.io.File;
import java.util.List;

public class PdfExportTemplate implements ExportTemplate {

    @Override
    public File export(Context context, List<JobReport> reports, String filename) {
        try {
            File dir = context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS);
            if (!dir.exists()) dir.mkdirs();
            File file = new File(dir, filename + ".pdf");

            PdfWriter writer = new PdfWriter(file);
            PdfDocument pdf = new PdfDocument(writer);
            Document document = new Document(pdf);

            for (JobReport report : reports) {
                document.add(new Paragraph("Название работы: " + report.jobTitle).setBold());
                document.add(new Paragraph("Оборудование: " + report.equipmentName));
                document.add(new Paragraph("Дата: " + report.getFormattedDate()));
                document.add(new Paragraph(" "));
            }

            document.close();
            return file;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}
