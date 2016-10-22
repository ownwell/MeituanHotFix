package com.baidu.meituanhotfix;

/**
 * Created by lixingyun on 16/9/29.
 */

public interface ChangeQuickRedirect {

    public Object accessDispatch(String methodSignature, Object[] paramArrayOfObject) ;

    public boolean isSupport(String methodSignature, Object[] paramArrayOfObject);

}
