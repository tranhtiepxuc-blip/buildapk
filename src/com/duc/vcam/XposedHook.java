package com.duc.vcam;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.PixelFormat;
import android.net.Uri;
import android.provider.MediaStore;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.Toast;
import java.io.File;
import java.io.FileOutputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam;

public class XposedHook implements IXposedHookLoadPackage {

    private static final String TARGET_IMAGE = "/sdcard/Movies/anh_fake.jpg";
    private static Button floatingButton;
    private static WindowManager windowManager;
    private static WindowManager.LayoutParams params;

    @Override
    public void handleLoadPackage(final LoadPackageParam lpparam) throws Throwable {
        if (lpparam == null || lpparam.packageName == null) {
            return;
        }

        // Chỉ bọc đuôi cho app FMS Bình Thuận
        if (!lpparam.packageName.equals("com.gfd.fms.binhthuan")) {
            return;
        }

        XposedBridge.log("[OmniVCam] Đã nạp Menu nổi lấy ảnh gốc vào FMS!");

        // 🚀 HOOK VÒNG ĐỜI: Dùng phản xạ thuần Java lấy "thisObject" để bypass hoàn toàn file JAR lỗi
        XposedHelpers.findAndHookMethod(Activity.class, "onResume", new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                Activity currentActivity = null;
                try {
                    // Dùng phản xạ thô của Java, không phụ thuộc vào XposedHelpers
                    Field thisObjectField = param.getClass().getField("thisObject");
                    currentActivity = (Activity) thisObjectField.get(param);
                } catch (Throwable e) {
                    // Nếu getField thất bại, ép kiểu gián tiếp qua Object để b bịt mắt trình dịch
                    Object rawParam = (Object) param;
                    currentActivity = (Activity) XposedHelpers.getObjectField(rawParam, "thisObject");
                }

                if (floatingButton == null && currentActivity != null) {
                    createFloatingMenu(currentActivity);
                }
            }
        });

        // 🚀 XỬ LÝ KẾT QUẢ CHỌN ẢNH TỪ MENU NỔI
        XposedHelpers.findAndHookMethod(Activity.class, "onActivityResult", int.class, int.class, Intent.class, new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                int requestCode = (Integer) param.args[0];
                int resultCode = (Integer) param.args[1];
                Intent data = (Intent) param.args[2];

                if (requestCode == 9999 && resultCode == Activity.RESULT_OK && data != null) {
                    Activity activity = null;
                    try {
                        Field thisObjectField = param.getClass().getField("thisObject");
                        activity = (Activity) thisObjectField.get(param);
                    } catch (Throwable e) {
                        Object rawParam = (Object) param;
                        activity = (Activity) XposedHelpers.getObjectField(rawParam, "thisObject");
                    }

                    if (activity == null) return;
                    
                    try {
                        Uri uri = data.getData();
                        Bitmap bitmap = MediaStore.Images.Media.getBitmap(activity.getContentResolver(), uri);

                        // Xoay đứng ảnh 90 độ cho khớp hướng màn hình app FMS
                        Matrix matrix = new Matrix();
                        matrix.postRotate(90);
                        
                        // GIỮ NGUYÊN ẢNH GỐC ĐỘ PHÂN GIẢI CAO
                        Bitmap finalBitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);

                        File targetFile = new File(TARGET_IMAGE);
                        if (targetFile.getParentFile() != null) targetFile.getParentFile().mkdirs();
                        
                        FileOutputStream fos = new FileOutputStream(targetFile);
                        finalBitmap.compress(Bitmap.CompressFormat.JPEG, 95, fos);
                        fos.close();

                        Toast.makeText(activity, "🟢 Đã nạp ẢNH GỐC thành công!", Toast.LENGTH_SHORT).show();
                    } catch (Exception e) {
                        Toast.makeText(activity, "🔴 Lỗi nạp ảnh gốc: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    }
                    
                    // Bypass triệt để hàm setResult bằng cách gọi gián tiếp thông qua Object thô
                    try {
                        Method setResultMethod = param.getClass().getMethod("setResult", Object.class);
                        setResultMethod.invoke(param, new Object[]{null});
                    } catch (Throwable t) {
                        Object rawParam = (Object) param;
                        XposedHelpers.callMethod(rawParam, "setResult", new Object[]{null});
                    }
                }
            }
        });

        // 🚀 THAY THẾ DRIVER CAMERA: Ép luồng nhận ảnh JPG từ bộ nhớ
        try {
            XposedHelpers.findAndHookMethod(BitmapFactory.class, "decodeFile", String.class, BitmapFactory.Options.class, new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    File fakeFile = new File(TARGET_IMAGE);
                    if (fakeFile.exists()) {
                        param.args[0] = TARGET_IMAGE;
                    }
                }
            });
        } catch (Throwable t) {
            XposedBridge.log("[OmniVCam-Error] Lỗi ép ảnh: " + t.getMessage());
        }
    }

    // Cơ chế tạo và quản lý nút nổi kéo thả trực tiếp trên RAM app FMS
    private void createFloatingMenu(final Activity activity) {
        try {
            windowManager = (WindowManager) activity.getSystemService(Context.WINDOW_SERVICE);
            
            floatingButton = new Button(activity);
            floatingButton.setText("ĐỔI ẢNH");
            floatingButton.setBackgroundColor(Color.parseColor("#FF009688"));
            floatingButton.setTextColor(Color.WHITE);
            floatingButton.setPadding(20, 10, 20, 10);
            floatingButton.setTextSize(12);

            params = new WindowManager.LayoutParams(
                    WindowManager.LayoutParams.WRAP_CONTENT,
                    WindowManager.LayoutParams.WRAP_CONTENT,
                    WindowManager.LayoutParams.TYPE_APPLICATION,
                    WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                    PixelFormat.TRANSLUCENT
            );

            params.gravity = Gravity.TOP | Gravity.END;
            params.x = 20;
            params.y = 250;

            floatingButton.setOnTouchListener(new View.OnTouchListener() {
                private int initialX;
                private int initialY;
                private float initialTouchX;
                private float initialTouchY;

                @Override
                public boolean onTouch(View v, MotionEvent event) {
                    switch (event.getAction()) {
                        case MotionEvent.ACTION_DOWN:
                            initialX = params.x;
                            initialY = params.y;
                            initialTouchX = event.getRawX();
                            initialTouchY = event.getRawY();
                            return true;
                        case MotionEvent.ACTION_MOVE:
                            params.x = initialX - (int) (event.getRawX() - initialTouchX);
                            params.y = initialY + (int) (event.getRawY() - initialTouchY);
                            windowManager.updateViewLayout(floatingButton, params);
                            return true;
                        case MotionEvent.ACTION_UP:
                            if (Math.abs(event.getRawX() - initialTouchX) < 10 && Math.abs(event.getRawY() - initialTouchY) < 10) {
                                Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                                activity.startActivityForResult(intent, 9999);
                            }
                            return true;
                    }
                    return false;
                }
            });

            windowManager.addView(floatingButton, params);
        } catch (Throwable e) {
            XposedBridge.log("[OmniVCam] Không thể tạo view nổi: " + e.getMessage());
        }
    }
}
