package com.example.myapplication.util;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Outline;
import android.os.Build;
import android.view.View;
import android.view.ViewOutlineProvider;
import android.widget.ImageView;

import com.example.myapplication.R;
import com.example.myapplication.data.local.AppDatabase;
import com.example.myapplication.data.local.ShopItemEntity;
import com.example.myapplication.data.local.UserEntity;

import java.io.File;

public class AvatarHelper {

    public interface AvatarLoadCallback {
        void onLoaded();
    }

    /**
     * Apply circular clip to an ImageView using ViewOutlineProvider.
     */
    public static void applyCircleClip(ImageView iv) {
        if (iv == null) return;
        iv.setOutlineProvider(new ViewOutlineProvider() {
            @Override
            public void getOutline(View view, Outline outline) {
                int w = view.getWidth();
                int h = view.getHeight();
                if (w > 0 && h > 0) {
                    outline.setOval(0, 0, w, h);
                } else {
                    outline.setOval(0, 0, view.getLayoutParams().width, view.getLayoutParams().height);
                }
            }
        });
        iv.setClipToOutline(true);
        iv.setScaleType(ImageView.ScaleType.CENTER_CROP);
    }

    public static void loadAvatar(Context context, int avatarId, ImageView iv, AvatarLoadCallback callback) {
        if (avatarId <= 0) {
            iv.setImageResource(R.drawable.ic_profile);
            if (callback != null) callback.onLoaded();
            return;
        }
        AppDatabase.databaseWriteExecutor.execute(() -> {
            try {
                ShopItemEntity item = AppDatabase.getInstance(context).shopItemDao().getItemById(avatarId);
                if (item != null && item.getIconResName() != null) {
                    int resId = context.getResources().getIdentifier(
                            item.getIconResName(), "drawable", context.getPackageName());
                    if (resId != 0) {
                        iv.post(() -> {
                            iv.setImageResource(resId);
                            iv.setImageTintList(null);
                            if (callback != null) callback.onLoaded();
                        });
                        return;
                    }
                }
                iv.post(() -> {
                    iv.setImageResource(R.drawable.ic_profile);
                    if (callback != null) callback.onLoaded();
                });
            } catch (Exception e) {
                iv.post(() -> {
                    iv.setImageResource(R.drawable.ic_profile);
                    if (callback != null) callback.onLoaded();
                });
            }
        });
    }

    public static void loadAvatar(Context context, int avatarId, ImageView iv) {
        loadAvatar(context, avatarId, iv, null);
    }

    public static void loadAvatar(Context context, UserEntity user, ImageView iv) {
        if (user == null) return;
        String customPath = user.getCustomAvatarPath();
        if (customPath != null && !customPath.isEmpty()) {
            File file = new File(customPath);
            if (file.exists()) {
                loadCustomAvatar(customPath, iv);
                return;
            }
        }
        loadAvatar(context, user.getAvatarId(), iv);
    }

    public static void loadAvatar(Context context, UserEntity user, ImageView iv, AvatarLoadCallback callback) {
        if (user == null) {
            iv.setImageResource(R.drawable.ic_profile);
            if (callback != null) callback.onLoaded();
            return;
        }
        String customPath = user.getCustomAvatarPath();
        if (customPath != null && !customPath.isEmpty()) {
            File file = new File(customPath);
            if (file.exists()) {
                loadCustomAvatar(customPath, iv);
                if (callback != null) callback.onLoaded();
                return;
            }
        }
        loadAvatar(context, user.getAvatarId(), iv, callback);
    }

    /**
     * Load a custom avatar from file path, center-crop to square, set on ImageView.
     * The ImageView should have applyCircleClip() applied for circular display.
     */
    public static void loadCustomAvatar(String path, ImageView iv) {
        BitmapFactory.Options opts = new BitmapFactory.Options();
        opts.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(path, opts);

        int targetSize = 256;
        opts.inSampleSize = calculateInSampleSize(opts, targetSize, targetSize);
        opts.inJustDecodeBounds = false;

        Bitmap bitmap = BitmapFactory.decodeFile(path, opts);
        if (bitmap == null) {
            iv.setImageResource(R.drawable.ic_profile);
            return;
        }

        // Center crop to square
        int size = Math.min(bitmap.getWidth(), bitmap.getHeight());
        int x = (bitmap.getWidth() - size) / 2;
        int y = (bitmap.getHeight() - size) / 2;
        Bitmap square = Bitmap.createBitmap(bitmap, x, y, size, size);
        if (square != bitmap) bitmap.recycle();

        iv.setImageBitmap(square);
        iv.setImageTintList(null);
    }

    private static int calculateInSampleSize(BitmapFactory.Options opts, int reqWidth, int reqHeight) {
        int height = opts.outHeight;
        int width = opts.outWidth;
        int inSampleSize = 1;
        if (height > reqHeight || width > reqWidth) {
            int halfHeight = height / 2;
            int halfWidth = width / 2;
            while ((halfHeight / inSampleSize) >= reqHeight && (halfWidth / inSampleSize) >= reqWidth) {
                inSampleSize *= 2;
            }
        }
        return inSampleSize;
    }
}
