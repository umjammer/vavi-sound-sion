package vavi.display;

import org.si.utils.EventDispatcher;
import vavi.system.ApplicationDomain;

public class LoaderInfo extends EventDispatcher {
    public ApplicationDomain applicationDomain = new ApplicationDomain();
    public int bytesLoaded = 0;
    public int bytesTotal = 0;
}
