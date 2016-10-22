package com.baidu.meituanhotfix;

import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.Toast;


import java.io.File;
import java.util.List;

import dalvik.system.DexClassLoader;

public class MainActivity extends AppCompatActivity {


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Toast.makeText(this,getIndex()+"",Toast.LENGTH_LONG).show();
    }

    public static ChangeQuickRedirect changeQuickRedirect;

    public long getIndex(){
        if(changeQuickRedirect != null) {
            //PatchProxy中封装了获取当前className和methodName的逻辑，并在其内部最终调用了changeQuickRedirect的对应函数
            if(PatchProxy.isSupport( "getIndex", changeQuickRedirect, (Object[]) null)) {
                return ((Long)PatchProxy.accessDispatch(this, changeQuickRedirect, (Object[])null)).longValue();
            }
        }
        return 100L;
    }


}
