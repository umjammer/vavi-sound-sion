package org.si.as3.net;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;

import org.si.utils.Event;
import org.si.utils.EventDispatcher;
import org.si.utils.IOErrorEvent;
import org.si.utils.ProgressEvent;


public class URLLoader extends EventDispatcher {

    public String dataFormat = URLLoaderDataFormat.BINARY;
    public byte[] data;
    public int bytesLoaded;
    public int bytesTotal;
    private volatile boolean closed;

    public void load(String url) {
        load(new URLRequest(url));
    }

    public void load(URLRequest request) {
        if (request == null || request.url == null) {
            dispatchEvent(new IOErrorEvent(IOErrorEvent.IO_ERROR, false, false, "URL is null"));
            return;
        }

        closed = false;
        dispatchEvent(new Event(Event.OPEN));

        try {
            byte[] bytes = readAllBytes(request.url);
            if (closed) {
                return;
            }
            data = bytes;
            bytesTotal = bytes.length;
            bytesLoaded = bytes.length;
            dispatchEvent(new ProgressEvent(ProgressEvent.PROGRESS, false, false, bytesLoaded, bytesTotal));
            dispatchEvent(new Event(Event.COMPLETE));
        } catch (IOException e) {
            if (!closed) {
                dispatchEvent(new IOErrorEvent(IOErrorEvent.IO_ERROR, false, false, e.getMessage()));
            }
        }
    }

    public void close() {
        closed = true;
    }

    private byte[] readAllBytes(String location) throws IOException {
        if (location.contains("://")) {
            try (InputStream is = new URL(location).openStream()) {
                return is.readAllBytes();
            }
        }

        Path path;
        try {
            path = Path.of(URI.create(location));
        } catch (IllegalArgumentException e) {
            path = Path.of(location);
        }
        return Files.readAllBytes(path);
    }
}
