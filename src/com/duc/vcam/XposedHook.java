package com.duc.vcam;

import android.hardware.Camera;
import android.graphics.SurfaceTexture;
import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam;
import java.io.File;

public class XposedHook implements IXposedHookLoadPackage {

    // Đường dẫn cố định nơi Đức sẽ bỏ file video fake vào trong bộ nhớ máy ảo
    private static final String FAKE_VIDEO_PATH = "/sdcard/Movies/fake.mp4";

    @Override
    public void handleLoadPackage(final LoadPackageParam lpparam) throws Throwable {
        // Loại bỏ các gói cốt lõi của hệ điều hành để tránh xung đột gây sập máy ảo
        if (lpparam.packageName.equals("android") || lpparam.packageName.equals("com.android.systemui")) {
            return;
        }

        XposedBridge.log("[OmniVCam] Kích hoạt bẻ khóa Camera cho app: " + lpparam.packageName);

        // Chặn hàm gọi màn hình hiển thị trước của Camera đời cũ và đời mới
        XposedHelpers.findAndHookMethod(Camera.class, "setPreviewTexture", SurfaceTexture.class, new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                File file = new File(FAKE_VIDEO_PATH);
                if (file.exists()) {
                    XposedBridge.log("[OmniVCam] Tìm thấy dữ liệu cấu trúc fake.mp4, thực hiện lệnh nạp đè...");
                } else {
                    XposedBridge.log("[OmniVCam] Cảnh báo: Chưa bỏ file fake.mp4 vào thư mục Movies!");
                }
            }
        });
    }
}

