package com.duc.vcam;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.widget.Toast;
import java.io.File;
import java.io.FileOutputStream;

public class MainActivity extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(intent, 1);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK && data != null) {
            try {
                Uri uri = data.getData();
                Bitmap bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), uri);

                // Tự động xoay đứng ảnh và ép về khung màn hình dọc 720x960 để không bị thu nhỏ chút ét
                Matrix matrix = new Matrix();
                matrix.postRotate(90);
                Bitmap rotated = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
                Bitmap scaled = Bitmap.createScaledBitmap(rotated, 720, 960, true);

                File targetFile = new File("/sdcard/Movies/anh_fake.jpg");
                if (targetFile.getParentFile() != null) targetFile.getParentFile().mkdirs();
                
                FileOutputStream fos = new FileOutputStream(targetFile);
                scaled.compress(Bitmap.CompressFormat.JPEG, 90, fos); // Lưu dạng JPG thông thường
                fos.close();

                Toast.makeText(this, "🟢 Đã đồng bộ ảnh JPG mới thành công!", Toast.LENGTH_SHORT).show();
            } catch (Exception e) {
                Toast.makeText(this, "🔴 Lỗi: " + e.getMessage(), Toast.LENGTH_LONG).show();
            }
        }
        finish();
    }
}
