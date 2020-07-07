package com.foobnix.ui2;

import android.app.Activity;
import android.app.IntentService;
import android.app.Notification;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.support.v4.media.session.MediaSessionCompat;

import androidx.core.app.NotificationCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.foobnix.android.utils.Apps;
import com.foobnix.android.utils.JsonDB;
import com.foobnix.android.utils.LOG;
import com.foobnix.android.utils.TxtUtils;
import com.foobnix.dao2.FileMeta;
import com.foobnix.drive.GFile;
import com.foobnix.ext.CacheZipUtils.CacheDir;
import com.foobnix.ext.EbookMeta;
import com.foobnix.model.AppData;
import com.foobnix.model.AppProfile;
import com.foobnix.model.AppSP;
import com.foobnix.model.AppState;
import com.foobnix.model.SimpleMeta;
import com.foobnix.model.TagData;
import com.foobnix.pdf.info.Clouds;
import com.foobnix.pdf.info.ExtUtils;
import com.foobnix.pdf.info.IMG;
import com.foobnix.pdf.info.R;
import com.foobnix.pdf.info.io.SearchCore;
import com.foobnix.pdf.info.model.BookCSS;
import com.foobnix.pdf.search.activity.msg.MessageSync;
import com.foobnix.pdf.search.activity.msg.MessageSyncFinish;
import com.foobnix.pdf.search.activity.msg.UpdateAllFragments;
import com.foobnix.sys.ImageExtractor;
import com.foobnix.sys.TempHolder;
import com.foobnix.tts.TTSNotification;
import com.google.api.client.googleapis.extensions.android.gms.auth.UserRecoverableAuthIOException;

import org.ebookdroid.common.settings.books.SharedBooks;
import org.greenrobot.eventbus.EventBus;

import java.io.File;
import java.util.LinkedList;
import java.util.List;

/**
 * 书本加载服务类
 * 这是一个加载书本的服务类
 * IntentService 默认为我们开启了一个工作线程，在任务执行完毕后，自动停止服务
 * 使用Service 可以同时执行多个请求，而使用IntentService 只能同时执行一个请求。
 */
public class BooksService extends IntentService {
    public static String TAG = "BooksService";
    public static String INTENT_NAME = "BooksServiceIntent";
    public static String ACTION_SEARCH_ALL = "ACTION_SEARCH_ALL";//查找数据库全部数据的指令
    public static String ACTION_REMOVE_DELETED = "ACTION_REMOVE_DELETED";//移除数据
    public static String ACTION_SYNC_DROPBOX = "ACTION_SYNC_DROPBOX";
    public static String ACTION_RUN_SYNCRONICATION = "ACTION_RUN_SYNCRONICATION";
    public static String RESULT_SYNC_FINISH = "RESULT_SYNC_FINISH";//扫描完毕
    public static String RESULT_SEARCH_FINISH = "RESULT_SEARCH_FINISH";
    public static String RESULT_BUILD_LIBRARY = "RESULT_BUILD_LIBRARY";//创建书库
    public static String RESULT_SEARCH_COUNT = "RESULT_SEARCH_COUNT";//搜索到的条目数
    //用volatile修饰的变量是对所有线程共享的、可见的，每次JVM都会读取最新写入的值并使其最新值在
    // 所有CPU可见。目的就是解决多线程中，同一时间多个线程对内存中的同一个变量操作的问题。
    public static volatile boolean isRunning = false;
    Handler handler;
    boolean isStartForeground = false;
    Runnable timer2 = new Runnable() {

        @Override
        public void run() {
            LOG.d("timer2");
            sendBuildingLibrary();
            handler.postDelayed(timer2, 250);
        }
    };
    private MediaSessionCompat mediaSessionCompat;
    private List<FileMeta> itemsMeta = new LinkedList<FileMeta>();
    Runnable timer = new Runnable() {

        @Override
        public void run() {
            LOG.d("timer 2");
            sendProggressMessage();
            handler.postDelayed(timer, 250);
        }
    };

    public BooksService() {
        super("BooksService");
        handler = new Handler();
        LOG.d("BooksService", "Create");
    }

    public static void sendFinishMessage(Context c) {
        Intent intent = new Intent(INTENT_NAME).putExtra(Intent.EXTRA_TEXT, RESULT_SEARCH_FINISH);
        LocalBroadcastManager.getInstance(c).sendBroadcast(intent);
    }

    public static void startForeground(Activity a, String action) {
        final Intent intent = new Intent(a, BooksService.class).setAction(action);
        a.startService(intent);

//        if (Build.VERSION.SDK_INT >= 26) {
//            a.startForegroundService(intent);
//        } else {
//            a.startService(intent);
//
//        }

    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        isStartForeground = false;
        LOG.d("BooksService", "onDestroy");
    }

    @Override
    public void onCreate() {
        super.onCreate();
        //startMyForeground();
    }

    public void startMyForeground() {
        if (!isStartForeground) {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.JELLY_BEAN) {
                Notification notification = new NotificationCompat.Builder(this, TTSNotification.DEFAULT) //
                        .setSmallIcon(R.drawable.glyphicons_748_synchronization1) //
                        .setContentTitle(Apps.getApplicationName(this)) //
                        .setContentText(getString(R.string.please_wait_books_are_being_processed_))
                        .setPriority(NotificationCompat.PRIORITY_DEFAULT)//
                        .build();

                startForeground(TTSNotification.NOT_ID_2, notification);
            }
            AppProfile.init(this);
            isStartForeground = true;
        }
    }


    //重写IntentService 的onHandleIntent()方法
    @Override
    protected void onHandleIntent(Intent intent) {
        //startMyForeground();

        // 这里已经是工作线程，在这里执行操作
        if (intent == null) {
            return;
        }

        try {
            sendProggressMessage();

            if (isRunning) {//服务处于启动中则返回
                LOG.d(TAG, "BooksService", "Is-running");
                return;
            }

            isRunning = true;
            LOG.d(TAG, "BooksService", "Action", intent.getAction());

            //TESET


            //当按按下刷新按键后，发送了的Action 为同步请求
            if (ACTION_RUN_SYNCRONICATION.equals(intent.getAction())) {
                if (AppSP.get().isEnableSync) {


                    AppProfile.save(this);


                    try {
                        EventBus.getDefault().post(new MessageSync(MessageSync.STATE_VISIBLE));
                        AppSP.get().syncTimeStatus = MessageSync.STATE_VISIBLE;
                        GFile.sycnronizeAll(this);

                        AppSP.get().syncTime = System.currentTimeMillis();
                        AppSP.get().syncTimeStatus = MessageSync.STATE_SUCCESS;
                        EventBus.getDefault().post(new MessageSync(MessageSync.STATE_SUCCESS));
                    } catch (UserRecoverableAuthIOException e) {
                        GFile.logout(this);
                        AppSP.get().syncTimeStatus = MessageSync.STATE_FAILE;
                        EventBus.getDefault().post(new MessageSync(MessageSync.STATE_FAILE));
                    } catch (Exception e) {
                        AppSP.get().syncTimeStatus = MessageSync.STATE_FAILE;
                        EventBus.getDefault().post(new MessageSync(MessageSync.STATE_FAILE));
                        LOG.e(e);
                    }

                    if (GFile.isNeedUpdate) {
                        LOG.d("GFILE-isNeedUpdate", GFile.isNeedUpdate);
                        TempHolder.get().listHash++;
                        EventBus.getDefault().post(new UpdateAllFragments());
                    }

                }

            }

            //正常打开APP,不是第一次进入的，会走这里
            //删除Action
            if (ACTION_REMOVE_DELETED.equals(intent.getAction())) {
                List<FileMeta> all = AppDB.get().getAll();

                for (FileMeta meta : all) {
                    if (meta == null) {
                        continue;
                    }

                    if (Clouds.isCloud(meta.getPath())) {//检查是不是云端的书籍
                        continue;
                    }

                    File bookFile = new File(meta.getPath());
                    //存储状态（如果介质存在并安装在其挂载点，具有*读/写访问权限）。
                    if (ExtUtils.isMounted(bookFile)) {//检查在该路径（挂载点）上，是否有该书籍并且拥有读写权限
                        if (!bookFile.exists()) {//如果没有，（某些书籍在后来删除了）
                            AppDB.get().delete(meta);//则把这些书籍信息从数据库中删除
                            LOG.d("BooksService Delete-setIsSearchBook", meta.getPath());
                        }
                    }

                }

                List<FileMeta> localMeta = new LinkedList<FileMeta>();
                if(JsonDB.isEmpty(BookCSS.get().searchPathsJson)){
                    sendFinishMessage();
                    return;
                }

                for (final String path : JsonDB.get(BookCSS.get().searchPathsJson)) {
                    if (path != null && path.trim().length() > 0) {
                        final File root = new File(path);
                        if (root.isDirectory()) {
                            LOG.d(TAG, "Search in " + root.getPath());
                            SearchCore.search(localMeta, root, ExtUtils.seachExts);
                        }
                    }
                }


                for (FileMeta meta : localMeta) {
                    if (!all.contains(meta)) {
                        FileMetaCore.createMetaIfNeedSafe(meta.getPath(), true);
                        LOG.d("BooksService add book", meta.getPath());
                    }
                }


                List<FileMeta> allNone = AppDB.get().getAllByState(FileMetaCore.STATE_NONE);
                for (FileMeta m : allNone) {
                    LOG.d("BooksService-createMetaIfNeedSafe-service", m.getTitle(),m.getPath(), m.getTitle());
                    FileMetaCore.createMetaIfNeedSafe(m.getPath(), false);
                }

                Clouds.get().syncronizeGet();

            }

            /* ********** 以上是 不是第一次打开app 的加载数据流程 ********** */

            /* ********** 第一次加载数据进入这里： 查找全部Action********** */

            else if (ACTION_SEARCH_ALL.equals(intent.getAction())) {
                LOG.d(ACTION_SEARCH_ALL);
                //TempHolder.listHash++;
                //AppDB.get().getDao().detachAll();

                AppProfile.init(this);

                ImageExtractor.clearErrors();//清除错误
                IMG.clearDiscCache();//清除光盘缓存

                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        IMG.clearMemoryCache();//清除内存缓存
                    }
                });




                AppDB.get().deleteAllData();//删除数据库所有数据
                itemsMeta.clear();

                handler.post(timer);//发送更新进度消息

                //path :是每一个文件夹路径
                for (final String path : JsonDB.get(BookCSS.get().searchPathsJson)) {
                    if (path != null && path.trim().length() > 0) {
                        final File root = new File(path);
                        if (root.isDirectory()) {//如果这个文件是一个路径
                            LOG.d("Search in: " + root.getPath());
                            SearchCore.search(itemsMeta, root, ExtUtils.seachExts);//继续递归调用。seachExts是需要查找的后缀文件类型的集合
                        }
                    }
                }

                //上面的递归查找文件夹结束后，所有 书元类 （FileMeta） 只有 path属性有数据


                for (FileMeta meta : itemsMeta) {
                    meta.setIsSearchBook(true);//标记为该次扫描查找中的目标书籍
                }

                //其他不在该次扫描中查找的书籍，统一集中计算起来
                final List<SimpleMeta> allExcluded = AppData.get().getAllExcluded();

                if (TxtUtils.isListNotEmpty(allExcluded)) {
                    for (FileMeta meta : itemsMeta) {
                        if (allExcluded.contains(SimpleMeta.SyncSimpleMeta(meta.getPath()))) {
                            meta.setIsSearchBook(false);//标记为非目标书籍
                        }
                    }
                }

                final List<FileMeta> allSyncBooks = AppData.get().getAllSyncBooks();
                if (TxtUtils.isListNotEmpty(allSyncBooks)) {
                    for (FileMeta meta : itemsMeta) {
                        for (FileMeta sync : allSyncBooks) {
                            if (meta.getTitle().equals(sync.getTitle()) && !meta.getPath().equals(sync.getPath())) {
                                meta.setIsSearchBook(false);
                                LOG.d(TAG, "remove-dublicate", meta.getPath());
                            }
                        }

                    }
                }


                itemsMeta.addAll(AppData.get().getAllFavoriteFiles(false));
                itemsMeta.addAll(AppData.get().getAllFavoriteFolders());

                //这条作用是把上面插入的数据生成主键
                AppDB.get().saveAll(itemsMeta);//全部保存到数据库

                handler.removeCallbacks(timer);//停止更新进度信息
                //加载完所有书本的地址后通知 listview更新书本数据
                sendFinishMessage();//debug模式下，第一次进入app加载数据，进过这个方法的时候list上已经出现完整的list数据

                handler.post(timer2);//通知创建书库

                //程序来到这里每一个FileMeta 里面只有 path有数据
                //一下这段是填上书本的其他信息
                for (FileMeta meta : itemsMeta) {
                    File file = new File(meta.getPath());
                    FileMetaCore.get().upadteBasicMeta(meta, file);
                }

                AppDB.get().updateAll(itemsMeta);
                sendFinishMessage();


                for (FileMeta meta : itemsMeta) {
                    if(FileMetaCore.isSafeToExtactBook(meta.getPath())) {
                        EbookMeta ebookMeta = FileMetaCore.get().getEbookMeta(meta.getPath(), CacheDir.ZipService, true);
                        FileMetaCore.get().udpateFullMeta(meta, ebookMeta);//填上所有书元信息
                    }
                }

                SharedBooks.updateProgress(itemsMeta, true,-1);
                AppDB.get().updateAll(itemsMeta);//更新全部


                itemsMeta.clear();

                handler.removeCallbacks(timer2);
                sendFinishMessage();//完成扫描
                CacheDir.ZipService.removeCacheContent();

                Clouds.get().syncronizeGet();

                TagData.restoreTags();


                List<FileMeta> allNone = AppDB.get().getAllByState(FileMetaCore.STATE_NONE);
                for (FileMeta m : allNone) {
                    LOG.d("BooksService-createMetaIfNeedSafe-service", m.getTitle(),m.getPath(), m.getTitle());
                    FileMetaCore.createMetaIfNeedSafe(m.getPath(), false);
                }

                updateBookAnnotations();


            }

            //以上时第一次加载书本数据

            else if (ACTION_SYNC_DROPBOX.equals(intent.getAction())) {
                Clouds.get().syncronizeGet();

            }


        } finally {
            sendFinishMessage();
            isRunning = false;

        }
        //stopSelf();
    }

    public void updateBookAnnotations() {

        if (AppState.get().isDisplayAnnotation) {
            sendBuildingLibrary();
            LOG.d("updateBookAnnotations begin");
            List<FileMeta> itemsMeta = AppDB.get().getAll();
            for (FileMeta meta : itemsMeta) {
                if (TxtUtils.isEmpty(meta.getAnnotation())) {
                    String bookOverview = FileMetaCore.getBookOverview(meta.getPath());
                    meta.setAnnotation(bookOverview);
                }
            }
            AppDB.get().updateAll(itemsMeta);
            sendFinishMessage();
            LOG.d("updateBookAnnotations end");
        }

    }

    //完成发送
    private void sendFinishMessage() {
        try {
            //AppDB.get().getDao().detachAll();//关闭数据库
        } catch (Exception e) {
            LOG.e(e);
        }

        sendFinishMessage(this);
        EventBus.getDefault().post(new MessageSyncFinish());
    }

    //更新进度，主要用来显示扫描进度到多少了
    private void sendProggressMessage() {
        Intent itent = new Intent(INTENT_NAME).putExtra(Intent.EXTRA_TEXT, RESULT_SEARCH_COUNT).putExtra("android.intent.extra.INDEX", itemsMeta.size());
        LocalBroadcastManager.getInstance(this).sendBroadcast(itent);
    }

    private void sendBuildingLibrary() {
        Intent itent = new Intent(INTENT_NAME).putExtra(Intent.EXTRA_TEXT, RESULT_BUILD_LIBRARY);
        LocalBroadcastManager.getInstance(this).sendBroadcast(itent);
    }

}
