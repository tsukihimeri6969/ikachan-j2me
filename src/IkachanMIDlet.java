package ikachan;

import javax.microedition.lcdui.Display;
import javax.microedition.midlet.MIDlet;

public class IkachanMIDlet extends MIDlet {
    private IkachanCanvas canvas;

    protected void startApp() {
        if (canvas == null) {
            canvas = new IkachanCanvas(this);
            Display.getDisplay(this).setCurrent(canvas);
            canvas.start();
        } else {
            canvas.resume();
        }
    }

    protected void pauseApp() {
        if (canvas != null) {
            canvas.pause();
        }
    }

    protected void destroyApp(boolean unconditional) {
        if (canvas != null) {
            canvas.stop();
        }
    }
}