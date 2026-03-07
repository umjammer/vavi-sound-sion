package org.si.as3.display;

import org.si.utils.EventDispatcher;
import org.si.as3.system.ApplicationDomain;

public class LoaderInfo extends EventDispatcher {
    public ApplicationDomain applicationDomain = new ApplicationDomain();
    public int bytesLoaded = 0;
    public int bytesTotal = 0;
}
