package com.duc.vcam;

import android.hardware.Camera;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import java.io.ByteArrayOutputStream;
import java.io.File;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.XSharedPreferences;
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam;

public class XposedHook implements IXposedHookLoadPackage {

    @Override
    public void handleLoadPackage(final LoadPackageParam lpparam) throws Throwable {
        if (lpparam.packageName.equals("android") || lpparam.packageName.equals("com.android.systemui") || lpparam.packageName.equals("com.duc.vcam")) {
            return;
        }

        XposedBridge.log("[OmniVCam-Pro] Khởi động bẻ khóa camera cho app: " + lpparam.packageName);

        XposedHelpers.findAndHookMethod(Camera.class, "takePicture", 
            Camera.ShutterCallback.class, 
            Camera.PictureCallback.class, 
            Camera.PictureCallback.class, 
            Camera.PictureCallback.class, 
            new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    XposedBridge.log("[OmniVCam-Pro] Thao tác bấm máy chụp kích hoạt!");

                    // 🚀 ĐỌC ĐƯỜNG DẪN ẢNH ĐỨC TỰ CHỌN TỪ GIAO DIỆN
                    XSharedPreferences xPref = new XSharedPreferences("com.duc.vcam", "vcam_settings");
                    xPref.makeWorldReadable();
                    String customImagePath = xPref.getString("image_path", null);

                    if (customImagePath != null) {
                        File imgFile = new File(customImagePath);
                        if (imgFile.exists()) {
                            XposedBridge.log("[OmniVCam-Pro] Đang nạp đè tấm ảnh tự chọn: " + customImagePath);
                            
                            Bitmap bitmap = BitmapFactory.decodeFile(customImagePath);
                            if (bitmap != null) {
                                ByteArrayOutputStream stream = new ByteArrayOutputStream();
                                bitmap.compress(Bitmap.CompressFormat.JPEG, 90, stream);
                                byte[] fakePhotoBytes = stream.toByteArray();

                                if (param.args[3] != null) {
                                    param.args[3] = new Camera.PictureCallback() {
                                        @Override
                                        public void onPictureTaken(byte[] data, Camera camera) {
                                            ((Camera.PictureCallback) param.args[3]).onPictureTaken(fakePhotoBytes, camera);
                                        }
                                    };
                                }
                            }
                        }
                    } else {
                        XposedBridge.log("[OmniVCam-Pro] Đức chưa chọn tấm ảnh nào trong giao diện ứng dụng!");
                    }
                }
        });
    }
}
