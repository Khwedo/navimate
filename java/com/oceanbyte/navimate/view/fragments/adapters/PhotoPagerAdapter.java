package com.oceanbyte.navimate.view.fragments.adapters;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.github.chrisbanes.photoview.PhotoView;

import java.io.File;
import java.util.List;

/**
 * Адаптер для ViewPager2 — отображает список изображений с поддержкой зума (PhotoView).
 */
public class PhotoPagerAdapter extends RecyclerView.Adapter<PhotoPagerAdapter.PhotoViewHolder> {

    private final Context context;
    private final List<String> photoPaths;

    public PhotoPagerAdapter(Context context, List<String> photoPaths) {
        this.context = context;
        this.photoPaths = photoPaths;
    }

    @NonNull
    @Override
    public PhotoViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        PhotoView photoView = new PhotoView(context);
        photoView.setLayoutParams(new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
        ));
        photoView.setBackgroundColor(0xFF000000);
        photoView.setScaleType(PhotoView.ScaleType.FIT_CENTER);
        return new PhotoViewHolder(photoView);
    }

    @Override
    public void onBindViewHolder(@NonNull PhotoViewHolder holder, int position) {
        File file = new File(photoPaths.get(position));
        if (file.exists()) {
            Glide.with(context)
                    .load(file)
                    .into(holder.photoView);
        } else {
            // Можно установить заглушку или сообщение об ошибке
            holder.photoView.setImageDrawable(null);
        }
    }

    @Override
    public int getItemCount() {
        return photoPaths.size();
    }

    static class PhotoViewHolder extends RecyclerView.ViewHolder {
        PhotoView photoView;

        public PhotoViewHolder(@NonNull View itemView) {
            super(itemView);
            photoView = (PhotoView) itemView;
        }
    }
}
