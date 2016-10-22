---
title: 热修复之美团方案
tags: [Android热修复]
categories: [Android]
date: 2016-09-30 19:49:24
---

最近在研究热修复的一些框架，发现目前市面上有很多的热修复的一些思想和框架，有腾讯、阿里、美团等大型APP一些实践过的，有为我们大家扩展思路的Demo，但是这些热修复技术的活跃而蓬勃发展，让我们有更多的选择。

但是按照实现方式上，Android热补丁技术应该分为以下两个流派：

* Native，代表有阿里的Dexposed、[AndFix](https://github.com/alibaba/AndFix)与腾讯的内部方案KKFix；     
    
* Java, 代表有Qzone的超级补丁Dex插桩的方式、[大众点评的nuwa](https://github.com/jasonross/Nuwa)、[百度金融的rocooFix](https://github.com/dodola/RocooFix), 饿了么的[amigo](https://github.com/eleme/Amigo/)以及美团的robust。

这里我们不做拓展，Qzone的超级补丁Dex插桩的方式之前讲过啦，就不在啰嗦，今天我们要分析的是美团的`robust`.美团的`robust`暂时还没开源，不过美团公布了他们的实践思路 ----[Android热更新方案Robust](http://tech.meituan.com/android_robust.html)。

# 原理
`Robust插件`对每个产品代码的每个函数都在编译打包阶段自动的插入了一段代码,这段代码实际上相当于多了个分支，当满足这些分支时执行这段新加入的代码。

在开始之前还是给大家讲一个谍战片：
剧情是这样滴，那是一个战争年代，某红色特务机构在敌方的各个连队都有一个间谍的坑，当有大活动发生时"上头"都会下发一个替换命令到敌方各个连队里。当间谍收到信息后，看到满足"上头"给出的信息（连队名称、对应的代码）和自己的一模一样时时，就意味着自己需要完成上级给的任务，同时接应同志来伪装完成"上头"下发的任务。对于那些不满足上头给予的信息的间谍，仍原地待命.

`Robust插件`热修复其实和这个谍战的剧情一样一样滴，上头下发的信息，会告诉我们下发的连队名称和接应我们去实施的人，这些位置信息其实就是出问题的那个类，需要打补丁，我们称之为**补丁类**，有人来替卧底实施，我们称之为**锲子类**。


好了回归代码，来看下美团技术团队的大概思路。


旧代码：
```java
public long getIndex() {
        return 100L;
    }
```

<span id="qw">被处理成如下的实现</span>
```java
public static ChangeQuickRedirect     changeQuickRedirect;    
public long getIndex() {    
        if(changeQuickRedirect != null) {    
            //PatchProxy中封装了获取当前className和methodName的逻辑，并在其内部最终调用了changeQuickRedirect的对应函数
            if(PatchProxy.isSupport(new Object[0], this, changeQuickRedirect, false)) {     
                return ((Long)PatchProxy.accessDispatch(new Object[0], this, changeQuickRedirect, false)).longValue();    
            }
        }
        return 100L;
    }
```

`ChangeQuickRedirect`是一个接口，每个类中都有这个静态变量，这就相当于那个卧底，不过他是个接口.


下发补丁类：
```java
public class PatchesInfoImpl implements PatchesInfo {
    public List<PatchedClassInfo> getPatchedClassesInfo() {
        List<PatchedClassInfo> patchedClassesInfos = new ArrayList<PatchedClassInfo>();
        PatchedClassInfo patchedClass = new PatchedClassInfo("com.meituan.sample.d", StatePatch.class.getCanonicalName());
        patchedClassesInfos.add(patchedClass);
        return patchedClassesInfos;
    }
}
```

 >上述代码就是解释 com.meituan.sample.d（混淆后的）需要需要修复，并且插入的锲子类StatePatch.class.getCanonicalName()

当app加载时通过`PatchesInfoImpl`这个类中的getPatchedClassesInfo的信息，来获取那些需要打补丁（而不是每个类都打补丁，这就相当于下发补丁类），以及将补丁锲子替换之前类的锲子（实际上就是用我们下发的ChangeQuickRedirect来替换补丁类的ChangeQuickRedirect的静态变量。）




State类的锲子类 StatePatch.java  这就是我们说的需要赋值给patch类的锲子。
```java
public class StatePatch implements ChangeQuickRedirect {
    @Override
    public Object accessDispatch(String methodSignature, Object[] paramArrayOfObject) {
        String[] signature = methodSignature.split(":");
        if (TextUtils.equals(signature[1], "a")) {//long getIndex() -> a
            return 106;
        }
        return null;
    }

    @Override
    public boolean isSupport(String methodSignature, Object[] paramArrayOfObject) {
        String[] signature = methodSignature.split(":");
        if (TextUtils.equals(signature[1], "a")) {//long getIndex() -> a
            return true;
        }
        return false;
    }
}
```
这个大概就是判断当前方法是不是a（long getIndex() 混淆后的），要是就执行`accessDispatch()`,这个是吧返回值给改成了106。
大概流程图:
![](http://7xj9f0.com1.z0.glb.clouddn.com/meituan_patch.png)


# 初步实现

由于只是原理性调研，加上现在美团提出这个热修后一直未放出源码，我按捺不住自己模仿实现了一个demo，可以分享给大家。

# 关键代码实现

* [PatchedClassInfo](#PatchedClassInfo) 存放下发patch类和锲子类
* [PatchProxy 工具类](#PatchProxy)
通过上下文获取方法的名称和参数，以及执行ChangeQuickRedirect中的方法，[参看之前的实现getIndex()](#qw)的第5行、第6行。
* [loadDex()]()


<span id="PatchedClassInfo">PatchedClassInfo.java</span>
```java
public class PatchedClassInfo {

    String clazz;//patch类
    String pathRedicIMpl;//锲子类

    public PatchedClassInfo(String clazz, String pathRedicIMpl) {
        this.clazz = clazz;
        this.pathRedicIMpl = pathRedicIMpl;
    }

    public String getClazz() {
        return clazz;
    }

    public String getPathRedicIMpl() {
        return pathRedicIMpl;
    }
}
```

<span id="PatchProxy">PatchProxy.java</span>
```java
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

```


<span id="loaddex">loadDex()</span>
```java
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
```

项目已经开源到github [MeituanHotFix](https://github.com/ownwell/MeituanHotFix)