package com.duc.vcam;

import android.app.Activity;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
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
import java.io.FileOutputStream;
import java.nio.ByteBuffer;

public class MainActivity extends Activity {
    private static final int PICK_IMAGE = 1;
    private TextView txtStatus;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setGravity(Gravity.CENTER);
        layout.setPadding(50, 50, 50, 50);

        Button btnPick = new Button(this);
        btnPick.setText("BẤM ĐỂ CHỌN ẢNH FAKE (TỰ ĐỘNG ÉP 960x720)");
        layout.addView(btnPick);

        txtStatus = new TextView(this);
        txtStatus.setText("\nTrạng thái: Sẵn sàng nạp ảnh.");
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

                    Bitmap originalBitmap = BitmapFactory.decodeFile(picturePath);
                    Bitmap scaledBitmap = Bitmap.createScaledBitmap(originalBitmap, 960, 720, true);

                    File dir1 = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES), "");
                    File dir2 = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM), "Camera1");
                    File dir3 = new File("/sdcard/Android/data/com.gfd.fms.binhthuan/files/Camera1");

                    if (!dir1.exists()) dir1.mkdirs();
                    if (!dir2.exists()) dir2.mkdirs();
                    if (!dir3.exists()) dir3.mkdirs();

                    saveAsBmp(scaledBitmap, new File(dir1, "origin.jpg"));
                    saveAsBmp(scaledBitmap, new File(dir2, "1000.bmp"));
                    saveAsBmp(scaledBitmap, new File(dir3, "1000.bmp"));

                    txtStatus.setText("\n🟢 ĐÃ ĐỒNG BỘ HOÀN TOÀN!\nẢnh đã được chuyển thành BMP và rải vào tất cả thư mục chốt chặn!");
                    Toast.makeText(this, "Đã đồng bộ ảnh fake thành công!", Toast.LENGTH_LONG).show();
                }
            } catch (Exception e) {
                txtStatus.setText("\n🔴 Lỗi: Chưa cấp quyền bộ nhớ hoặc file lỗi: " + e.getMessage());
            }
        }
    }

    private void saveAsBmp(Bitmap bitmap, File file) throws Exception {
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();
        int[] pixels = new int[width * height];
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height);

        byte[] rgb = new byte[width * height * 3];
        for (int i = 0; i < pixels.length; i++) {
            int row = height - 1 - (i / width);
            int col = i % width;
            int index = (row * width + col) * 3;
            int p = pixels[i];
            rgb[index] = (byte) (p & 0xFF);          
            rgb[index + 1] = (byte) ((p >> 8) & 0xFF);  
            rgb[index + 2] = (byte) ((p >> 16) & 0xFF); 
        }

        ByteBuffer buffer = ByteBuffer.allocate(54 + rgb.length);
        buffer.put((byte) 'B'); buffer.put((byte) 'M');
        buffer.putInt(54 + rgb.length);
        buffer.putShort((short) 0); buffer.putShort((short) 0);
        buffer.putInt(54);
        buffer.putInt(40);
        buffer.putInt(width);
        buffer.putInt(height);
        buffer.putShort((short) 1);
        buffer.putShort((short) 24);
        buffer.putInt(0);
        buffer.putInt(rgb.length);
        buffer.putInt(0); buffer.putInt(0); buffer.putInt(0); buffer.putInt(0);
        buffer.put(rgb);

        FileOutputStream fos = new FileOutputStream(file);
        fos.write(buffer.array());
        fos.close();
    }
}
