package org.ebookdroid.core.events;

/**
 * 缩放监听器
 * 所有阅读界面都默认继承缩放功能
 */
public interface ZoomListener {

    void zoomChanged(float oldZoom, float newZoom, boolean committed);
}
