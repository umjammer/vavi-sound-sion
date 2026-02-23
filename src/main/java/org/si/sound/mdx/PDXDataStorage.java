//
// PDX data storage
//  Copyright (c) 2009 keim All rights reserved.
//  Distributed under BSD-style license (see org.si.license.txt).
//

package org.si.sound.mdx;

import java.util.HashMap;
import java.util.Map;

import vavi.net.URLRequest;


/** PDX data storage */
public class PDXDataStorage {

    /** constructor */
    public PDXDataStorage() {
    }

    // operations
    //

    Map<String, PDXData> self = new HashMap<>();

    /** Clear. */
    public void clear() {
        for (String key : self.keySet()) self.put(key, null);
    }

    /** load pdx data from url */
    public PDXData load(URLRequest url) {
        String fileName = extractFileName(url.url);
        if (self.get(fileName) == null) {
            PDXData pdxData = new PDXData();
            self.put(fileName, pdxData);
            pdxData.load(url);
        }
        return self.get(fileName);
    }

    /** extract file name from url string */
    public String extractFileName(String url) {
        int index = url.lastIndexOf('/');
        return ((index == -1) ? (url) : (url.substring(index + 1))).toUpperCase();
    }
}
