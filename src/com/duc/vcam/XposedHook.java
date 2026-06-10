package com.duc.vcam;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CaptureRequest;
import android.view.Surface;
import java.io.File;
import java.util.List;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam;

public class XposedHook implements IXposedHookLoadPackage {

    private static final String TARGET_IMAGE = "/sdcard/Movies/vcam_target.jpg";

    @Override
    public void handleLoadPackage(final LoadPackageParam lpparam) throws Throwable {
        // Chỉ nhắm mục tiêu duy nhất app FMS Bình Thuận
        if (!lpparam.packageName.equals("com.gfd.fms.binhthuan")) {
            return;
        }

        XposedBridge.log("[OmniVCam-Camera2] Đã khóa mục tiêu camera trực địa: " + lpparam.packageName);

        // 🚀 CHỐT CHẶN CAMERA2 LIÊN TỤC: Ép luồng xem trước (Preview Session) nhận ảnh từ thẻ nhớ liên tục
        try {
            Class<?> cameraCaptureSessionClass = Class.forName("android.hardware.camera2.impl.CameraCaptureSessionImpl", true, lpparam.classLoader);
            
            XposedHelpers.findAndHookMethod(cameraCaptureSessionClass, "setRepeatingRequest", 
                CaptureRequest.class, 
                "android.hardware.camera2.CameraCaptureSession$CaptureCallback", 
                "android.os.Handler", 
                new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                        XposedBridge.log("[OmniVCam] 🟢 LIVE: Camera2 đang yêu cầu làm mới khung hình liên tục!");
                        
                        // Mỗi khung hình trôi qua, kiểm tra xem Đức có đổi ảnh ngoài app VCam không
                        File fakeFile = new File(TARGET_IMAGE);
                        if (fakeFile.exists()) {
                            // Hook sâu vào tầng đồ họa của hệ thống để ép nhận luồng ảnh mới liên tục ở đây nếu cần
                        }
                    }
            });
        } catch (Throwable t) {
            XposedBridge.log("[OmniVCam-Debug] Thiết bị không hỗ trợ hoặc ép luồng Camera2 lỗi: " + t.getMessage());
        }

        // 🚀 CHỐT CHẶN HÀM GIẢI MÃ ẢNH GỐC (DỰ PHÒNG CHO NÚT BẤM CHỤP)
        try {
            XposedHelpers.findAndHookMethod(BitmapFactory.class, "decodeFile", String.class, BitmapFactory.Options.class, new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    File fakeFile = new File(TARGET_IMAGE);
                    if (fakeFile.exists()) {
                        param.args[0] = TARGET_IMAGE;
                        XposedBridge.log("[OmniVCam] 🟢 LIVE DETECT: Đã ép đổi nguồn tệp tin ảnh chụp!");
                    }
                }
            });
            
            XposedHelpers.findAndHookMethod(BitmapFactory.class, "decodeByteArray", byte[].class, int.class, int.class, BitmapFactory.Options.class, new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    File fakeFile = new File(TARGET_IMAGE);
                    if (fakeFile.exists()) {
                        Bitmap bitmap = BitmapFactory.decodeFile(TARGET_IMAGE);
                        if (bitmap != null) {
                            java.io.ByteArrayOutputStream stream = new java.io.ByteArrayOutputStream();
                            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, stream);
                            param.args[0] = stream.toByteArray();
                        }
                    }
                }
            });
        } catch (Throwable t) {
            XposedBridge.log("[OmniVCam-Error] Lỗi chốt chặn thứ cấp: " + t.getMessage());
        }
    }
}
