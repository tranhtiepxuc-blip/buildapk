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
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam;

public class XposedHook implements IXposedHookLoadPackage {

    // Kéo ảnh trực tiếp từ khu vực công cộng, bỏ qua phân quyền SharedPrefs
    private static final String PUBLIC_FAKE_IMAGE = "/sdcard/Movies/origin.jpg";

    @Override
    public void handleLoadPackage(final LoadPackageParam lpparam) throws Throwable {
        if (lpparam.packageName.equals("android") || lpparam.packageName.equals("com.android.systemui") || lpparam.packageName.equals("com.duc.vcam")) {
            return;
        }

        XposedBridge.log("[OmniVCam] Đang ép luồng camera trực tiếp từ Movies cho: " + lpparam.packageName);

        // Chặn luồng xử lý ảnh thô toàn diện
        try {
            XposedHelpers.findAndHookMethod(BitmapFactory.class, "decodeByteArray", byte[].class, int.class, int.class, BitmapFactory.Options.class, new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    File imgFile = new File(PUBLIC_FAKE_IMAGE);
                    if (imgFile.exists()) {
                        Bitmap bitmap = BitmapFactory.decodeFile(PUBLIC_FAKE_IMAGE);
                        if (bitmap != null) {
                            ByteArrayOutputStream stream = new ByteArrayOutputStream();
                            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, stream);
                            param.args[0] = stream.toByteArray();
                            XposedBridge.log("[OmniVCam] 🟢 ĐÃ TRÁO ẢNH THÀNH CÔNG TỪ THƯ MỤC MOVIES!");
                        }
                    }
                }
            });
        } catch (Throwable t) {
            XposedBridge.log("[OmniVCam] Lỗi chặn luồng: " + t.getMessage());
        }
    }
}
