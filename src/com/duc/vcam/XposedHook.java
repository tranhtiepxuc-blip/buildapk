package com.duc.vcam;

import android.hardware.Camera;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.lang.reflect.Field;

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

        // 🚀 DÙNG REFLECTION THUẦN CỦA JAVA: Lấy thẳng trường classLoader, không thèm qua XposedHelpers nữa!
        ClassLoader appClassLoader = null;
        try {
            Field clField = lpparam.getClass().getDeclaredField("classLoader");
            clField.setAccessible(true);
            appClassLoader = (ClassLoader) clField.get(lpparam);
        } catch (Throwable t) {
            XposedBridge.log("[OmniVCam] Lỗi dùng Java Reflection lấy ClassLoader: " + t.getMessage());
        }

        if (appClassLoader == null) {
            XposedBridge.log("[OmniVCam] Không lấy được ClassLoader, dừng luồng Hook nâng cao.");
            return;
        }

        // =========================================================================
        // 🔥 ĐÒN CHÍ MẠNG 1: Hook thẳng vào lớp nội bộ của CameraX (Dùng Class.forName)
        // =========================================================================
        try {
            // Sửa lỗi ép kiểu: Tìm đích danh Class đó thông qua appClassLoader trước
            Class<?> classCamera2Impl = Class.forName("androidx.camera.camera2.internal.Camera2CameraImpl", true, appClassLoader);
            XposedHelpers.findAndHookMethod(classCamera2Impl, "openCaptureSession", new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    XposedBridge.log("[OmniVCam] 🎯 BẮT TRÚNG LUỒNG: CameraX đang cố gắng mở Capture Session!");
                }
            });
        } catch (Throwable t) {
            XposedBridge.log("[OmniVCam] Không tìm thấy lớp Camera2CameraImpl của CameraX");
        }

        try {
            Class<?> classDeviceCompat = Class.forName("androidx.camera.camera2.internal.compat.CameraDeviceCompatAndR", true, appClassLoader);
            XposedHelpers.findAndHookMethod(classDeviceCompat, "openCamera", String.class, java.util.concurrent.Executor.class, android.hardware.camera2.CameraDevice.StateCallback.class, new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    XposedBridge.log("[OmniVCam] 🎯 Đã tóm sống hàm openCamera ngầm của CameraX Jetpack!");
                }
            });
        } catch (Throwable t) {
            XposedBridge.log("[OmniVCam] Không tìm thấy lớp CameraDeviceCompatAndR của CameraX");
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
