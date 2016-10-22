package com.baidu.meituanhotfix;

import android.app.Application;
import android.content.Context;
import android.content.res.AssetManager;
import android.os.Environment;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;

import dalvik.system.DexClassLoader;

/**
 * Created by lixingyun on 16/9/29.
 */

public class TestApplication extends Application {


    List<PatchedClassInfo> patchs;

    @Override
    public void onCreate() {
        super.onCreate();
        copy("patch.jar");
        final File optimizedDexOutputPath = new File(Environment.getExternalStorageDirectory().toString()
                + File.separator + "patch.jar");

        Log.e("tag", "file is exist " + optimizedDexOutputPath.exists());

        loadDex();


    }


    public void loadDex() {
        //加载patch.jar
        File dexPath = new File(getDir("dex", Context.MODE_PRIVATE), "patch.jar");
//        新建一个classloader
        DexClassLoader cl = new DexClassLoader(dexPath.getAbsolutePath(),
                dexPath.getAbsolutePath(), null, getClassLoader());
        Class libProviderClazz = null;

        try {
            // 从patch.java 加载下发类
            libProviderClazz = cl.loadClass("com.baidu.meituanhotfix.dex.PatchesInfoImpl");

            PatchesInfo patchesInfo = (PatchesInfo) libProviderClazz.newInstance();

            // 获取下发信息 ，这是一个list 每一条存放patch类和每个patch的楔子类
            patchs = patchesInfo.getPatchedClassesInfo();

            for (PatchedClassInfo cls : patchs) {

//                patch类
                Class hostClazz = getClassLoader().loadClass(cls.getClazz());
//                patch类对应的楔子类
                Class implClazz = cl.loadClass(cls.getPathRedicIMpl());
//                实例化楔子类
                ChangeQuickRedirect impl = (ChangeQuickRedirect) implClazz.newInstance();

//                将刚才实例化之后的赋值给patch
                Field[] fields = hostClazz.getFields();
                if (fields != null && fields.length > 0) {
                    Field field = hostClazz.getField("changeQuickRedirect");
                    field.set(hostClazz, impl);
                }
            }


        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private AssetManager mAssetManager = null;

    protected void copy(String asset) {
        File destinationFile = null;
        try {
            mAssetManager = this.getAssets();
            final File optimizedDexOutputPath = getDir("dex", Context.MODE_PRIVATE);
            InputStream source = mAssetManager.open(new File(asset).getPath());
            destinationFile = new File(optimizedDexOutputPath, asset);
            destinationFile.getParentFile().mkdirs();
            OutputStream destination = new FileOutputStream(destinationFile);
            byte[] buffer = new byte[1024];
            int nread;

            while ((nread = source.read(buffer)) != -1) {
                if (nread == 0) {
                    nread = source.read();
                    if (nread < 0)
                        break;
                    destination.write(nread);
                    continue;
                }
                destination.write(buffer, 0, nread);
            }
            destination.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }
}
