package com.duc.vcam;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity {
    private static final int PICK_IMAGE = 1;
    private TextView txtPath;
    private SharedPreferences prefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState) ;
        
        // Tạo bộ nhớ lưu trữ đường dẫn ảnh, cho phép app khác đọc được (World Readable giả lập)
        prefs = getSharedPreferences("vcam_settings", Context.MODE_PRIVATE);

        // Tự dựng bố cục giao diện thẳng bằng code Java
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setGravity(Gravity.CENTER);
        layout.setPadding(50, 50, 50, 50);

        TextView title = new TextView(this);
        title.setText("OMNI VCAM PRO - CHỌN ẢNH FAKE");
        title.setTextSize(20);
        title.setPadding(0, 0, 0, 50);
        layout.addView(title);

        Button btnPick = new Button(this);
        btnPick.setText("Bấm Để Chọn Ảnh Từ Thư Viện");
        layout.addView(btnPick);

        txtPath = new TextView(this);
        String savedPath = prefs.getString("image_path", "Chưa chọn tấm ảnh nào!");
        txtPath.setText("\nĐường dẫn hiện tại:\n" + savedPath);
        txtPath.setGravity(Gravity.CENTER);
        layout.addView(txtPath);

        setContentView(layout);

        // Sự kiện khi Đức bấm vào nút chọn ảnh
        btnPick.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                startMirrorIntent(intent);
            }
        });
    }

    private void startMirrorIntent(Intent intent) {
        try {
            startActivityForResult(intent, PICK_IMAGE);
        } catch (Exception e) {
            Toast.makeText(this, "Vui lòng cấp quyền truy cập bộ nhớ!", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_IMAGE && resultCode == RESULT_OK && data != null) {
            Uri selectedImage = data.getData();
            String[] filePathColumn = { MediaStore.Images.Media.DATA };
            Cursor cursor = getContentResolver().query(selectedImage, filePathColumn, null, null, null);
            if (cursor != null) {
                cursor.moveToFirst();
                int columnIndex = cursor.getColumnIndex(filePathColumn[0]);
                String picturePath = cursor.getString(columnIndex);
                cursor.close();

                // Lưu đường dẫn ảnh Đức vừa chọn vào bộ nhớ cấu hình
                prefs.edit().putString("image_path", picturePath).apply();
                txtPath.setText("\nĐường dẫn hiện tại:\n" + picturePath);
                Toast.makeText(this, "Đã lưu ảnh fake thành công!", Toast.LENGTH_SHORT).show();
            }
        }
    }
}
