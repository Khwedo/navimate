package com.oceanbyte.navimate.export;

import android.content.Context;
import java.io.File;
import java.util.List;
import com.oceanbyte.navimate.models.JobReport;

public interface ExportTemplate {
    File export(Context context, List<JobReport> reports, String filename);
}