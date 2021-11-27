package ti.pagetransform;


import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.os.Build;
import android.os.Parcelable;
import android.util.DisplayMetrics;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.view.View.OnClickListener;
import android.widget.FrameLayout;
import android.widget.FrameLayout.LayoutParams;

import androidx.annotation.RequiresApi;
import androidx.viewpager.widget.PagerAdapter;
import androidx.viewpager.widget.ViewPager;
import androidx.viewpager.widget.ViewPager.SimpleOnPageChangeListener;
import java.util.ArrayList;
import java.util.HashMap;
import org.appcelerator.kroll.KrollDict;
import org.appcelerator.kroll.KrollProxy;
import org.appcelerator.kroll.common.Log;
import org.appcelerator.titanium.proxy.TiViewProxy;
import org.appcelerator.titanium.util.TiConvert;
import org.appcelerator.titanium.view.TiCompositeLayout;
import org.appcelerator.titanium.view.TiUIView;
import ti.modules.titanium.ui.widget.TiArrowView;
import ti.modules.titanium.ui.widget.TiUIScrollableView;
import ti.modules.titanium.ui.widget.listview.ListItemProxy;

@SuppressLint({"NewApi"})
public class ScrollableView extends TiUIView {
    private static final String TAG = "TiUIScrollableView";
    private static final int PAGE_LEFT_ID = View.generateViewId();
    private static final int PAGE_RIGHT_ID = View.generateViewId();
    private final ViewPager mPager;
    private final TiUIScrollableView.ViewPagerAdapter mAdapter;
    private final TiViewPagerLayout mContainer;
    private final FrameLayout mPagingControl;
    private int mCurIndex = 0;
    private boolean mEnabled = true;

    public ScrollableView(ti.pagetransform.ScrollableViewProxy proxy) {
        super(proxy);
        this.getLayoutParams().autoFillsWidth = true;
        this.getLayoutParams().autoFillsHeight = true;
        Activity activity = proxy.getActivity();
        this.mContainer = new TiViewPagerLayout(activity);
        this.mAdapter = new TiUIScrollableView.ViewPagerAdapter(activity, proxy.getViewsList());
        this.mPager = this.buildViewPager(activity, this.mAdapter);
        if (proxy.hasPropertyAndNotNull("clipViews")) {
            this.mPager.setClipToPadding(TiConvert.toBoolean(proxy.getProperty("clipViews"), true));
        }

        this.mContainer.addView(this.mPager, new LayoutParams(-1, -1));
        this.mPagingControl = this.buildPagingControl(activity);
        this.mContainer.addView(this.mPagingControl, new LayoutParams(-1, -1));

        mPager.setClipChildren(false);
        mPager.setClipToPadding(false);
        mPager.setPageTransformer(true, new DepthPageTransformer());

        this.setNativeView(this.mContainer);
    }

    public class DepthPageTransformer implements ViewPager.PageTransformer {
        private static final float MIN_SCALE = 0.75f;

        @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
        public void transformPage(View view, float position) {
            int pageWidth = view.getWidth();
            int pageHeight = view.getHeight();

            if (position < 0) {
                // old pages
                view.setAlpha(1f + position * 0.2f);
                view.setTranslationX(pageWidth * -position);
                view.setTranslationY(25f * position);
                view.setScaleX(0.9f + position * 0.01f);
                view.setScaleY(0.9f + position * 0.01f);
                view.setTranslationZ(0.9f * position);

            } else if (position == 0) {
                // visible page
                view.setTranslationX(pageWidth * -position);
                view.setTranslationY(20f * position);
                view.setScaleX(0.9f);
                view.setScaleY(0.9f);
                view.setTranslationZ(0.9f * position);

            } else if (position <= 1) {
                // next page
                view.setScaleX(0.9f + (0.02f * position));
                view.setScaleY(0.9f + (0.02f * position));
                view.setTranslationX(pageWidth * -position);
                view.setTranslationY(pageHeight * position - 150f * position );
                view.setTranslationZ(2f);
            } else {
                // all other pages
                view.setScaleX(0.92f);
                view.setScaleY(0.92f);
                view.setTranslationX(pageWidth * -position);
                view.setTranslationZ(2f);

                // keep at stack level
                view.setTranslationY(pageHeight * 1f - 150f * 1f );

            }
        }
    }

    private ViewPager buildViewPager(Context context, ti.modules.titanium.ui.widget.TiUIScrollableView.ViewPagerAdapter adapter) {
        ViewPager pager = new ViewPager(context) {

            private MotionEvent swapXY(MotionEvent ev) {

                //Get display dimensions
                float displayWidth=this.getWidth();
                float displayHeight=this.getHeight();

                //Get current touch position
                float posX=ev.getX();
                float posY=ev.getY();

                //Transform (X,Y) into (Y,X) taking display dimensions into account
                float newPosX=(posY/displayHeight)*displayWidth;
                float newPosY=(1-posX/displayWidth)*displayHeight;

                //swap the x and y coords of the touch event
                ev.setLocation(newPosX, newPosY);

                return ev;
            }

            public boolean onTouchEvent(MotionEvent ev) {
                return super.onTouchEvent(swapXY(ev));
            }

            public boolean onInterceptTouchEvent(MotionEvent ev) {
                boolean intercepted = super.onInterceptTouchEvent(swapXY(ev));
                swapXY(ev); // return touch coordinates to original reference frame for any child views
                return intercepted;
            }

            protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
                int widthMode = MeasureSpec.getMode(widthMeasureSpec);
                int heightMode = MeasureSpec.getMode(heightMeasureSpec);
                if (widthMode != 1073741824 || heightMode != 1073741824) {
                    int maxWidth = 0;
                    int maxHeight = 0;

                    int containerHeight;
                    for(containerHeight = this.getChildCount() - 1; containerHeight >= 0; --containerHeight) {
                        View child = this.getChildAt(containerHeight);
                        if (child != null && child.getVisibility() != 8) {
                            child.measure(widthMeasureSpec, heightMeasureSpec);
                            int childWidth = child.getMeasuredWidth();
                            childWidth += child.getPaddingLeft() + child.getPaddingRight();
                            int childHeight = child.getMeasuredHeight();
                            childHeight += child.getPaddingTop() + child.getPaddingBottom();
                            maxWidth = Math.max(maxWidth, childWidth);
                            maxHeight = Math.max(maxHeight, childHeight);
                        }
                    }

                    maxWidth = Math.max(maxWidth, this.getSuggestedMinimumWidth());
                    maxHeight = Math.max(maxHeight, this.getSuggestedMinimumHeight());
                    if (widthMode != 1073741824) {
                        containerHeight = MeasureSpec.getSize(widthMeasureSpec);
                        if (widthMode == 0 || maxWidth < containerHeight) {
                            widthMode = -2147483648;
                            widthMeasureSpec = MeasureSpec.makeMeasureSpec(maxWidth, widthMode);
                        }
                    }

                    if (heightMode != 1073741824) {
                        containerHeight = MeasureSpec.getSize(heightMeasureSpec);
                        if (heightMode == 0 || maxHeight < containerHeight) {
                            heightMode = -2147483648;
                            heightMeasureSpec = MeasureSpec.makeMeasureSpec(maxHeight, heightMode);
                        }
                    }
                }

                super.onMeasure(widthMeasureSpec, heightMeasureSpec);
            }
        };
        pager.setAdapter(adapter);
        pager.addOnPageChangeListener(new SimpleOnPageChangeListener() {
            private int lastSelectedPageIndex;
            private boolean isScrolling;
            private boolean isDragging;

            public void onPageScrollStateChanged(int scrollState) {
                switch(scrollState) {
                    case 0:
                        if (this.isScrolling || this.isDragging) {
                            ScrollableView.this.mCurIndex = this.lastSelectedPageIndex;
                            TiViewProxy pageProxy = null;
                            if (this.lastSelectedPageIndex >= 0 && this.lastSelectedPageIndex < ScrollableView.this.getViews().size()) {
                                pageProxy = (TiViewProxy) ScrollableView.this.getViews().get(this.lastSelectedPageIndex);
                            }

                            if (this.isDragging) {
                                this.isDragging = false;
                                if (ScrollableView.this.proxy != null) {
                                    ((ScrollableViewProxy) ScrollableView.this.proxy).fireDragEnd(this.lastSelectedPageIndex, pageProxy);
                                }

                                ScrollableView.this.mPager.requestDisallowInterceptTouchEvent(false);
                            }

                            if (this.isScrolling) {
                                this.isScrolling = false;
                                if (ScrollableView.this.proxy != null) {
                                    ((ScrollableViewProxy) ScrollableView.this.proxy).fireScrollEnd(this.lastSelectedPageIndex, pageProxy);
                                }

                                ScrollableView.this.proxy.setProperty("currentPage", ScrollableView.this.mCurIndex);
                            }
                        }

                        if (ScrollableView.this.shouldShowPager()) {
                            ScrollableView.this.showPager();
                        }
                        break;
                    case 1:
                        if (!this.isDragging && !ScrollableView.this.getViews().isEmpty()) {
                            this.isDragging = true;
                            this.isScrolling = true;
                            if (ScrollableView.this.proxy != null) {
                                ScrollableView.this.proxy.fireEvent("dragstart", new KrollDict());
                            }

                            ScrollableView.this.mPager.requestDisallowInterceptTouchEvent(true);
                        }
                }

            }

            public void onPageSelected(int pageIndex) {
                this.lastSelectedPageIndex = pageIndex;
            }

            public void onPageScrolled(int pageIndex, float pageOffsetNormalized, int pageOffsetPixels) {
                if (!ScrollableView.this.getViews().isEmpty()) {
                    if (!this.isScrolling && Math.abs(pageOffsetNormalized) >= 0.01F) {
                        this.isScrolling = true;
                    }

                    if (this.isScrolling) {
                        float currentPageAsFloat = (float)pageIndex + pageOffsetNormalized;
                        int currentPageIndex = (int)Math.floor((double)(currentPageAsFloat + 0.5F));
                        if (currentPageIndex < 0) {
                            currentPageIndex = 0;
                        } else if (currentPageIndex >= ScrollableView.this.getViews().size()) {
                            currentPageIndex = ScrollableView.this.getViews().size() - 1;
                        }

                        ScrollableView.this.mCurIndex = currentPageIndex;
                        if (ScrollableView.this.proxy != null) {
                            ((ScrollableViewProxy) ScrollableView.this.proxy).fireScroll(ScrollableView.this.mCurIndex, currentPageAsFloat, (TiViewProxy) ScrollableView.this.getViews().get(ScrollableView.this.mCurIndex));
                        }

                    }
                }
            }
        });
        return pager;
    }

    private boolean shouldShowPager() {
        Object showPagingControl = this.proxy.getProperty("showPagingControl");
        return showPagingControl != null ? TiConvert.toBoolean(showPagingControl) : false;
    }

    private FrameLayout buildPagingControl(Context context) {
        if (context == null) {
            return null;
        } else {
            int arrowSizeInPixels = 24;
            if (context.getResources() != null) {
                DisplayMetrics metrics = context.getResources().getDisplayMetrics();
                if (metrics != null && metrics.density >= 0.5F) {
                    arrowSizeInPixels = (int)((float)arrowSizeInPixels * metrics.density);
                }
            }

            FrameLayout layout = new FrameLayout(context);
            layout.setFocusable(false);
            layout.setFocusableInTouchMode(false);
            TiArrowView leftArrow = new TiArrowView(context);
            leftArrow.setVisibility(4);
            leftArrow.setId(PAGE_LEFT_ID);
            leftArrow.setMinimumWidth(arrowSizeInPixels);
            leftArrow.setMinimumHeight(arrowSizeInPixels);
            leftArrow.setOnClickListener(new OnClickListener() {
                public void onClick(View v) {
                    if (ScrollableView.this.mEnabled) {
                        ScrollableView.this.movePrevious();
                    }

                }
            });
            LayoutParams leftLayoutParams = new LayoutParams(-2, -2);
            leftLayoutParams.gravity = 19;
            layout.addView(leftArrow, leftLayoutParams);
            TiArrowView rightArrow = new TiArrowView(context);
            rightArrow.setLeft(false);
            rightArrow.setVisibility(4);
            rightArrow.setId(PAGE_RIGHT_ID);
            rightArrow.setMinimumWidth(arrowSizeInPixels);
            rightArrow.setMinimumHeight(arrowSizeInPixels);
            rightArrow.setOnClickListener(new OnClickListener() {
                public void onClick(View v) {
                    if (ScrollableView.this.mEnabled) {
                        ScrollableView.this.moveNext();
                    }

                }
            });
            LayoutParams rightLayoutParams = new LayoutParams(-2, -2);
            rightLayoutParams.gravity = 21;
            layout.addView(rightArrow, rightLayoutParams);
            layout.setVisibility(8);
            return layout;
        }
    }

    public TiUIScrollableView.ViewPagerAdapter getAdapter() {
        return this.mAdapter;
    }

    private ScrollableViewProxy getScrollableViewProxy() {
        return (ScrollableViewProxy)this.proxy;
    }

    private ArrayList<TiViewProxy> getViews() {
        return this.getScrollableViewProxy().getViewsList();
    }

    public void processProperties(KrollDict d) {
        if (d.containsKey("currentPage")) {
            int page = TiConvert.toInt(d, "currentPage");
            if (page > 0) {
                this.setCurrentPage(page);
            }
        }

        if (d.containsKey("showPagingControl") && TiConvert.toBoolean(d, "showPagingControl")) {
            this.showPager();
        }

        if (d.containsKey("scrollingEnabled")) {
            this.mEnabled = TiConvert.toBoolean(d, "scrollingEnabled");
        }

        if (d.containsKey("overScrollMode")) {
            this.mPager.setOverScrollMode(TiConvert.toInt(d.get("overScrollMode"), 0));
        }

        if (d.containsKey("cacheSize")) {
            this.setPageCacheSize(TiConvert.toInt(d.get("cacheSize")));
        }

        if (d.containsKey("padding")) {
            this.setPadding((HashMap)d.get("padding"));
        }

        super.processProperties(d);
    }

    public void propertyChanged(String key, Object oldValue, Object newValue, KrollProxy proxy) {
        if ("currentPage".equals(key)) {
            this.setCurrentPage(TiConvert.toInt(newValue));
        } else if ("showPagingControl".equals(key)) {
            boolean show = TiConvert.toBoolean(newValue);
            if (show) {
                this.showPager();
            } else {
                this.hidePager();
            }
        } else if ("padding".equals(key)) {
            this.setPadding((HashMap)newValue);
        } else if ("scrollingEnabled".equals(key)) {
            this.mEnabled = TiConvert.toBoolean(newValue);
        } else if ("overScrollMode".equals(key)) {
            this.mPager.setOverScrollMode(TiConvert.toInt(newValue, 0));
        } else if ("cacheSize".equals(key)) {
            this.setPageCacheSize(TiConvert.toInt(newValue));
        } else {
            super.propertyChanged(key, oldValue, newValue, proxy);
        }

    }

    private void setPageCacheSize(int value) {
        if (value < 3) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("ScrollableView 'cacheSize' cannot be set less than ");
            stringBuilder.append(3);
            stringBuilder.append(". Given value: ");
            stringBuilder.append(value);
            Log.w("TiUIScrollableView", stringBuilder.toString());
            value = 3;
        }

        --value;
        this.mPager.setOffscreenPageLimit(value);
    }

    public void showPager() {
        View v = null;
        v = this.mContainer.findViewById(PAGE_LEFT_ID);
        if (v != null) {
            v.setVisibility(this.mCurIndex > 0 ? 0 : 4);
        }

        v = this.mContainer.findViewById(PAGE_RIGHT_ID);
        if (v != null) {
            v.setVisibility(this.mCurIndex < this.getViews().size() - 1 ? 0 : 4);
        }

        this.mPagingControl.setVisibility(0);
        ((ScrollableViewProxy)this.proxy).setPagerTimeout();
    }

    public void hidePager() {
        this.mPagingControl.setVisibility(4);
    }

    public void moveNext() {
        this.move(this.mCurIndex + 1, true);
    }

    public void movePrevious() {
        this.move(this.mCurIndex - 1, true);
    }

    private void move(int index, boolean smoothScroll) {
        if (index >= 0 && index < this.getViews().size()) {
            this.mCurIndex = index;
            this.mPager.setCurrentItem(index, smoothScroll);
        } else {
            if (Log.isDebugModeEnabled()) {
                Log.w("TiUIScrollableView", "Request to move to index " + index + " ignored, as it is out-of-bounds.", "DEBUG_MODE");
            }

        }
    }

    public void scrollTo(Object view) {
        if (view instanceof Number) {
            this.move(((Number)view).intValue(), true);
        } else if (view instanceof TiViewProxy) {
            this.move(this.getViews().indexOf(view), true);
        }

    }

    public int getCurrentPage() {
        return this.mCurIndex;
    }

    public void setCurrentPage(Object view) {
        if (view instanceof Number) {
            this.move(((Number)view).intValue(), false);
        } else if (Log.isDebugModeEnabled()) {
            Log.w("TiUIScrollableView", "Request to set current page is ignored, as it is not a number.", "DEBUG_MODE");
        }

    }

    public void setEnabled(Object value) {
        this.mEnabled = TiConvert.toBoolean(value);
    }

    public boolean getEnabled() {
        return this.mEnabled;
    }

    private void setPadding(HashMap<String, Object> d) {
        int paddingLeft = this.mPager.getPaddingLeft();
        int paddingRight = this.mPager.getPaddingRight();
        int paddingTop = this.mPager.getPaddingTop();
        int paddingBottom = this.mPager.getPaddingBottom();
        if (d.containsKey("left")) {
            paddingLeft = TiConvert.toInt(d.get("left"), 0);
        }

        if (d.containsKey("right")) {
            paddingRight = TiConvert.toInt(d.get("right"), 0);
        }

        if (d.containsKey("top")) {
            paddingTop = TiConvert.toInt(d.get("top"), 0);
        }

        if (d.containsKey("bottom")) {
            paddingBottom = TiConvert.toInt(d.get("bottom"), 0);
        }

        this.mPager.setPadding(paddingLeft, paddingTop, paddingRight, paddingBottom);
    }

    public void release() {
        if (this.mPager != null) {
            this.mPager.removeAllViews();
        }

        super.release();
    }

    private class TiViewPagerLayout extends FrameLayout {
        public TiViewPagerLayout(Context context) {
            super(context);
            boolean focusable = true;
            if (this.isListViewParent(ScrollableView.this.proxy)) {
                focusable = false;
            }

            this.setFocusable(focusable);
            this.setFocusableInTouchMode(focusable);
            this.setDescendantFocusability(262144);
        }

        private boolean isListViewParent(TiViewProxy proxy) {
            if (proxy == null) {
                return false;
            } else if (proxy instanceof ListItemProxy) {
                return true;
            } else {
                TiViewProxy parent = proxy.getParent();
                return parent != null ? this.isListViewParent(parent) : false;
            }
        }

        public boolean onTrackballEvent(MotionEvent event) {
            if (ScrollableView.this.shouldShowPager() && ScrollableView.this.mPagingControl.getVisibility() != 0) {
                ScrollableView.this.showPager();
            }

            return super.onTrackballEvent(event);
        }

        public boolean dispatchKeyEvent(KeyEvent event) {
            boolean handled = false;
            if (event.getAction() == 0) {
                switch(event.getKeyCode()) {
                    case 21:
                        ScrollableView.this.movePrevious();
                        handled = true;
                        break;
                    case 22:
                        ScrollableView.this.moveNext();
                        handled = true;
                }
            }

            return handled || super.dispatchKeyEvent(event);
        }
    }

    public static class ViewPagerAdapter extends PagerAdapter {
        private final ArrayList<TiViewProxy> mViewProxies;

        public ViewPagerAdapter(Activity activity, ArrayList<TiViewProxy> viewProxies) {
            if (viewProxies == null) {
                throw new IllegalArgumentException();
            } else {
                this.mViewProxies = viewProxies;
            }
        }

        public void destroyItem(View container, int position, Object object) {
            if (container instanceof ViewPager) {
                if (object instanceof View) {
                    ViewParent parentView = ((View)object).getParent();
                    if (parentView instanceof ViewGroup) {
                        if (!(parentView instanceof ViewPager)) {
                            ((ViewPager)container).removeView((View)parentView);
                        }

                        ((ViewGroup)parentView).removeView((View)object);
                    }
                }

                if (position >= 0 && position < this.mViewProxies.size()) {
                    TiViewProxy proxy = (TiViewProxy)this.mViewProxies.get(position);
                    if (proxy != null) {
                        proxy.releaseViews();
                    }
                }

            }
        }

        public void finishUpdate(View container) {
        }

        public int getCount() {
            return this.mViewProxies.size();
        }

        public Object instantiateItem(View container, int position) {
            if (!(container instanceof ViewPager)) {
                return null;
            } else if (position >= 0 && position < this.mViewProxies.size()) {
                View pageView = null;
                android.view.ViewGroup.LayoutParams layoutParams = null;
                TiViewProxy proxy = (TiViewProxy)this.mViewProxies.get(position);
                if (proxy != null) {
                    TiUIView uiView = proxy.getOrCreateView();
                    if (uiView != null) {
                        pageView = uiView.getOuterView();
                        layoutParams = uiView.getLayoutParams();
                    }
                }

                if (pageView == null) {
                    return null;
                } else {
                    TiCompositeLayout pageLayout = new TiCompositeLayout(container.getContext());
                    ViewPager pager = (ViewPager)container;
                    ViewParent parentView = pageView.getParent();
                    if (parentView instanceof ViewGroup) {
                        pager.removeView((View)parentView);
                        ((ViewGroup)parentView).removeView(pageView);
                    }

                    pageLayout.addView(pageView, layoutParams);
                    android.view.ViewGroup.LayoutParams layoutParams2 = new android.view.ViewGroup.LayoutParams(-1, -1);
                    if (position < pager.getChildCount()) {
                        pager.addView(pageLayout, position, layoutParams2);
                    } else {
                        pager.addView(pageLayout, layoutParams2);
                    }

                    return pageView;
                }
            } else {
                return null;
            }
        }

        public boolean isViewFromObject(View view, Object obj) {
            if (view == null) {
                return obj == null;
            } else if (obj == null) {
                return false;
            } else if (view instanceof ViewGroup && ((ViewGroup)view).getChildCount() > 0) {
                return obj == ((ViewGroup)view).getChildAt(0);
            } else {
                return false;
            }
        }

        public void restoreState(Parcelable state, ClassLoader loader) {
        }

        public Parcelable saveState() {
            return null;
        }

        public void startUpdate(View container) {
        }

        public int getItemPosition(Object object) {
            return this.mViewProxies.contains(object) ? -1 : -2;
        }
    }
}
