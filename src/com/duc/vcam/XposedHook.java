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

    private static final String PUBLIC_FAKE_IMAGE = "/sdcard/Movies/origin.jpg";

    @Override
    public void handleLoadPackage(final LoadPackageParam lpparam) throws Throwable {
        if (lpparam == null || lpparam.packageName == null) {
            return;
        }

        if (lpparam.packageName.equals("android") || lpparam.packageName.equals("com.android.systemui") || lpparam.packageName.equals("com.duc.vcam")) {
            return;
        }

        XposedBridge.log("[OmniVCam-AntiCameraX] Đang thiết lập lưới bọc bảo mật cho: " + lpparam.packageName);

        // =========================================================================
        // 🔥 ĐÒN CHÍ MẠNG 1: Hook thẳng vào lớp nội bộ của CameraX (Jetpack androidx)
        // =========================================================================
        try {
            // SỬA LỖI ĐỒNG BỘ: Chuyển chính xác thành lpparam.classLoader (Chữ L viết hoa)
            XposedHelpers.findAndHookMethod("androidx.camera.camera2.internal.Camera2CameraImpl", lpparam.classLoader, 
                "openCaptureSession", new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                        XposedBridge.log("[OmniVCam] 🎯 BẮT TRÚNG LUỒNG: CameraX đang cố gắng mở Capture Session!");
                    }
            });

            XposedHelpers.findAndHookMethod("androidx.camera.camera2.internal.compat.CameraDeviceCompatAndR", lpparam.classLoader,
                "openCamera", String.class, java.util.concurrent.Executor.class, android.hardware.camera2.CameraDevice.StateCallback.class,
                new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                        XposedBridge.log("[OmniVCam] 🎯 Đã tóm sống hàm openCamera ngầm của CameraX Jetpack!");
                    }
            });
        } catch (Throwable t) {
            XposedBridge.log("[OmniVCam] App không tích hợp CameraX hoặc dùng bản Jetpack custom: " + t.getMessage());
        }

        // =========================================================================
        // 🔥 ĐÒN CHÍ MẠNG 2: Chốt chặn cuối tầng biến đổi mảng byte ảnh (BitmapFactory)
        // =========================================================================
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
                            XposedBridge.log("[OmniVCam] 🟢 THÀNH CÔNG: Đã ép tráo mảng byte ảnh tĩnh từ Movies!");
                        }
                    }
                }
            });
        } catch (Throwable t) {
            XposedBridge.log("[OmniVCam] Lỗi chốt chặn BitmapFactory: " + t.getMessage());
        }
    }
}
