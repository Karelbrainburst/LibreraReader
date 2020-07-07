package org.ebookdroid.common.settings.types;

import com.foobnix.model.AppBook;

import org.ebookdroid.core.HScrollController;
import org.ebookdroid.core.VScrollController;
import org.ebookdroid.ui.viewer.IActivityController;
import org.ebookdroid.ui.viewer.IViewController;

/**
 * 小说文档视图模式
 */
public enum DocumentViewMode {

    VERTICALL_SCROLL(PageAlign.WIDTH, VScrollController.class),//竖向滚动：滚动模式

    HORIZONTAL_SCROLL(PageAlign.HEIGHT, HScrollController.class);//横向滚动：书籍模式

    private final PageAlign pageAlign;


    private DocumentViewMode(final PageAlign pageAlign, final Class<? extends IViewController> clazz) {
        this.pageAlign = pageAlign;
    }

    public IViewController create(final IActivityController base) {
        //return new HScrollController(base);
        //TODO switch there
        return new VScrollController(base);
    }

    public static PageAlign getPageAlign(final AppBook bs) {
        return PageAlign.AUTO;
    }

    
}
