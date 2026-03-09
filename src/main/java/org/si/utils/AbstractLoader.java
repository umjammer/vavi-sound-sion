//
// Loader basic class
//  Copyright (c) 2008 keim All rights reserved.
//  Distributed under BSD-style license (see org.si.license.txt).
//

package org.si.utils;

import java.util.ArrayList;
import java.util.List;

import org.si.as3.net.URLLoader;
import org.si.as3.net.URLRequest;


/** Loader basic class. */
public class AbstractLoader extends EventDispatcher {

    // variables
    //

    /** loader */
    protected URLLoader _loader;
    /** total bytes */
    protected int _bytesTotal;
    /** loaded bytes */
    protected int _bytesLoaded;
    /** flag complete loading */
    protected boolean _isLoadCompleted;
    /** child loaders */
    protected List<AbstractLoader> _childLoaders;
    /** event priority */
    protected int _eventPriority;

    // constructor
    //

    /** Constructor */
    public AbstractLoader(int priority) /* = 0 */ {
        _loader = new URLLoader();
        _bytesTotal = 0;
        _bytesLoaded = 0;
        _isLoadCompleted = false;
        _childLoaders = new ArrayList<>();
        _eventPriority = priority;
    }

    // operation
    //

    /** load */
    public void load(URLRequest url) {
        _loader.close();
        _bytesTotal = 0;
        _bytesLoaded = 0;
        _isLoadCompleted = false;
        _addAllListeners();
        _loader.load(url);
    }

    /** add child loader */
    public void addChild(AbstractLoader child) {
        _childLoaders.add(child);
        child.addEventListener(Event.COMPLETE, this::_onChildComplete);
    }

    // virtual function
    //

    /** overriding function when completes loading */
    protected void onComplete() {
    }

    // default handler
    //
    private void _onProgress(Event e) {
        if (e instanceof ProgressEvent pe) {
            _bytesTotal = pe.bytesTotal;
            _bytesLoaded = pe.bytesLoaded;
            _isLoadCompleted = false;
            dispatchEvent(new ProgressEvent(ProgressEvent.PROGRESS, false, false, _bytesLoaded, _bytesTotal));
        }
    }

    private void _onComplete(Event e) {
        _removeAllListeners();
        _bytesLoaded = _bytesTotal;
        _isLoadCompleted = true;
        onComplete();
        if (_childLoaders.isEmpty()) {
            dispatchEvent(new Event(Event.COMPLETE));
        }
    }

    private void _onError(Event e) {
        if (e instanceof ErrorEvent) {
            _removeAllListeners();
            dispatchEvent(new ErrorEvent(ErrorEvent.ERROR, false, false, e.toString()));
        }
    }

    private void _onChildComplete(Event e) {
        int index = _childLoaders.indexOf((AbstractLoader) e.target);
        if (index == -1) throw new Error("AbstractLoader; unknown error, children mismatched.");
        _childLoaders.subList(index, index + 1).clear();
        if (_childLoaders.isEmpty() && _isLoadCompleted) {
            dispatchEvent(new Event(Event.COMPLETE));
        }
    }

    private void _addAllListeners() {
        _loader.addEventListener(Event.COMPLETE, this::_onComplete, false, _eventPriority);
        _loader.addEventListener(ProgressEvent.PROGRESS, this::_onProgress, false, _eventPriority);
        _loader.addEventListener(IOErrorEvent.IO_ERROR, this::_onError, false, _eventPriority);
        _loader.addEventListener(SecurityErrorEvent.SECURITY_ERROR, this::_onError, false, _eventPriority);
    }

    private void _removeAllListeners() {
        _loader.removeEventListener(Event.COMPLETE, this::_onComplete);
        _loader.removeEventListener(ProgressEvent.PROGRESS, this::_onProgress);
        _loader.removeEventListener(IOErrorEvent.IO_ERROR, this::_onError);
        _loader.removeEventListener(SecurityErrorEvent.SECURITY_ERROR, this::_onError);
    }
}
