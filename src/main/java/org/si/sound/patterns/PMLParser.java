//
// PML (Pattern/Primitive Macro Language) parser
//  Copyright (c) 2009 keim All rights reserved.
//  Distributed under BSD-style license (see org.si.license.txt).
//

package org.si.sound.patterns;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


/**
 * PML (Pattern/Primitive Macro Language) parser, this class provides quite simple pattern generator.
 *
 * @example The PML string instanceof translated by rule property's key to Note instance. The letter "^" extends previous Note's length and the letter "[...]n" is ((a) translated) loop. The letters not included in the rule property are translated to rest.
 * <pre>
 * PMLParser pp = new PMLParser();
 * pp.rule = {"A":new Note(60), "B":new Note(72)}; // set rule. letter "A" as Note(60) and letter "B" as Note(72).
 * Vector pat1.&lt;Note&gt; = pp.parse("A B AABB");  // generate pattern. The PML "A B AABB" is simply translated by rule.
 * // The whitespaces are translated to rest.
 * for (int i=0; i&lt;pat1.length; i++) {
 * trace(pat1[i].note);                        // output "60 -1  72 -1  60  60  72  72" (rest's note property is -1)
 * }
 * Vector pat2.&lt;Note&gt; = pp.parse("A B A^^^");  // generate pattern. The letter "^" extends previous Note's length.
 * Vector pat3.&lt;Note&gt; = pp.parse("[A B ]2");   // generate pattern. The letter "[...]n" is translated as a loop. you cannot nest loops.
 * </pre>
 */
public class PMLParser {

    // variables
    //

    /** parsing rule. */
    public Map<String, Note> rule;

    // constructor
    //

    /** constructor */
    PMLParser(Map<String, Note> rule) {
        this.rule = rule != null ? rule : new HashMap<>();
    }

    // operation
    //

    /**
     * generate pattern from PML.
     *
     * @param pml ((string) pattern).
     */
    public Note[] parse(String pml) {
        Pattern _pattern = Pattern.compile("\\[(.+?)\\](\\d*)");
        Matcher matcher = _pattern.matcher(pml);
        if (matcher.find()) {
            String group = matcher.group(1);
            String repStr = matcher.group(2);

            int rep = repStr.isEmpty() ? 2 : Integer.parseInt(repStr);

            StringBuilder sb = new StringBuilder(group.length() * rep);
            sb.append(group.repeat(Math.max(0, rep)));

            pml = matcher.replaceFirst(Matcher.quoteReplacement(sb.toString()));
        }
        int imax = pml.length();
        Note[] pattern = new Note[imax];
        int i;
        String l;
        Note org, prev = null;
        for (i = 0; i < imax; i++) {
            l = String.valueOf(pml.charAt(i));
            org = rule.get(l);
            if (org != null) {
                pattern[i] = (new Note()).copyFrom(org);
                prev = pattern[i];
            } else if (l.equals("^") && prev != null && !Double.isNaN(prev.length)) {
                prev.length += 1;
            }
        }

        return pattern;
    }
}
