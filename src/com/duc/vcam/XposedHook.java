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

    private static final String TARGET_IMAGE = "/sdcard/Movies/vcam_target.jpg";

    @Override
    public void handleLoadPackage(final LoadPackageParam lpparam) throws Throwable {
        // Chỉ xử lý riêng cho app FMS Bình Thuận để tránh làm lag máy
        if (!lpparam.packageName.equals("com.gfd.fms.binhthuan")) {
            return;
        }

        XposedBridge.log("[OmniVCam-Live] Đã khóa mục tiêu liên tục: " + lpparam.packageName);

        // 🚀 ĐÒN QUYẾT ĐỊNH: Hook thẳng vào hàm decodeFile. Mỗi lần app FMS gọi file ảnh để hiển thị 
        // hoặc nạp luồng camera, nó bắt buộc phải đọc trực tiếp file từ thẻ nhớ theo thời gian thực.
        try {
            XposedHelpers.findAndHookMethod(BitmapFactory.class, "decodeFile", String.class, BitmapFactory.Options.class, new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    String filePath = (String) param.args[0];
                    
                    // Nếu app FMS đang cố gắng nạp một file ảnh hoặc luồng dữ liệu camera cũ
                    if (filePath != null) {
                        File fakeFile = new File(TARGET_IMAGE);
                        if (fakeFile.exists()) {
                            // Ép app FMS đọc thẳng tệp vcam_target.jpg mới tinh mà Đức vừa chọn ngoài app
                            param.args[0] = TARGET_IMAGE;
                            XposedBridge.log("[OmniVCam] 🟢 LIVE: Đã tráo luồng ảnh mới thời gian thực!");
                        }
                    }
                }
            });

            // Dự phòng thêm chốt chặn decodeByteArray để xử lý luồng streaming liên tục
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
            XposedBridge.log("[OmniVCam-Error] Lỗi nạp luồng liên tục: " + t.getMessage());
        }
    }
}
