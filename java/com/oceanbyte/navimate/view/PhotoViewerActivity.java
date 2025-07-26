package com.oceanbyte.navimate.view;

import android.os.Bundle;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;
import androidx.viewpager2.widget.ViewPager2;

import com.oceanbyte.navimate.R;
import com.oceanbyte.navimate.custom.ZoomDismissLayout;
import com.oceanbyte.navimate.view.fragments.adapters.PhotoPagerAdapter;

import java.util.ArrayList;

public class PhotoViewerActivity extends AppCompatActivity {

    private ZoomDismissLayout zoomLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);


        setContentView(R.layout.activity_photo_viewer);

        zoomLayout = findViewById(R.id.photoViewerRoot);
        ViewPager2 photoPager = findViewById(R.id.photoPager);

        ArrayList<String> photoPaths = getIntent().getStringArrayListExtra("photoPaths");
        int startPosition = getIntent().getIntExtra("startPosition", 0);

        if (photoPaths == null || photoPaths.isEmpty()) {
            finish();
            return;
        }

        PhotoPagerAdapter adapter = new PhotoPagerAdapter(this, photoPaths);
        photoPager.setAdapter(adapter);
        photoPager.setCurrentItem(startPosition, false);

        zoomLayout.setOnDismissListener(() -> {
            finish();
            overridePendingTransition(0, 0);
        });
    }
}
