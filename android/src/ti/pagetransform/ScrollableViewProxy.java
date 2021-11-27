package ti.pagetransform;
import android.app.Activity;
import android.os.Message;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import org.appcelerator.kroll.KrollDict;
import org.appcelerator.kroll.annotations.Kroll;
import org.appcelerator.titanium.proxy.TiViewProxy;
import org.appcelerator.titanium.util.TiConvert;
import org.appcelerator.titanium.view.TiUIView;
import ti.modules.titanium.ui.widget.TiUIScrollableView;

@Kroll.proxy(creatableInModule = TiPagetransformModule.class)
public class ScrollableViewProxy extends TiViewProxy {
    private static final String TAG = "TiScrollableView";
    private static final int MSG_FIRST_ID = 1212;
    public static final int MSG_HIDE_PAGER = 1313;
    public static final int MSG_MOVE_PREV = 1314;
    public static final int MSG_MOVE_NEXT = 1315;
    public static final int MSG_SCROLL_TO = 1316;
    public static final int MSG_SET_CURRENT = 1319;
    public static final int MSG_SET_ENABLED = 1321;
    public static final int MSG_LAST_ID = 2211;
    private static final int DEFAULT_PAGING_CONTROL_TIMEOUT = 3000;
    public static final int MIN_CACHE_SIZE = 3;
    protected AtomicBoolean inScroll = new AtomicBoolean(false);
    private List<TiViewProxy> views = new ArrayList();
    private ScrollableView scrollableView;

    public ScrollableViewProxy() {
        this.defaultValues.put("cacheSize", 3);
        this.defaultValues.put("clipViews", true);
        this.defaultValues.put("showPagingControl", false);
        this.defaultValues.put("overScrollMode", 0);
    }

    public void handleCreationDict(KrollDict properties) {
        super.handleCreationDict(properties);
        if (properties.containsKey("views")) {
            this.setViews(properties.get("views"));
        }

    }

    public TiUIView createView(Activity activity) {
        this.scrollableView = new ScrollableView(this);
        return this.scrollableView;
    }

    public boolean handleMessage(Message msg) {
        boolean handled = false;
        switch(msg.what) {
            case 1313:
                if (this.scrollableView != null) {
                    this.scrollableView.hidePager();
                    handled = true;
                }
                break;
            case 1314:
                if (this.scrollableView != null) {
                    this.inScroll.set(true);
                    this.scrollableView.movePrevious();
                    this.inScroll.set(false);
                    handled = true;
                }
                break;
            case 1315:
                if (this.scrollableView != null) {
                    this.inScroll.set(true);
                    this.scrollableView.moveNext();
                    this.inScroll.set(false);
                    handled = true;
                }
                break;
            case 1316:
                if (this.scrollableView != null) {
                    this.inScroll.set(true);
                    this.scrollableView.scrollTo(msg.obj);
                    this.inScroll.set(false);
                    handled = true;
                }
                break;
            default:
                handled = super.handleMessage(msg);
        }

        return handled;
    }

    public void removeAllViews() {
        Iterator var1 = this.views.iterator();

        while(var1.hasNext()) {
            TiViewProxy view = (TiViewProxy)var1.next();
            view.releaseViews();
            view.setParent((TiViewProxy)null);
        }

        this.views.clear();
        if (this.scrollableView != null) {
            this.scrollableView.getAdapter().notifyDataSetChanged();
        }

    }

    public ArrayList<TiViewProxy> getViewsList() {
        return (ArrayList)this.views;
    }

    public TiViewProxy[] getViews() {
        return (TiViewProxy[])this.views.toArray(new TiViewProxy[0]);
    }

    public void setViews(Object views) {
        ArrayList<TiViewProxy> oldViewList = new ArrayList(this.views);
        this.views.clear();
        if (views instanceof Object[]) {
            Object[] var3 = (Object[])views;
            int var4 = var3.length;

            for(int var5 = 0; var5 < var4; ++var5) {
                Object nextObject = var3[var5];
                if (nextObject instanceof TiViewProxy) {
                    TiViewProxy view = (TiViewProxy)nextObject;
                    if (!this.views.contains(view)) {
                        view.setActivity(this.getActivity());
                        view.setParent(this);
                        this.views.add(view);
                    }
                }
            }
        }

        Iterator var8 = oldViewList.iterator();

        while(var8.hasNext()) {
            TiViewProxy oldView = (TiViewProxy)var8.next();
            if (!this.views.contains(oldView)) {
                oldView.releaseViews();
                oldView.setParent((TiViewProxy)null);
            }
        }

        if (this.scrollableView != null) {
            this.scrollableView.getAdapter().notifyDataSetChanged();
        }

    }

    public void addView(TiViewProxy view) {
        if (view != null) {
            if (!this.views.contains(view)) {
                view.setActivity(this.getActivity());
                view.setParent(this);
                this.views.add(view);
                if (this.scrollableView != null) {
                    this.scrollableView.getAdapter().notifyDataSetChanged();
                }

            }
        }
    }

    public void insertViewsAt(int insertIndex, Object viewObject) {
        if (viewObject instanceof TiViewProxy) {
            TiViewProxy view = (TiViewProxy)viewObject;
            if (!this.views.contains(view)) {
                view.setActivity(this.getActivity());
                view.setParent(this);
                this.views.add(insertIndex, view);
            }
        } else {
            if (!(viewObject instanceof Object[])) {
                return;
            }

            Object[] views = (Object[])viewObject;

            for(int i = 0; i < views.length; ++i) {
                this.insertViewsAt(insertIndex, views[i]);
            }
        }

        if (this.scrollableView != null) {
            this.scrollableView.getAdapter().notifyDataSetChanged();
        }

    }

    public void removeView(Object viewObject) {
        if (viewObject instanceof Number) {
            this.views.remove((Integer)viewObject);
        } else {
            if (!(viewObject instanceof TiViewProxy)) {
                return;
            }

            TiViewProxy view = (TiViewProxy)viewObject;
            this.views.remove(view);
        }

        if (this.scrollableView != null) {
            int currentPage = this.scrollableView.getCurrentPage();
            this.scrollableView.getAdapter().notifyDataSetChanged();
            if (currentPage >= this.views.size()) {
                this.scrollableView.setCurrentPage(this.views.size() - 1);
            }
        }

    }

    public void scrollToView(Object view) {
        if (!this.inScroll.get()) {
            this.getMainHandler().obtainMessage(1316, view).sendToTarget();
        }
    }

    public void movePrevious() {
        if (!this.inScroll.get()) {
            this.getMainHandler().removeMessages(1314);
            this.getMainHandler().sendEmptyMessage(1314);
        }
    }

    public void moveNext() {
        if (!this.inScroll.get()) {
            this.getMainHandler().removeMessages(1315);
            this.getMainHandler().sendEmptyMessage(1315);
        }
    }

    public void setPagerTimeout() {
        this.getMainHandler().removeMessages(1313);
        int timeout = 3000;
        Object o = this.getProperty("pagingControlTimeout");
        if (o != null) {
            timeout = TiConvert.toInt(o);
        }

        if (timeout > 0) {
            this.getMainHandler().sendEmptyMessageDelayed(1313, (long)timeout);
        }

    }

    public void fireDragEnd(int currentPage, TiViewProxy currentView) {
        KrollDict options;
        if (this.hasListeners("dragend")) {
            options = new KrollDict();
            options.put("view", currentView);
            options.put("currentPage", currentPage);
            this.fireEvent("dragend", options);
        }

        if (this.hasListeners("dragEnd")) {
            options = new KrollDict();
            options.put("view", currentView);
            options.put("currentPage", currentPage);
            this.fireEvent("dragEnd", options);
        }

    }

    public void fireScrollEnd(int currentPage, TiViewProxy currentView) {
        KrollDict options;
        if (this.hasListeners("scrollend")) {
            options = new KrollDict();
            options.put("view", currentView);
            options.put("currentPage", currentPage);
            this.fireEvent("scrollend", options);
        }

        if (this.hasListeners("scrollEnd")) {
            options = new KrollDict();
            options.put("view", currentView);
            options.put("currentPage", currentPage);
            this.fireEvent("scrollEnd", options);
        }

    }

    public void fireScroll(int currentPage, float currentPageAsFloat, TiViewProxy currentView) {
        if (this.hasListeners("scroll")) {
            KrollDict options = new KrollDict();
            options.put("view", currentView);
            options.put("currentPage", currentPage);
            options.put("currentPageAsFloat", currentPageAsFloat);
            this.fireEvent("scroll", options);
        }

    }

    public boolean getScrollingEnabled() {
        return this.scrollableView != null ? this.scrollableView.getEnabled() : this.getProperties().optBoolean("scrollingEnabled", true);
    }

    public int getCurrentPage() {
        return this.scrollableView != null ? this.scrollableView.getCurrentPage() : this.getProperties().optInt("currentPage", 0);
    }

    public void releaseViews() {
        this.getMainHandler().removeMessages(1313);
        Iterator var1 = this.views.iterator();

        while(var1.hasNext()) {
            TiViewProxy view = (TiViewProxy)var1.next();
            view.releaseViews();
        }

        this.properties.remove("views");
        this.scrollableView = null;
        super.releaseViews();
    }

    public void setActivity(Activity activity) {
        super.setActivity(activity);
        Iterator var2 = this.views.iterator();

        while(var2.hasNext()) {
            TiViewProxy view = (TiViewProxy)var2.next();
            view.setActivity(activity);
        }

    }

    public String getApiName() {
        return "Ti.UI.ScrollableView";
    }
}
