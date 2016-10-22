package com.baidu.meituanhotfix;

import java.lang.reflect.Proxy;

/**
 * Created by lixingyun on 16/9/29.
 */

public class PatchProxy {
    /**
     *
     * @param param
     * @param object class的实例对象
     * @param changeQuickRedirect
     * @return
     */
    public  static boolean isSupport( Object object, ChangeQuickRedirect changeQuickRedirect, Object... param){
        if (changeQuickRedirect != null){
            String  methodSignature  = new Throwable().getStackTrace()[1].getMethodName();
            Object[] paramArrayOfObject = param;
            return changeQuickRedirect.isSupport(methodSignature,paramArrayOfObject);
        }
        return false;
    }

    public  static Object accessDispatch(Object object, ChangeQuickRedirect changeQuickRedirect, Object... param){

        if (changeQuickRedirect != null){
            String  methodSignature = new Throwable().getStackTrace()[1].getMethodName();
            Object[] paramArrayOfObject = param;
            return changeQuickRedirect.accessDispatch(methodSignature,paramArrayOfObject);
        }
        return null;

    }
}
