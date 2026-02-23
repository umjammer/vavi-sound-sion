package vavi.display;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;

import javax.imageio.ImageIO;

import org.si.utils.ByteArray;
import org.si.utils.Event;
import org.si.utils.IOErrorEvent;
import org.si.utils.ProgressEvent;
import vavi.media.Sound;
import vavi.net.URLRequest;
import vavi.system.LoaderContext;


public class Loader {

    public LoaderInfo contentLoaderInfo = new LoaderInfo();
    public Object content = null;

    public void load(URLRequest request, LoaderContext context) {
        if (request == null || request.url == null) {
            contentLoaderInfo.dispatchEvent(new IOErrorEvent(IOErrorEvent.IO_ERROR, false, false, "URL is null"));
            return;
        }

        try {
            byte[] bytes = readAllBytes(request.url);
            processBytes(bytes);
        } catch (IOException e) {
            contentLoaderInfo.dispatchEvent(new IOErrorEvent(IOErrorEvent.IO_ERROR, false, false, e.getMessage()));
        }
    }

    public void loadBytes(ByteArray bytes) {
        if (bytes == null) {
            contentLoaderInfo.dispatchEvent(new IOErrorEvent(IOErrorEvent.IO_ERROR, false, false, "bytes is null"));
            return;
        }
        byte[] data = new byte[bytes.length];
        System.arraycopy(bytes.buffer, 0, data, 0, bytes.length);
        processBytes(data);
    }

    private void processBytes(byte[] bytes) {
        contentLoaderInfo.bytesTotal = bytes.length;
        contentLoaderInfo.bytesLoaded = bytes.length;
        contentLoaderInfo.dispatchEvent(new ProgressEvent(ProgressEvent.PROGRESS, false, false, bytes.length, bytes.length));

        BufferedImage image = decodeImage(bytes);
        if (image != null) {
            BitmapData bitmapData = new BitmapData(image.getWidth(), image.getHeight(), true, 0);
            for (int y = 0; y < image.getHeight(); y++) {
                for (int x = 0; x < image.getWidth(); x++) {
                    bitmapData.setPixel32(x, y, image.getRGB(x, y));
                }
            }
            content = new Bitmap(bitmapData);
        } else {
            content = bytes;
            // Flash SWF workflow resolves SoundClass from applicationDomain.
            contentLoaderInfo.applicationDomain.define("SoundClass", Sound.class);
        }

        contentLoaderInfo.dispatchEvent(new Event(Event.COMPLETE));
    }

    private BufferedImage decodeImage(byte[] bytes) {
        try {
            return ImageIO.read(new ByteArrayInputStream(bytes));
        } catch (IOException e) {
            return null;
        }
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
