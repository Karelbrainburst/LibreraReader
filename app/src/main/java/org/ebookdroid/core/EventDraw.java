package org.ebookdroid.core;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.graphics.RectF;
import android.text.TextPaint;
import android.util.Log;

import androidx.core.graphics.ColorUtils;

import com.foobnix.android.utils.Dips;
import com.foobnix.android.utils.LOG;
import com.foobnix.model.AppSP;
import com.foobnix.model.AppState;
import com.foobnix.pdf.info.R;
import com.foobnix.pdf.info.model.BookCSS;
import com.foobnix.pdf.info.wrapper.MagicHelper;

import org.ebookdroid.LibreraApp;
import org.ebookdroid.core.codec.PageLink;
import org.ebookdroid.ui.viewer.IActivityController;
import org.emdev.utils.LengthUtils;

import java.util.Queue;

/**
 * 绘制事件
 */
public class EventDraw implements IEvent {

    static Paint rect = new Paint();

    static {//设置描绘的画笔属性
        rect.setColor(Color.DKGRAY);
        rect.setStrokeWidth(Dips.DP_1);
        rect.setStyle(Style.STROKE);

    }

    final RectF fixedPageBounds = new RectF();
    private final Queue<EventDraw> eventQueue;
    public ViewState viewState;
    public PageTreeLevel level;
    public Canvas canvas;
    RectF pageBounds;//页面边界
    Paint paintWrods = new Paint();
    private IActivityController base;

    EventDraw(final Queue<EventDraw> eventQueue) {
        this.eventQueue = eventQueue;
        paintWrods.setAlpha(60);
        paintWrods.setStrokeWidth(Dips.dpToPx(1));
        paintWrods.setTextSize(30);
    }

    void init(final ViewState viewState, final Canvas canvas, IActivityController base) {
        this.viewState = viewState;
        this.base = base;
        this.level = PageTreeLevel.getLevel(viewState.zoom);
        this.canvas = canvas;

    }

    void init(final EventDraw event, final Canvas canvas, IActivityController base) {
        this.base = base;
        this.viewState = event.viewState;
        this.level = event.level;
        this.canvas = canvas;
    }

    void release() {
        this.canvas = null;
        this.level = null;
        this.pageBounds = null;
        this.viewState = null;
        eventQueue.offer(this);
    }

    /**
     * 线程处理
     * 每当滚动阅读界面，都会触发这里绘制界面
     * @return
     */
    @Override
    public ViewState process() {
        try {
            Log.v("EventDraw","process 111");
            if (AppState.get().isOLED && !AppState.get().isDayNotInvert /* && MagicHelper.getBgColor() == Color.BLACK */) {
                viewState.paint.backgroundFillPaint.setColor(Color.BLACK);//背景补漆，每次打开小说阅读界面时会优先填上一片黑色
                Log.v("EventDraw","process 333");
            } else {
                viewState.paint.backgroundFillPaint.setColor(MagicHelper.ligtherColor(MagicHelper.getBgColor()));
            }
            if (canvas != null) {
                canvas.drawRect(canvas.getClipBounds(), viewState.paint.backgroundFillPaint);//将黑色的背景画上画布
                Log.v("EventDraw","process 444");
            }

            viewState.ctrl.drawView(this);//整个阅读界面绘制出来
            return viewState;
        } finally {
            release();
        }
    }

    /**
     * 上面的process方法最终会调用到这里来执行真正的绘制
     * @param page
     * @return
     */
    @Override
    public boolean process(final Page page) {
        pageBounds = viewState.getBounds(page);

        drawPageBackground(page);

        final boolean res = process(page.nodes);
        Log.v("EventDraw","process 222");
        if (MagicHelper.isNeedBookBackgroundImage()) {//是需要书背景图片

            if (MagicHelper.isNeedBookBackgroundImage()) {
                // viewState.paint.bitmapPaint.setAlpha(MagicHelper.getTransparencyInt());
            }

            Bitmap bgBitmap = MagicHelper.getBackgroundImage();
            Matrix m = new Matrix();
            float width = fixedPageBounds.width();
            float height = fixedPageBounds.height();
            m.setScale(width / bgBitmap.getWidth(), height / bgBitmap.getHeight());
            m.postTranslate(fixedPageBounds.left, fixedPageBounds.top);

            Paint p = new Paint();
            p.setAlpha(255 - MagicHelper.getTransparencyInt());
            canvas.drawBitmap(MagicHelper.getBackgroundImage(), m, p);
            Log.v("EventDraw","process 555");
        }
        if (AppState.get().isOLED && !AppState.get().isDayNotInvert/* && !TempHolder.get().isTextFormat */) {//刚开始不修改时，结果为false&&false = false
            canvas.drawRect(fixedPageBounds.left - Dips.DP_1, fixedPageBounds.top - Dips.DP_1, fixedPageBounds.right + Dips.DP_1, fixedPageBounds.bottom + Dips.DP_1, rect);
            Log.v("EventDraw","process 666");
        }

        if (AppState.get().isShowLastPageRed && AppSP.get().readingMode == AppState.READING_MODE_MUSICIAN && page.isLastPage) {//true&&false &&false =false
            rect.setColor(ColorUtils.setAlphaComponent(Color.RED, 150));
            rect.setStyle(Style.FILL);
            canvas.drawRect(fixedPageBounds.left - Dips.DP_1, fixedPageBounds.bottom - Dips.DP_25, fixedPageBounds.right + Dips.DP_1, fixedPageBounds.bottom + Dips.DP_1, rect);
            canvas.drawRect(fixedPageBounds.left - Dips.DP_1, fixedPageBounds.bottom - fixedPageBounds.height() / 4 - Dips.DP_5, fixedPageBounds.right + Dips.DP_1, fixedPageBounds.bottom - fixedPageBounds.height() / 4, rect);
            Log.v("EventDraw","process 777");
        } else if (AppState.get().isShowLineDividing && AppSP.get().readingMode == AppState.READING_MODE_MUSICIAN) {//true && false = false 如果是乐师模式则会进入这
            rect.setColor(ColorUtils.setAlphaComponent(Color.GRAY, 200));
            rect.setStyle(Style.FILL);
            canvas.drawRect(fixedPageBounds.left - Dips.DP_1, fixedPageBounds.bottom - Dips.DP_2, fixedPageBounds.right + Dips.DP_1, fixedPageBounds.bottom + Dips.DP_1, rect);
            Log.v("EventDraw","process 888");
        }
//        Paint paint = new Paint();
//        paint.breakText("Vincent hahhaha",true,50,new float[]{50.0f});
//        canvas.drawPaint(paint);


        // TODO Draw there
        // drawLine(page);
        if (!(BookCSS.get().isTextFormat() || AppSP.get().readingMode == AppState.READING_MODE_MUSICIAN)) {//false
            drawPageLinks(page);
            Log.v("EventDraw","process 999");
        }
        // drawSomething(page);
        // drawHighlights(page);
        drawSelectedText(page);//绘制出选中文字时的界面

        return res;
    }

    @Override
    public boolean process(final PageTree nodes) {
        return process(nodes, level);
    }

    @Override
    public boolean process(final PageTree nodes, final PageTreeLevel level) {
        return nodes.process(this, level, false);
    }

    @Override
    public boolean process(final PageTreeNode node) {
        final RectF nodeRect = node.getTargetRect(pageBounds);

        if (!viewState.isNodeVisible(nodeRect)) {
            return false;
        }

        try {
            if (node.holder.drawBitmap(canvas, viewState.paint, viewState.viewBase, nodeRect, nodeRect)) {
                return true;
            }

            if (node.parent != null) {
                final RectF parentRect = node.parent.getTargetRect(pageBounds);
                if (node.parent.holder.drawBitmap(canvas, viewState.paint, viewState.viewBase, parentRect, nodeRect)) {
                    return true;
                }
            }

            return node.page.nodes.paintChildren(this, node, nodeRect);

        } finally {
        }
    }

    public boolean paintChild(final PageTreeNode node, final PageTreeNode child, final RectF nodeRect) {
        final RectF childRect = child.getTargetRect(pageBounds);
        return child.holder.drawBitmap(canvas, viewState.paint, viewState.viewBase, childRect, nodeRect);
    }

    /**
     * 绘制页面背景
     * @param page
     */
    protected void drawPageBackground(final Page page) {
//        if (canvas == null) {
//            LOG.d("canvas is null");
//        }
//        Log.v("EventDraw","process ddd");
//        fixedPageBounds.set(pageBounds);//设置页面边界
//        fixedPageBounds.offset(-viewState.viewBase.x, -viewState.viewBase.y);
//
//        viewState.paint.fillPaint.setColor(MagicHelper.getBgColor());
//        canvas.drawRect(fixedPageBounds, viewState.paint.fillPaint);
//
//        final TextPaint textPaint = viewState.paint.textPaint;
//        // textPaint.setTextSize(20 * viewState.z);
//        textPaint.setTextSize(Dips.spToPx(16));
//        textPaint.setColor(MagicHelper.getTextColor());
//
//        final String text = LibreraApp.context.getString(R.string.text_page) + " " + (page.index.viewIndex + 1);
//        Log.v("EventDraw","process text： "+text);
//        canvas.drawText(text, fixedPageBounds.centerX(), fixedPageBounds.centerY(), textPaint);

    }


    private void drawPageLinks(final Page page) {

        if (LengthUtils.isEmpty(page.links)) {
            return;
        }

        paintWrods.setColor(AppState.get().isDayNotInvert ? Color.BLUE : Color.YELLOW);
        paintWrods.setAlpha(60);

        for (final PageLink link : page.links) {
            final RectF rect = page.getLinkSourceRect(pageBounds, link);
            if (rect != null) {
                rect.offset(-viewState.viewBase.x, -viewState.viewBase.y);
                // canvas.drawRect(rect, paintWrods);
                canvas.drawLine(rect.left, rect.bottom, rect.right, rect.bottom, paintWrods);
            }
        }
    }

    private void drawSomething(final Page page) {
        final RectF link = new RectF(0.1f, 0.1f, 0.3f, 0.3f);
        final RectF rect = page.getPageRegion(pageBounds, new RectF(link));
        rect.offset(-viewState.viewBase.x, -viewState.viewBase.y);
        final Paint p = new Paint();
        p.setColor(Color.MAGENTA);
        p.setAlpha(40);
        canvas.drawRect(rect, p);
    }

    /**
     * 绘制所选文字
     * 在小说阅读界面上选中文字时会触发这里
     * @param page
     */
    private void drawSelectedText(final Page page) {
        final Paint p = new Paint();
        p.setColor(AppState.get().isDayNotInvert ? Color.BLUE : Color.YELLOW);
        p.setAlpha(60);

        if (page.selectionAnnotion != null) {
            final RectF rect = page.getPageRegion(pageBounds, new RectF(page.selectionAnnotion));
            rect.offset(-viewState.viewBase.x, -viewState.viewBase.y);
            canvas.drawRect(rect, p);
            Log.v("EventDraw","process aaa");
        }

        if (page.selectedText.isEmpty()) {
            Log.v("EventDraw","process bbb");
            return;
        }
        for (RectF selected : page.selectedText) {//当选中文字时会进入这里开始绘制选中部分文字的背景效果
            final RectF rect = page.getPageRegion(pageBounds, new RectF(selected));
            rect.offset(-viewState.viewBase.x, -viewState.viewBase.y);
            canvas.drawRect(rect, p);
            Log.v("EventDraw","process ccc");
        }

    }

}
