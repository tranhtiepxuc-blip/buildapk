package com.duc.vcam;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import java.io.File;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam;

public class XposedHook implements IXposedHookLoadPackage {

    // 🚀 TỰ ĐỊNH NGHĨA: Dùng file ảnh JPG thông thường cho nhẹ máy, đặt tên gì cũng được!
    private static final String TARGET_IMAGE = "/sdcard/Movies/anh_fake.jpg";

    @Override
    public void handleLoadPackage(final LoadPackageParam lpparam) throws Throwable {
        // Chỉ nhắm vào đúng app FMS Bình Thuận
        if (!lpparam.packageName.equals("com.gfd.fms.binhthuan")) {
            return;
        }

        XposedBridge.log("[OmniVCam] Đã bọc thành công vào RAM app FMS Bình Thuận!");

        // Chốt chặn giải mã file ảnh chụp thực địa liên tục
        try {
            XposedHelpers.findAndHookMethod(BitmapFactory.class, "decodeFile", String.class, BitmapFactory.Options.class, new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    File fakeFile = new File(TARGET_IMAGE);
                    if (fakeFile.exists()) {
                        // Ép app FMS đọc thẳng tệp ảnh JPG Đức vừa chọn, không cần 1000.bmp gì nữa!
                        param.args[0] = TARGET_IMAGE;
                        XposedBridge.log("[OmniVCam] 🟢 LIVE: Đã tráo ảnh JPG tràn màn hình thành công!");
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
            XposedBridge.log("[OmniVCam-Error] Lỗi chốt chặn: " + t.getMessage());
        }
    }
}
