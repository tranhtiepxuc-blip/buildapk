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
        if (lpparam == null || lpparam.packageName == null) return;
        if (lpparam.packageName.equals("android") || lpparam.packageName.equals("com.android.systemui") || lpparam.packageName.equals("com.duc.vcam")) {
            return;
        }

        XposedBridge.log("[OmniVCam] Nhận diện mục tiêu CameraX: " + lpparam.packageName);

        ClassLoader appClassLoader = null;
        try {
            Field clField = lpparam.getClass().getDeclaredField("classLoader");
            clField.setAccessible(true);
            appClassLoader = (ClassLoader) clField.get(lpparam);
        } catch (Throwable t) {
            XposedBridge.log("[OmniVCam] Lỗi lấy ClassLoader: " + t.getMessage());
        }

        if (appClassLoader == null) return;

        // Tấn công lớp tương thích cơ sở CameraX của FMS
        try {
            Class<?> classDeviceBase = Class.forName("androidx.camera.camera2.internal.compat.CameraDeviceCompatBaseImpl", true, appClassLoader);
            XposedHelpers.findAndHookMethod(classDeviceBase, "createCaptureSession", "androidx.camera.camera2.internal.compat.params.SessionConfigurationCompat", new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    XposedBridge.log("[OmniVCam] 🎯 ĐÃ CHẶN: Luồng CameraX Base Impl tạo Session!");
                }
            });
        } catch (Throwable t) {}

        // Tấn công lớp tương thích API 28 CameraX của FMS
        try {
            Class<?> classDeviceApi28 = Class.forName("androidx.camera.camera2.internal.compat.CameraDeviceCompatApi28Impl", true, appClassLoader);
            XposedHelpers.findAndHookMethod(classDeviceApi28, "createCaptureSession", "androidx.camera.camera2.internal.compat.params.SessionConfigurationCompat", new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    XposedBridge.log("[OmniVCam] 🎯 ĐÃ CHẶN: Luồng CameraX API 28 Impl tạo Session!");
                }
            });
        } catch (Throwable t) {}

        // Chốt chặn cuối tầng biến đổi mảng byte
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
                            XposedBridge.log("[OmniVCam] 🟢 THÀNH CÔNG: Đã tiêm mảng byte ảnh tĩnh!");
                        }
                    }
                }
            });
        } catch (Throwable t) {}
    }
}
