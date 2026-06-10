package com.duc.vcam;

import android.app.Activity;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;

public class MainActivity extends Activity {
    private static final int PICK_IMAGE = 1;
    private TextView txtStatus;

    // 🚀 ĐÃ FIX CHÍ MẠNG: Đổi tên về hàm onCreate chuẩn của Android Activity
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setGravity(Gravity.CENTER);
        layout.setPadding(50, 50, 50, 50);

        Button btnPick = new Button(this);
        btnPick.setText("Bấm Để Chọn Ảnh Từ Thư Viện");
        layout.addView(btnPick);

        txtStatus = new TextView(this);
        txtStatus.setText("\nTrạng thái: Sẵn sàng.");
        txtStatus.setGravity(Gravity.CENTER);
        layout.addView(txtStatus);

        setContentView(layout);

        btnPick.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                startActivityForResult(intent, PICK_IMAGE);
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_IMAGE && resultCode == RESULT_OK && data != null) {
            try {
                Uri selectedImage = data.getData();
                String[] filePathColumn = { MediaStore.Images.Media.DATA };
                Cursor cursor = getContentResolver().query(selectedImage, filePathColumn, null, null, null);
                if (cursor != null) {
                    cursor.moveToFirst();
                    int columnIndex = cursor.getColumnIndex(filePathColumn[0]);
                    String picturePath = cursor.getString(columnIndex);
                    cursor.close();

                    // Copy đè thẳng tấm ảnh được chọn vào thư mục công cộng Movies
                    File src = new File(picturePath);
                    File destDir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES), "");
                    if (!destDir.exists()) destDir.mkdirs();
                    File dest = new File(destDir, "origin.jpg");

                    copyFile(src, dest);
                    txtStatus.setText("\n🟢 Đã đồng bộ ảnh fake vào hệ thống công cộng!");
                    Toast.makeText(this, "Chọn ảnh thành công!", Toast.LENGTH_SHORT).show();
                }
            } catch (Exception e) {
                txtStatus.setText("\n🔴 Lỗi: Chưa cấp quyền truy cập bộ nhớ cho App!");
            }
        }
    }

    private void copyFile(File source, File dest) throws Exception {
        InputStream is = new FileInputStream(source);
        OutputStream os = new FileOutputStream(dest);
        byte[] buffer = new byte[1024];
        int length;
        while ((length = is.read(buffer)) > 0) {
            os.write(buffer, 0, length);
        }
        is.close();
        os.close();
    }
}
