package com.moparisthebest.phonehash;

import java.util.Comparator;

/**
 * Created by mopar on 2/10/17.
 */
public class ByteArrayComparator implements Comparator<byte[]> {
    @Override
    public int compare(final byte[] o1, final byte[] o2) {
        for (int x = 0; x < o1.length; ++x) {
            final int y = o1[x] - o2[x];
            if (y != 0)
                return y;
        }
        return 0;
    }
}
