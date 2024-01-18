// IControlService.aidl
package xyz.hyli.connect;

// Declare any non-default types here with import statements
import xyz.hyli.connect.bean.MotionEventBean;
import android.view.MotionEvent;

interface IControlService {
    boolean init();
    void pressBack(int displayId);
    void touch(in MotionEventBean motionEventBean);
    boolean moveStack(int displayId);
    boolean execShell(String command, boolean useRoot);
}