package com.moparisthebest.phonehash;

import com.moparisthebest.filelist.TransformComparator;

/**
 * Created by mopar on 2/10/17.
 */
public class PhoneComparator implements java.util.Comparator<Long>, TransformComparator<byte[], Long> {

    private static final boolean debug = false;

    // format is +00000000000
    // +14404746223 -> 3a6da5520baee5353448fd95cb036279da7f6b5f
    // +00000000023 -> 18ff74bfab994d0b7f5a52fbbdcce34c0bc309e3

    private static final byte[][] prefix = new byte[][]{
            "+00000000000".getBytes(),
            "+0000000000".getBytes(),
            "+000000000".getBytes(),
            "+00000000".getBytes(),
            "+0000000".getBytes(),
            "+000000".getBytes(),
            "+00000".getBytes(),
            "+0000".getBytes(),
            "+000".getBytes(),
            "+00".getBytes(),
            "+0".getBytes(),
            "+".getBytes(),
    };

    private static final String[] stringPrefix = new String[]{
            "+00000000000",
            "+0000000000",
            "+000000000",
            "+00000000",
            "+0000000",
            "+000000",
            "+00000",
            "+0000",
            "+000",
            "+00",
            "+0",
            "+",
    };

    public static String formatPhoneNumber(final Long num){
        final String numString = num.toString();
        return stringPrefix[numString.length()] + numString;
    }

    /*
    public PhoneComparator() throws NoSuchAlgorithmException {
    }

    // safe method
    private final MessageDigest md = MessageDigest.getInstance("SHA-1");

    private byte[] safeHash(final Long o) {
        final byte[] s = o.toString().getBytes();
        md.reset();
        md.update(prefix[s.length]);
        final byte[] ret = md.digest(s);
        if (debug)
            System.out.printf("num '%s%s' hash '%s'%n",
                    new String(prefix[s.length]), new String(s), String.format("%040x", new java.math.BigInteger(1, ret)));
        return ret;
    }
    */

    // fast and dirty method
    private final Sha1 digest = new Sha1();
    // 2 to store sha1 hashes, 1 to store longs as byte[] for hashing
    private final byte[] s1hash = new byte[20], s2hash = new byte[20], lArr = new byte[11];

    public void fastHash(final long o, final byte[] hash) {
        final int size = stringSize(o);
        getChars(o, size, lArr);
        digest.engineReset();
        final byte[] sPrefix = prefix[size];
        digest.engineUpdate(sPrefix, 0, sPrefix.length);
        digest.engineUpdate(lArr, 0, size);
        digest.engineDigest(hash, 0, hash.length);
        if (debug)
            System.out.printf("num '%s%s' hash '%s'%n",
                    new String(prefix[size]), new String(lArr, 0, size), String.format("%040x", new java.math.BigInteger(1, hash)));
    }

    public byte[] fastHashCopy(final long o) {
        final byte[] hash = new byte[20];
        this.fastHash(o, hash);
        return hash;
    }

    public byte[] fastHashReadOnly(final long o) {
        this.fastHash(o, s1hash);
        return s1hash;
    }

    @Override
    public int compare(final Long o1, final Long o2) {
        //final byte[] s1hash = safeHash(o1);
        fastHash(o1, s1hash);
        return compareTransform(s1hash, o2);
    }

    @Override
    public int compareTransform(final byte[] s1hash, final Long o2) {
        //final byte[] s2hash = safeHash(o2);
        fastHash(o2, s2hash);
        for (int x = 0; x < s1hash.length; ++x) {
            final int y = s1hash[x] - s2hash[x];
            if (y != 0)
                return y;
        }
        return 0;
    }

    // below was pulled and modified from Long and Integer to convert positive longs directly to byte[], producing the same effect
    // as someLong.toString().getBytes(), but MUCH faster, and writing into existing buffers instead of creating new

    /**
     * All possible chars for representing a number as a String
     */
    final static byte[] digits = {
            '0' , '1' , '2' , '3' , '4' , '5' ,
            '6' , '7' , '8' , '9' , 'a' , 'b' ,
            'c' , 'd' , 'e' , 'f' , 'g' , 'h' ,
            'i' , 'j' , 'k' , 'l' , 'm' , 'n' ,
            'o' , 'p' , 'q' , 'r' , 's' , 't' ,
            'u' , 'v' , 'w' , 'x' , 'y' , 'z'
    };

    final static byte [] DigitTens = {
            '0', '0', '0', '0', '0', '0', '0', '0', '0', '0',
            '1', '1', '1', '1', '1', '1', '1', '1', '1', '1',
            '2', '2', '2', '2', '2', '2', '2', '2', '2', '2',
            '3', '3', '3', '3', '3', '3', '3', '3', '3', '3',
            '4', '4', '4', '4', '4', '4', '4', '4', '4', '4',
            '5', '5', '5', '5', '5', '5', '5', '5', '5', '5',
            '6', '6', '6', '6', '6', '6', '6', '6', '6', '6',
            '7', '7', '7', '7', '7', '7', '7', '7', '7', '7',
            '8', '8', '8', '8', '8', '8', '8', '8', '8', '8',
            '9', '9', '9', '9', '9', '9', '9', '9', '9', '9',
    } ;

    final static byte [] DigitOnes = {
            '0', '1', '2', '3', '4', '5', '6', '7', '8', '9',
            '0', '1', '2', '3', '4', '5', '6', '7', '8', '9',
            '0', '1', '2', '3', '4', '5', '6', '7', '8', '9',
            '0', '1', '2', '3', '4', '5', '6', '7', '8', '9',
            '0', '1', '2', '3', '4', '5', '6', '7', '8', '9',
            '0', '1', '2', '3', '4', '5', '6', '7', '8', '9',
            '0', '1', '2', '3', '4', '5', '6', '7', '8', '9',
            '0', '1', '2', '3', '4', '5', '6', '7', '8', '9',
            '0', '1', '2', '3', '4', '5', '6', '7', '8', '9',
            '0', '1', '2', '3', '4', '5', '6', '7', '8', '9',
    } ;

    /**
     * Places characters representing the integer i into the
     * character array buf. The characters are placed into
     * the buffer backwards starting with the least significant
     * digit at the specified index (exclusive), and working
     * backwards from there.
     *
     * Will fail if i == Long.MIN_VALUE
     */
    static void getChars(long i, final int index, final byte[] buf) {
        long q;
        int r;
        int charPos = index;

        // Get 2 digits/iteration using longs until quotient fits into an int
        while (i > Integer.MAX_VALUE) {
            q = i / 100;
            // really: r = i - (q * 100);
            r = (int)(i - ((q << 6) + (q << 5) + (q << 2)));
            i = q;
            buf[--charPos] = DigitOnes[r];
            buf[--charPos] = DigitTens[r];
        }

        // Get 2 digits/iteration using ints
        int q2;
        int i2 = (int)i;
        while (i2 >= 65536) {
            q2 = i2 / 100;
            // really: r = i2 - (q * 100);
            r = i2 - ((q2 << 6) + (q2 << 5) + (q2 << 2));
            i2 = q2;
            buf[--charPos] = DigitOnes[r];
            buf[--charPos] = DigitTens[r];
        }

        // Fall thru to fast mode for smaller numbers
        // assert(i2 <= 65536, i2);
        for (;;) {
            q2 = (i2 * 52429) >>> (16+3);
            r = i2 - ((q2 << 3) + (q2 << 1));  // r = i2-(q2*10) ...
            buf[--charPos] = digits[r];
            i2 = q2;
            if (i2 == 0) break;
        }
    }

    // Requires positive x
    static int stringSize(final long x) {
        long p = 10;
        for (int i=1; i<19; ++i) {
            if (x < p)
                return i;
            p = 10*p;
        }
        return 19;
    }
}
