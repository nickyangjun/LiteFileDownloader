# LiteFileDownloader
File Downloader,  Simple is the highest. Fastest, lightweight, easy to extend

## Download

**Gradle:**

Step 1. Add the JitPack repository to your build file Add it in your root build.gradle at the end of repositories:
```
    allprojects {
        repositories {
            ...
            maven { url 'https://jitpack.io' }
        }
    }
```

Step 2. Add the dependency
```
    dependencies {
            compile 'com.github.nickyangjun:LiteFileDownloader:v1.0.2'
    }
```

Or Maven:
```
    <repositories>
        <repository>
            <id>jitpack.io</id>
            <url>https://jitpack.io</url>
        </repository>
    </repositories>

    <dependency>
        <groupId>com.github.nickyangjun</groupId>
        <artifactId>LiteFileDownloader</artifactId>
        <version>v1.0.2</version>
    </dependency>
```

## How do I use LiteFileDownloader?

Step 1. build a global FileDownloader
```java
    FileDownloader downloader = FileDownloader
                    .createBuilder()
                    .build();

```

Step 2. start a request
```java
    Request request = Request
                .createBuilder()
                .url("url")
                .build();
    Task task = downloader.newTask(request);
    task.enqueue(new DownloadListener() {
        @Override
        public void onStart(Request request) {
        }

        @Override
        @ExecuteMode(threadMode = ThreadMode.MAIN)
        public void onProgress(Request request, long curBytes, long totalBytes){
        }

        @Override
        public void onPause(Request request) {
        }

        @Override
        public void onRestart(Request request) {
        }

        @Override
        public void onFinished(Request request) {
        }

        @Override
        public void onCancel(Request request) {
        }

        @Override
        public void onFailed(Request request, Exception e) {
        }
    });
```

If you want the DownloadListener to callbck in Android Main thread, please use the annotation of ExecuteMode.

enjoy yourself.
