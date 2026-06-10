package de.robv.android.xposed;

import java.io.File;

public class XSharedPreferences {
    public XSharedPreferences(String packageName, String preferenceName) {}
    public void makeWorldReadable() {}
    public String getString(String key, String defValue) {
        return defValue;
    }
}
