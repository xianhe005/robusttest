## Android 热修复技术值_Robust
### 技术介绍
技术|介绍
:---:|:---:
热更新|修改某处我们需要进行少量修改的地方或者Bug，修复快，时效性高
增量更新|原有app的基础上只更新发生变化的地方，其余保持原样
升级更新|在当前的版本做了大的修改时，我们需要全部下载Apk进行升级
### Robust的实现
CC是一套Android的组件化框架，由CC核心API类库和cc-register插件组成

### 流程：
1. 集成 Robust，生成 apk。保存期间的混淆文件 mapping.txt，以及 Robust 生成记录文件 methodMap.robust
2. 使用注解 @Modify 或者方法 RobustModify.modify() 标注需要修复的方法
3. 开启补丁插件，执行生成 apk 命令，获得补丁包 patch.jar
4. 通过推送或者接口的形式，通知 app 有补丁，需要修复
5. 加载补丁文件不需要重新启动应用
### 一、添加依赖：
1.添加`classpath 'com.meituan.robust:gradle-plugin:0.4.85' classpath 'com.meituan.robust:auto-patch-plugin:0.4.85'`到build.gradle中
```
buildscript {
    repositories {
        jcenter()
    }
    dependencies {
        classpath 'com.android.tools.build:gradle:2.2.2'
        classpath 'com.meituan.robust:gradle-plugin:0.4.85'
        classpath 'com.meituan.robust:auto-patch-plugin:0.4.85'
        // NOTE: Do not place your application dependencies here; they belong
        // in the individual module build.gradle files
    }
}
```
2.添加`implementation 'com.meituan.robust:robust:0.4.85'`到app-build.gradle的dependencies中
```
dependencies {
    compile fileTree(include: ['*.jar'], dir: 'libs')
    androidTestCompile('com.android.support.test.espresso:espresso-     core:2.2.2', {
        exclude group: 'com.android.support', module: 'support-annotations'
    })
    compile 'com.android.support:appcompat-v7:25.0.0'
    implementation 'com.meituan.robust:robust:0.4.85'
    testCompile 'junit:junit:4.12'
}
```
3.添加两种模式到app-build.gradle中，具体看注解
```
apply plugin: 'com.android.application'
//apply plugin: 'auto-patch-plugin'  //生成插件是打开
apply plugin: 'robust'//生成Apk时打开
```
4.在app-build.gradle打开混淆，如果不打开混淆，编译Apk时将不生成mipping.txt文件，Robust更新会报错，找不到此文件，具体看后面
```
buildTypes {
        release {
            // minifyEnabled false
            minifyEnabled true
            proguardFiles getDefaultProguardFile('proguard-android.txt'), 'proguard-rules.pro'
            signingConfig signingConfigs.meituan
        }
        debug {
            minifyEnabled true
            // minifyEnabled false
            proguardFiles getDefaultProguardFile('proguard-android.txt'), 'proguard-rules.pro'
        }
    }
```
5.混淆proguard-rules.pro,形如：
```
-dontwarn
-keepattributes Signature,SourceFile,LineNumberTable
-keepattributes *Annotation*
-keeppackagenames
-ignorewarnings
-dontwarn android.support.v4.**,**CompatHoneycomb,com.tenpay.android.**
-optimizations !class/unboxing/enum,!code/simplification/arithmetic
```
**注意：程序需要签名，不签名会导致安装Apk时提示文件损坏**
### 二、添加robust.xml到app根目录中，可从demo中复制,修改以下部分(packname和patchPackname修改成应用的包名)：
```
<!--需要热补的包名或者类名，这些包名下的所有类都被会插入代码-->
<!--这个配置项是各个APP需要自行配置，就是你们App里面你们自己代码的包名，
这些包名下的类会被Robust插入代码，没有被Robust插入代码的类Robust是无法修复的-->
<packname name="hotfixPackage">
    <name>com.hxh.robusttest</name>
</packname>

<!--不需要Robust插入代码的包名，Robust库不需要插入代码，如下的配置项请保留，还可以根据各个APP的情况执行添加-->
<exceptPackname name="exceptPackage">
</exceptPackname>

<!--补丁的包名，请保持和类PatchManipulateImp中fetchPatchList方法中设置的补丁类名保持一致（ patch.setPatchesInfoImplClassFullName("com.hxh.robusttest.PatchesInfoImpl")），
各个App可以独立定制，需要确保的是setPatchesInfoImplClassFullName设置的包名是如下的配置项，类名必须是：PatchesInfoImpl-->
<patchPackname name="patchPackname">
    <name>com.hxh.robusttest</name>
</patchPackname>
```
robust配置完成。
### 三、基本应用程序
需要开启的权限，如果是targetSdkVersion>=23即6.0及以上，还需进行动态权限的处理：
```
<uses-permission android:name="android.permission.INTERNET" />
<uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE" />
<uses-permission android:name="android.permission.MOUNT_UNMOUNT_FILESYSTEMS" />
<uses-permission android:name="android.permission.READ_EXTERNAL_STORAGE" />
```
1.测试主界面MainActivity
```
package com.hxh.robusttest;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.meituan.robust.PatchExecutor;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        init();
    }

    private void init() {
        Button btn = findViewById(R.id.btn);
        Button second = findViewById(R.id.btn_seconde);
        btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (isGrantSDCardReadPermission()) {
                    runRobust();
                } else {
                    requestPermission();
                }
            }
        });
        second.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(MainActivity.this, SecondActivity.class));
            }
        });
    }

    private boolean isGrantSDCardReadPermission() {
        return PermissionUtils.isGrantSDCardReadPermission(this);
    }

    private void requestPermission() {
        PermissionUtils.requestSDCardReadPermission(this, REQUEST_CODE_SDCARD_READ);
    }

    private static final int REQUEST_CODE_SDCARD_READ = 1;

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case REQUEST_CODE_SDCARD_READ:
                handlePermissionResult();
                break;

            default:
                break;
        }
    }

    private void handlePermissionResult() {
        if (isGrantSDCardReadPermission()) {
            runRobust();
        } else {
            Toast.makeText(this, "failure because without sd card read permission", Toast.LENGTH_SHORT).show();
        }

    }

    private void runRobust() {
        new PatchExecutor(getApplicationContext(), new PatchManipulateImp(), new RobustCallBackSample()).start();
    }
}
```
声明两个按钮，一个更新按钮，一个跳转有bug的页面，其中需要注意的是更新所使用的方法为PatchExecutor，参数一、当前上下文，参数二，关联patch.jar的信息，参数三、回调

PatchManipulateImp .java
```
package com.hxh.robusttest;

import android.content.Context;
import android.os.Environment;
import android.util.Log;

import com.meituan.robust.Patch;
import com.meituan.robust.PatchManipulate;
import com.meituan.robust.RobustApkHashUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by mivanzhang on 17/2/27.
 *
 * We recommend you rewrite your own PatchManipulate class ,adding your special patch Strategy，in the demo we just load the patch directly
 *
 * <br>
 *   Pay attention to the difference of patch's LocalPath and patch's TempPath
 *
 *     <br>
 *    We recommend LocalPath store the origin patch.jar which may be encrypted,while TempPath is the true runnable jar
 *<br>
 *<br>
 *    我们推荐继承PatchManipulate实现你们App独特的A补丁加载策略，其中setLocalPath设置补丁的原始路径，这个路径存储的补丁是加密过得，setTempPath存储解密之后的补丁，是可以执行的jar文件
 *     <br>
 *     setTempPath设置的补丁加载完毕即刻删除，如果不需要加密和解密补丁，两者没有啥区别
 */

public class PatchManipulateImp extends PatchManipulate {
    /***
     * connect to the network ,get the latest patches
     * l联网获取最新的补丁
     * @param context
     *
     * @return
     */
    @Override
    protected List<Patch> fetchPatchList(Context context) {
        //将app自己的robustApkHash上报给服务端，服务端根据robustApkHash来区分每一次apk build来给app下发补丁
        //apkhash is the unique identifier for  apk,so you cannnot patch wrong apk.
        String robustApkHash = RobustApkHashUtils.readRobustApkHash(context);
        Log.w("robust","robustApkHash :" + robustApkHash);
        //connect to network to get patch list on servers
        //在这里去联网获取补丁列表
        Patch patch = new Patch();
        patch.setName("123");
        //we recommend LocalPath store the origin patch.jar which may be encrypted,while TempPath is the true runnable jar
        //LocalPath是存储原始的补丁文件，这个文件应该是加密过的，TempPath是加密之后的，TempPath下的补丁加载完毕就删除，保证安全性
        //这里面需要设置一些补丁的信息，主要是联网的获取的补丁信息。重要的如MD5，进行原始补丁文件的简单校验，以及补丁存储的位置，这边推荐把补丁的储存位置放置到应用的私有目录下，保证安全性
        patch.setLocalPath(Environment.getExternalStorageDirectory().getPath()+ File.separator+"robust"+ File.separator + "patch");

        //setPatchesInfoImplClassFullName 设置项各个App可以独立定制，需要确保的是setPatchesInfoImplClassFullName设置的包名是和xml配置项patchPackname保持一致，而且类名必须是：PatchesInfoImpl
        //请注意这里的设置
        patch.setPatchesInfoImplClassFullName("com.hxh.robusttest.PatchesInfoImpl");
        List patches = new ArrayList<Patch>();
        patches.add(patch);
        return patches;
    }

    /**
     *
     * @param context
     * @param patch
     * @return
     *
     * you can verify your patches here
     */
    @Override

    protected boolean verifyPatch(Context context, Patch patch) {
        //do your verification, put the real patch to patch
        //放到app的私有目录
        patch.setTempPath(context.getCacheDir()+ File.separator+"robust"+ File.separator + "patch");
        //in the sample we just copy the file
        try {
            copy(patch.getLocalPath(), patch.getTempPath());
        }catch (Exception e){
            e.printStackTrace();
            throw new RuntimeException("copy source patch to local patch error, no patch execute in path "+patch.getTempPath());
        }

        return true;
    }
    public void copy(String srcPath, String dstPath) throws IOException {
        File src=new File(srcPath);
        if(!src.exists()){
            throw new RuntimeException("source patch does not exist ");
        }
        File dst=new File(dstPath);
        if(!dst.getParentFile().exists()){
            dst.getParentFile().mkdirs();
        }
        InputStream in = new FileInputStream(src);
        try {
            OutputStream out = new FileOutputStream(dst);
            try {
                // Transfer bytes from in to out
                byte[] buf = new byte[1024];
                int len;
                while ((len = in.read(buf)) > 0) {
                    out.write(buf, 0, len);
                }
            } finally {
                out.close();
            }
        } finally {
            in.close();
        }
    }
    /**
     *
     * @param patch
     * @return
     *
     * you may download your patches here, you can check whether patch is in the phone
     */
    @Override
    protected boolean ensurePatchExist(Patch patch) {
        return true;
    }
}
```
RobustCallBackSample.java
```
package com.hxh.robusttest;

import android.util.Log;

import com.meituan.robust.Patch;
import com.meituan.robust.RobustCallBack;

import java.util.List;

/**
 * Created by HXH at 2019/3/7
 * RobustCallBack
 */
public class RobustCallBackSample implements RobustCallBack {


    /*@Override
    public void onPatchListFetched(boolean result, boolean isNet) {
        Log.i("RobustCallBack", "onPatchListFetched result: " + result);
    }*/

    @Override
    public void onPatchListFetched(boolean result, boolean isNet, List<Patch> patches) {
        Log.i("RobustCallBack", "onPatchListFetched result: " + result);
    }

    @Override
    public void onPatchFetched(boolean result, boolean isNet, Patch patch) {
        Log.i("RobustCallBack", "onPatchFetched result: " + result);
        Log.i("RobustCallBack", "onPatchFetched isNet: " + isNet);
        Log.i("RobustCallBack", "onPatchFetched patch: " + patch.getName());
    }

    @Override
    public void onPatchApplied(boolean result, Patch patch) {
        Log.i("RobustCallBack", "onPatchApplied result: " + result);
        Log.i("RobustCallBack", "onPatchApplied patch: " + patch.getName());

    }

    @Override
    public void logNotify(String log, String where) {
        Log.i("RobustCallBack", "logNotify log: " + log);
        Log.i("RobustCallBack", "logNotify where: " + where);
    }

    @Override
    public void exceptionNotify(Throwable throwable, String where) {
        Log.e("RobustCallBack", "exceptionNotify where: " + where, throwable);
    }
}
```
2.SecondActivity.java 第二个界面 声明了一个TextView控件
```
public class SecondActivity extends AppCompatActivity {
    TextView t;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_second);
        t = findViewById(R.id.text);
        t.setText("未修改");
    }
}
```
### 四、编译生成Apk（PS：也就是有Bug（SecondActivity.java中的TextView为“未修改”））
打开美团生成Apk模式
```
apply plugin: 'com.android.application'
//apply plugin: 'auto-patch-plugin'  //生成插件是打开
apply plugin: 'robust'//生成Apk时打开
```
编译指令：gradlew clean assembleRelease --stacktrace --no-daemon
<div align=center><img style="width:auto;" src="/1.png"/></div>
创建app/robust文件夹,将outputs/mapping/release中的mipping.txt和outputs/robust中的methodsMap.robust文件考到app/robust文件下

mipping.txt：该文件列出了原始的类，方法和字段名与混淆后代码间的映射。这个文件很重要，可以用它来翻译被混淆的代码

methodsMap.robust：该文件在打补丁的时候用来区别到底哪些方法需要被修复，所以有它才能打补丁
将生成的apk安装到手机上。

### 五、生成补丁包
1.打开美团补丁模式 app-build.gradle：
```
apply plugin: 'com.android.application'
apply plugin: 'auto-patch-plugin'  //生成插件是打开
//apply plugin: 'robust'//生成Apk时打开
```
2.模拟修改Bug
```
package com.hxh.robusttest;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.widget.TextView;
import android.widget.Toast;

import com.meituan.robust.patch.annotaion.Add;
import com.meituan.robust.patch.annotaion.Modify;

/**
 * Created by HXH at 2019/3/7
 * second
 */
public class SecondActivity extends AppCompatActivity {
    TextView t;

    @Override
    @Modify
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_second);
        t = findViewById(R.id.text);
        //t.setText("未修改");
        t.setText("已经修改");
        addMethod();
    }

    @Add
    private void addMethod() {
        Toast.makeText(this, "add", Toast.LENGTH_SHORT).show();
    }
}
```
3.编译程序，使用同样的命令：gradlew clean assembleRelease --stacktrace --no-daemon
虽然提示BUILD FAILED，没有关系，只要auto patch end successfully就表明已经生成补丁包了，补丁包位于:outputs/robust/patch.jar。

将更新包考到手机的robust文件夹中即可，注意：手机需要手动开启存储权限，没有开启会报：未找到...../robust/patch.jar

点击MainActivity中的btn加载更新补丁，然后到SecondActivity页面发现已经能修复了。

**特别注意：杀死进程后，再打开app，还得重新加载更新补丁，否则还是原来的apk包内容。**

