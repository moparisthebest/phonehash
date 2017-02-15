package com.moparisthebest.phonehash;

import sun.misc.Unsafe;
import sun.security.action.GetPropertyAction;

import java.lang.reflect.Field;
import java.nio.ByteOrder;
import java.security.AccessController;

/**
 * Created by mopar on 2/10/17.
 */
public class Sha1 {
    private int[] W = new int[80];
    private int[] state = new int[5];
    private static final int round1_kt = 1518500249;
    private static final int round2_kt = 1859775393;
    private static final int round3_kt = -1894007588;
    private static final int round4_kt = -899497514;

    public Sha1() {
        this("SHA-1", 20, 64);
        this.implReset();
    }

    void implReset() {
        this.state[0] = 1732584193;
        this.state[1] = -271733879;
        this.state[2] = -1732584194;
        this.state[3] = 271733878;
        this.state[4] = -1009589776;
    }

    void implDigest(byte[] var1, int var2) {
        long var3 = this.bytesProcessed << 3;
        int var5 = (int)this.bytesProcessed & 63;
        int var6 = var5 < 56?56 - var5:120 - var5;
        this.engineUpdate(padding, 0, var6);
        i2bBig4((int)(var3 >>> 32), this.buffer, 56);
        i2bBig4((int)var3, this.buffer, 60);
        this.implCompress(this.buffer, 0);
        i2bBig(this.state, 0, var1, var2, 20);
    }

    void implCompress(byte[] var1, int var2) {
        b2iBig64(var1, var2, this.W);

        int var3;
        int var4;
        for(var3 = 16; var3 <= 79; ++var3) {
            var4 = this.W[var3 - 3] ^ this.W[var3 - 8] ^ this.W[var3 - 14] ^ this.W[var3 - 16];
            this.W[var3] = var4 << 1 | var4 >>> 31;
        }

        var3 = this.state[0];
        var4 = this.state[1];
        int var5 = this.state[2];
        int var6 = this.state[3];
        int var7 = this.state[4];

        int var8;
        int var9;
        for(var8 = 0; var8 < 20; ++var8) {
            var9 = (var3 << 5 | var3 >>> 27) + (var4 & var5 | ~var4 & var6) + var7 + this.W[var8] + 1518500249;
            var7 = var6;
            var6 = var5;
            var5 = var4 << 30 | var4 >>> 2;
            var4 = var3;
            var3 = var9;
        }

        for(var8 = 20; var8 < 40; ++var8) {
            var9 = (var3 << 5 | var3 >>> 27) + (var4 ^ var5 ^ var6) + var7 + this.W[var8] + 1859775393;
            var7 = var6;
            var6 = var5;
            var5 = var4 << 30 | var4 >>> 2;
            var4 = var3;
            var3 = var9;
        }

        for(var8 = 40; var8 < 60; ++var8) {
            var9 = (var3 << 5 | var3 >>> 27) + (var4 & var5 | var4 & var6 | var5 & var6) + var7 + this.W[var8] + -1894007588;
            var7 = var6;
            var6 = var5;
            var5 = var4 << 30 | var4 >>> 2;
            var4 = var3;
            var3 = var9;
        }

        for(var8 = 60; var8 < 80; ++var8) {
            var9 = (var3 << 5 | var3 >>> 27) + (var4 ^ var5 ^ var6) + var7 + this.W[var8] + -899497514;
            var7 = var6;
            var6 = var5;
            var5 = var4 << 30 | var4 >>> 2;
            var4 = var3;
            var3 = var9;
        }

        this.state[0] += var3;
        this.state[1] += var4;
        this.state[2] += var5;
        this.state[3] += var6;
        this.state[4] += var7;
    }

    // from DigestBase
    private byte[] oneByte;
    private final String algorithm;
    private final int digestLength;
    private final int blockSize;
    byte[] buffer;
    private int bufOfs;
    long bytesProcessed;
    static final byte[] padding = new byte[136];

    Sha1(String var1, int var2, int var3) {
        this.algorithm = var1;
        this.digestLength = var2;
        this.blockSize = var3;
        this.buffer = new byte[var3];
    }

    protected final int engineGetDigestLength() {
        return this.digestLength;
    }

    protected final void engineUpdate(byte var1) {
        if(this.oneByte == null) {
            this.oneByte = new byte[1];
        }

        this.oneByte[0] = var1;
        this.engineUpdate(this.oneByte, 0, 1);
    }

    protected final void engineUpdate(byte[] var1, int var2, int var3) {
        if(var3 != 0) {
            if(var2 >= 0 && var3 >= 0 && var2 <= var1.length - var3) {
                if(this.bytesProcessed < 0L) {
                    this.engineReset();
                }

                this.bytesProcessed += (long)var3;
                int var4;
                if(this.bufOfs != 0) {
                    var4 = Math.min(var3, this.blockSize - this.bufOfs);
                    System.arraycopy(var1, var2, this.buffer, this.bufOfs, var4);
                    this.bufOfs += var4;
                    var2 += var4;
                    var3 -= var4;
                    if(this.bufOfs >= this.blockSize) {
                        this.implCompress(this.buffer, 0);
                        this.bufOfs = 0;
                    }
                }

                if(var3 >= this.blockSize) {
                    var4 = var2 + var3;
                    var2 = this.implCompressMultiBlock(var1, var2, var4 - this.blockSize);
                    var3 = var4 - var2;
                }

                if(var3 > 0) {
                    System.arraycopy(var1, var2, this.buffer, 0, var3);
                    this.bufOfs = var3;
                }

            } else {
                throw new ArrayIndexOutOfBoundsException();
            }
        }
    }

    private int implCompressMultiBlock(byte[] var1, int var2, int var3) {
        while(var2 <= var3) {
            this.implCompress(var1, var2);
            var2 += this.blockSize;
        }

        return var2;
    }

    protected final void engineReset() {
        if(this.bytesProcessed != 0L) {
            this.implReset();
            this.bufOfs = 0;
            this.bytesProcessed = 0L;
        }
    }

    protected final int engineDigest(byte[] var1, int var2, int var3) {
        if(var3 < this.digestLength) {
            throw new ArrayIndexOutOfBoundsException("Length must be at least " + this.digestLength + " for " + this.algorithm + "digests");
        } else if(var2 >= 0 && var3 >= 0 && var2 <= var1.length - var3) {
            if(this.bytesProcessed < 0L) {
                this.engineReset();
            }

            this.implDigest(var1, var2);
            this.bytesProcessed = -1L;
            return this.digestLength;
        } else {
            throw new ArrayIndexOutOfBoundsException("Length must be at least " + this.digestLength + " for " + this.algorithm + "digests");
        }
    }

    // from ByteArrayAccess
    private static final Unsafe unsafe;
    private static final boolean littleEndianUnaligned;
    private static final boolean bigEndian;
    private static final int byteArrayOfs;

    static {
        // from DigestBase
        padding[0] = -128;
        // for the following
        try {
            final Field field = Unsafe.class.getDeclaredField("theUnsafe");
            field.setAccessible(true);
            unsafe = (Unsafe)field.get(null);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        // from ByteArrayAccess
        byteArrayOfs = unsafe.arrayBaseOffset(byte[].class);
        boolean var0 = unsafe.arrayIndexScale(byte[].class) == 1 && unsafe.arrayIndexScale(int[].class) == 4 && unsafe.arrayIndexScale(long[].class) == 8 && (byteArrayOfs & 3) == 0;
        ByteOrder var1 = ByteOrder.nativeOrder();
        littleEndianUnaligned = var0 && unaligned() && var1 == ByteOrder.LITTLE_ENDIAN;
        bigEndian = var0 && var1 == ByteOrder.BIG_ENDIAN;
    }

    private static boolean unaligned() {
        String var0 = (String) AccessController.doPrivileged(new GetPropertyAction("os.arch", ""));
        return var0.equals("i386") || var0.equals("x86") || var0.equals("amd64") || var0.equals("x86_64");
    }

    static void i2bBig4(int var0, byte[] var1, int var2) {
        if(var2 >= 0 && var1.length - var2 >= 4) {
            if(littleEndianUnaligned) {
                unsafe.putInt(var1, (long)(byteArrayOfs + var2), Integer.reverseBytes(var0));
            } else if(bigEndian && (var2 & 3) == 0) {
                unsafe.putInt(var1, (long)(byteArrayOfs + var2), var0);
            } else {
                var1[var2] = (byte)(var0 >> 24);
                var1[var2 + 1] = (byte)(var0 >> 16);
                var1[var2 + 2] = (byte)(var0 >> 8);
                var1[var2 + 3] = (byte)var0;
            }

        } else {
            throw new ArrayIndexOutOfBoundsException();
        }
    }

    static void i2bBig(int[] var0, int var1, byte[] var2, int var3, int var4) {
        if(var1 >= 0 && var0.length - var1 >= var4 / 4 && var3 >= 0 && var2.length - var3 >= var4) {
            if(littleEndianUnaligned) {
                var3 += byteArrayOfs;

                for(var4 += var3; var3 < var4; var3 += 4) {
                    unsafe.putInt(var2, (long)var3, Integer.reverseBytes(var0[var1++]));
                }
            } else {
                int var5;
                if(bigEndian && (var3 & 3) == 0) {
                    var3 += byteArrayOfs;

                    for(var4 += var3; var3 < var4; var3 += 4) {
                        unsafe.putInt(var2, (long)var3, var0[var1++]);
                    }
                } else {
                    for(var4 += var3; var3 < var4; var2[var3++] = (byte)var5) {
                        var5 = var0[var1++];
                        var2[var3++] = (byte)(var5 >> 24);
                        var2[var3++] = (byte)(var5 >> 16);
                        var2[var3++] = (byte)(var5 >> 8);
                    }
                }
            }

        } else {
            throw new ArrayIndexOutOfBoundsException();
        }
    }

    static void b2iBig64(byte[] var0, int var1, int[] var2) {
        if(var1 >= 0 && var0.length - var1 >= 64 && var2.length >= 16) {
            if(littleEndianUnaligned) {
                var1 += byteArrayOfs;
                var2[0] = Integer.reverseBytes(unsafe.getInt(var0, (long)var1));
                var2[1] = Integer.reverseBytes(unsafe.getInt(var0, (long)(var1 + 4)));
                var2[2] = Integer.reverseBytes(unsafe.getInt(var0, (long)(var1 + 8)));
                var2[3] = Integer.reverseBytes(unsafe.getInt(var0, (long)(var1 + 12)));
                var2[4] = Integer.reverseBytes(unsafe.getInt(var0, (long)(var1 + 16)));
                var2[5] = Integer.reverseBytes(unsafe.getInt(var0, (long)(var1 + 20)));
                var2[6] = Integer.reverseBytes(unsafe.getInt(var0, (long)(var1 + 24)));
                var2[7] = Integer.reverseBytes(unsafe.getInt(var0, (long)(var1 + 28)));
                var2[8] = Integer.reverseBytes(unsafe.getInt(var0, (long)(var1 + 32)));
                var2[9] = Integer.reverseBytes(unsafe.getInt(var0, (long)(var1 + 36)));
                var2[10] = Integer.reverseBytes(unsafe.getInt(var0, (long)(var1 + 40)));
                var2[11] = Integer.reverseBytes(unsafe.getInt(var0, (long)(var1 + 44)));
                var2[12] = Integer.reverseBytes(unsafe.getInt(var0, (long)(var1 + 48)));
                var2[13] = Integer.reverseBytes(unsafe.getInt(var0, (long)(var1 + 52)));
                var2[14] = Integer.reverseBytes(unsafe.getInt(var0, (long)(var1 + 56)));
                var2[15] = Integer.reverseBytes(unsafe.getInt(var0, (long)(var1 + 60)));
            } else if(bigEndian && (var1 & 3) == 0) {
                var1 += byteArrayOfs;
                var2[0] = unsafe.getInt(var0, (long)var1);
                var2[1] = unsafe.getInt(var0, (long)(var1 + 4));
                var2[2] = unsafe.getInt(var0, (long)(var1 + 8));
                var2[3] = unsafe.getInt(var0, (long)(var1 + 12));
                var2[4] = unsafe.getInt(var0, (long)(var1 + 16));
                var2[5] = unsafe.getInt(var0, (long)(var1 + 20));
                var2[6] = unsafe.getInt(var0, (long)(var1 + 24));
                var2[7] = unsafe.getInt(var0, (long)(var1 + 28));
                var2[8] = unsafe.getInt(var0, (long)(var1 + 32));
                var2[9] = unsafe.getInt(var0, (long)(var1 + 36));
                var2[10] = unsafe.getInt(var0, (long)(var1 + 40));
                var2[11] = unsafe.getInt(var0, (long)(var1 + 44));
                var2[12] = unsafe.getInt(var0, (long)(var1 + 48));
                var2[13] = unsafe.getInt(var0, (long)(var1 + 52));
                var2[14] = unsafe.getInt(var0, (long)(var1 + 56));
                var2[15] = unsafe.getInt(var0, (long)(var1 + 60));
            } else {
                b2iBig(var0, var1, var2, 0, 64);
            }

        } else {
            throw new ArrayIndexOutOfBoundsException();
        }
    }

    static void b2iBig(byte[] var0, int var1, int[] var2, int var3, int var4) {
        if(var1 >= 0 && var0.length - var1 >= var4 && var3 >= 0 && var2.length - var3 >= var4 / 4) {
            if(littleEndianUnaligned) {
                var1 += byteArrayOfs;

                for(var4 += var1; var1 < var4; var1 += 4) {
                    var2[var3++] = Integer.reverseBytes(unsafe.getInt(var0, (long)var1));
                }
            } else if(bigEndian && (var1 & 3) == 0) {
                var1 += byteArrayOfs;

                for(var4 += var1; var1 < var4; var1 += 4) {
                    var2[var3++] = unsafe.getInt(var0, (long)var1);
                }
            } else {
                for(var4 += var1; var1 < var4; var1 += 4) {
                    var2[var3++] = var0[var1 + 3] & 255 | (var0[var1 + 2] & 255) << 8 | (var0[var1 + 1] & 255) << 16 | var0[var1] << 24;
                }
            }

        } else {
            throw new ArrayIndexOutOfBoundsException();
        }
    }
}
