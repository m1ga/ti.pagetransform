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
public class ScrollableViewProxy extends ti.modules.titanium.ui.ScrollableViewProxy {
    private ScrollableView scrollableView;

    public TiUIView createView(Activity activity) {
        this.scrollableView = new ScrollableView(this);
        return this.scrollableView;
    }
}
