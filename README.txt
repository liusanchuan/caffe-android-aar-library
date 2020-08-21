@[toc]
# caffe android aar库
made by sanchuan

## 运行
直接下载本代码库，用android studio 打开就可以运行。

## 如果在自己的项目中用
如果你想把AAR库继承到自己的项目中，可以下载`app/libs/caffe_android.aar`到自己的项目中，

### 1 添加本地AAR

（1） 把 caffe_android.aar文件复制到新项目的`app/libs`下

（2） 在`build.gradle(app)`中添加引用:

~~~shell
apply plugin: 'com.android.application'

android {
... ...
}
// 这个
repositories {
    flatDir{
        dirs 'libs'
    }
}
dependencies {
    compile (name:'caffe_mobile',ext:'aar') // 和这个
... ...
}
~~~

### 2 Java代码示例

#### （1） 申请权限

AndroidManifest.xml 中

~~~xml
<uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE"/>
~~~

java代码中

~~~java
private void checkMyPermission() {

    if(ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)!= PackageManager.PERMISSION_GRANTED){
        ActivityCompat.requestPermissions(this,new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},1);
    }
}
~~~

#### （2）设置运行主函数

~~~java
// 全局变量
String[] IMAGENET_CLASSES;
CaffeMobile caffeMobile;
String[] IMAGENET_CLASSES;
CNNTask cnnTask;

// 初始化网络
void init_net(){
    // 1. 定义模型路径
    String modelDir = "/sdcard/caffe_mobile/bvlc_reference_caffenet";
    String modelProto = modelDir + "/deploy.prototxt";
    String modelBinary = modelDir + "/bvlc_reference_caffenet.caffemodel";
    // 2 加载标签列表
    AssetManager am = this.getAssets();
    try {
        InputStream is = am.open("synset_words.txt");
        Scanner sc = new Scanner(is);
        List<String> lines = new ArrayList<String>();
        while (sc.hasNextLine()) {
            final String temp = sc.nextLine();
            lines.add(temp.substring(temp.indexOf(" ") + 1));
        }
        IMAGENET_CLASSES = lines.toArray(new String[0]);
    } catch (IOException e) {
        e.printStackTrace();
    }
    // 3. 创建caffe 解释器实例
    caffeMobile = new CaffeMobile();
    caffeMobile.setNumThreads(4);
    caffeMobile.loadModel(modelProto, modelBinary);
    float[] meanValues = {104, 117, 123};
    caffeMobile.setMean(meanValues);
    // 4 创建输出结果监听器
    cnnTask = new CNNTask(new CNNListener() {
        @Override
        public void onTaskCompleted(int i) {
            Toast.makeText(getApplicationContext(),"鉴定结果："+ IMAGENET_CLASSES[i],Toast.LENGTH_SHORT).show();
        }
    });
}

~~~

#### （3）异步运行的监听器类

```java
private class CNNTask extends AsyncTask<String, Void, Integer> {
    private CNNListener listener;
    private long startTime;

    public CNNTask(CNNListener listener) {
        this.listener = listener;
    }
	// 监听器会把这个任务放后台执行
    @Override
    protected Integer doInBackground(String... strings) {
        startTime = SystemClock.uptimeMillis();
        return caffeMobile.predictImage(strings[0])[0];
    }

    @Override
    protected void onPostExecute(Integer integer) {
        Log.i(LOG_TAG, String.format("elapsed wall time: %d ms", SystemClock.uptimeMillis() - startTime));
        listener.onTaskCompleted(integer);
        super.onPostExecute(integer);
    }
}
```

#### (3) 启动神经网络识别任务

```java
// 输入一张图片的路径，在监听器中获取结果
void run_caffe(String imgPath){
    cnnTask.execute(imgPath);
}
```

