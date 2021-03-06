package no.nordicsemi.android.meshprovisioner.utils;

import android.os.Parcel;
import android.os.Parcelable;

import org.spongycastle.crypto.BlockCipher;
import org.spongycastle.crypto.CipherParameters;
import org.spongycastle.crypto.InvalidCipherTextException;
import org.spongycastle.crypto.engines.AESEngine;
import org.spongycastle.crypto.engines.AESLightEngine;
import org.spongycastle.crypto.macs.CMac;
import org.spongycastle.crypto.modes.CCMBlockCipher;
import org.spongycastle.crypto.params.AEADParameters;
import org.spongycastle.crypto.params.KeyParameter;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.Charset;
import java.security.SecureRandom;
import java.security.Security;

public class SecureUtils {

    /**
     * Used to calculate the confirmation key
     */
    public static final byte[] PRCK = "prck".getBytes(Charset.forName("US-ASCII"));
    /**
     * Used to calculate the session key
     */
    public static final byte[] PRSK = "prsk".getBytes(Charset.forName("US-ASCII"));
    /**
     * Used to calculate the session nonce
     */
    public static final byte[] PRSN = "prsn".getBytes(Charset.forName("US-ASCII"));
    /**
     * Used to calculate the device key
     */
    public static final byte[] PRDK = "prdk".getBytes(Charset.forName("US-ASCII"));
    /**
     * K2 Master input
     */
    public static final byte[] K2_MASTER_INPUT = {0x00};
    /**
     * Salt input for K2
     */
    public static final byte[] SMK2 = "smk2".getBytes(Charset.forName("US-ASCII"));
    /**
     * Salt input for K3
     */
    public static final byte[] SMK3 = "smk3".getBytes(Charset.forName("US-ASCII"));
    /**
     * Input for K3 data
     */
    public static final byte[] SMK3_DATA = "id64".getBytes(Charset.forName("US-ASCII"));
    /**
     * Output mask for K4
     */
    public static final int ENC_K3_OUTPUT_MASK = 0x7f;
    /**
     * Salt input for K4
     */
    public static final byte[] SMK4 = "smk4".getBytes(Charset.forName("US-ASCII"));
    /**
     * Input for K4 data
     */
    public static final byte[] SMK4_DATA = "id6".getBytes(Charset.forName("US-ASCII"));
    /**
     * Output mask for K4
     */
    public static final int ENC_K4_OUTPUT_MASK = 0x3f;
    //For S1, the key is constant
    protected static final byte[] SALT_KEY = {0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00};
    //Padding for the random nonce
    protected static final byte[] NONCE_PADDING = {0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00};
    private static final String TAG = SecureUtils.class.getSimpleName();
    /**
     * Salt input for identity key
     */
    private static final byte[] NKIK = "nkik".getBytes(Charset.forName("US-ASCII"));

    /**
     * Salt input for identity key
     */
    private static final byte[] ID128 = "id128".getBytes(Charset.forName("US-ASCII"));
    //Padding for the random nonce
    private static final byte[] HASH_PADDING = {0x00, 0x00, 0x00, 0x00, 0x00, 0x00};
    private static final int HASH_LENGTH = 8;
    public static int NRF_MESH_KEY_SIZE = 16;

    static {
        Security.insertProviderAt(new org.spongycastle.jce.provider.BouncyCastleProvider(), 1);
    }

    public static final byte[] generateRandomNumber() {
        final SecureRandom random = new SecureRandom();
        final byte[] randomBytes = new byte[16];
        random.nextBytes(randomBytes);

        return randomBytes;
    }

    /**
     * Generates a random number based on the number of bits
     *
     * @param bits number of bits of the random number
     * @return random number of bytes
     */
    public static final byte[] generateRandomNumber(final int bits) {
        final SecureRandom random = new SecureRandom();
        final byte[] randomBytes = new byte[bits / 8];
        random.nextBytes(randomBytes);

        return randomBytes;
    }

    public static final byte[] generateRandomNonce() {
        final SecureRandom random = new SecureRandom();
        final byte[] randomBytes = new byte[8];
        random.nextBytes(randomBytes);
        final int length = NONCE_PADDING.length + randomBytes.length;
        final ByteBuffer bufferRandomNonce = ByteBuffer.allocate(length).order(ByteOrder.BIG_ENDIAN);
        bufferRandomNonce.put(NONCE_PADDING);
        bufferRandomNonce.put(randomBytes);
        return randomBytes;
    }

    public static final String generateRandomNetworkKey() {
        final byte[] networkKey = generateRandomNumber();
        return MeshParserUtils.bytesToHex(networkKey, false);
    }

    public static final String generateRandomApplicationKey() {
        return MeshParserUtils.bytesToHex(generateRandomNumber(), false);
    }


    public static final byte[] calculateSalt(final byte[] data) {
        return calculateCMAC(data, SALT_KEY);
    }

    public static final byte[] calculateCMAC(final byte[] data, final byte[] key) {
        final byte[] cmac = new byte[16];

        CipherParameters cipherParameters = new KeyParameter(key);
        BlockCipher blockCipher = new AESEngine();
        CMac mac = new CMac(blockCipher);

        mac.init(cipherParameters);
        mac.update(data, 0, data.length);
        mac.doFinal(cmac, 0);
        return cmac;
    }

    public static final byte[] calculateCMAC(final byte[] data, final byte[] key, final int offset) {
        final byte[] cmac = new byte[data.length];

        CipherParameters cipherParameters = new KeyParameter(key);
        BlockCipher blockCipher = new AESEngine();
        CMac mac = new CMac(blockCipher);

        mac.init(cipherParameters);
        mac.update(data, offset, data.length);
        mac.doFinal(cmac, 0);
        return cmac;
    }

    public static final byte[] encryptCCM(final byte[] data, final byte[] key, final byte[] nonce) {
        final byte[] ccm = new byte[25 + 8];
        final ByteBuffer buffer = ByteBuffer.allocate(ccm.length + 8);
        CCMBlockCipher ccmBlockCipher = new CCMBlockCipher(new AESEngine());

        AEADParameters aeadParameters = new AEADParameters(new KeyParameter(key), 64, nonce);
        ccmBlockCipher.init(true, aeadParameters);
        ccmBlockCipher.processBytes(data, 0, data.length, ccm, data.length);
        try {
            ccmBlockCipher.doFinal(ccm, 0);
        } catch (InvalidCipherTextException e) {
            e.printStackTrace();
        }
        return ccm;
    }


    public static final byte[] encryptCCM(final byte[] data, final byte[] key, final byte[] nonce, final int micSize) {
        final byte[] ccm = new byte[data.length + micSize];

        CCMBlockCipher ccmBlockCipher = new CCMBlockCipher(new AESEngine());

        AEADParameters aeadParameters = new AEADParameters(new KeyParameter(key), micSize * 8, nonce);
        ccmBlockCipher.init(true, aeadParameters);
        ccmBlockCipher.processBytes(data, 0, data.length, ccm, data.length);
        try {
            ccmBlockCipher.doFinal(ccm, 0);
        } catch (InvalidCipherTextException e) {
            e.printStackTrace();
        }
        return ccm;
    }


    public static final byte[] decryptCCM(final byte[] data, final byte[] key, final byte[] nonce, final int micSize) {
        final byte[] ccm = new byte[data.length];

        CCMBlockCipher ccmBlockCipher = new CCMBlockCipher(new AESEngine());
        AEADParameters aeadParameters = new AEADParameters(new KeyParameter(key), micSize * 8, nonce);
        ccmBlockCipher.init(false, aeadParameters);
        ccmBlockCipher.processBytes(data, 0, data.length, ccm, 0);
        try {
            ccmBlockCipher.doFinal(ccm, 0);
        } catch (InvalidCipherTextException e) {
            e.printStackTrace();
        }
        final int ccmLength = data.length - micSize;
        final ByteBuffer ccmBuffer = ByteBuffer.allocate(ccmLength).order(ByteOrder.BIG_ENDIAN);
        ccmBuffer.put(ccm, 0, ccmLength);
        return ccmBuffer.array();
    }

    public static final byte[] calculateK1(final byte[] ecdh, final byte[] confirmationSalt, final byte[] text) {
        return calculateCMAC(text, calculateCMAC(ecdh, confirmationSalt));
    }

    /**
     * Calculate k2
     *
     * @param data network key
     * @param p
     * @return
     */
    public static K2Output calculateK2(final byte[] data, final byte[] p) {
        final byte[] salt = calculateSalt(SMK2);
        final byte[] t = calculateCMAC(data, salt);

        final byte[] t0 = {};
        final ByteBuffer inputBufferT0 = ByteBuffer.allocate(t0.length + p.length + 1);
        inputBufferT0.put(t0);
        inputBufferT0.put(p);
        inputBufferT0.put((byte) 0x01);
        final byte[] t1 = calculateCMAC(inputBufferT0.array(), t);
        final byte nid = (byte) (t1[15] & 0x7F);

        final ByteBuffer inputBufferT1 = ByteBuffer.allocate(t1.length + p.length + 1);
        inputBufferT1.put(t1);
        inputBufferT1.put(p);
        inputBufferT1.put((byte) 0x02);
        final byte[] encryptionKey = calculateCMAC(inputBufferT1.array(), t);

        final ByteBuffer inputBufferT2 = ByteBuffer.allocate(encryptionKey.length + p.length + 1);
        inputBufferT2.put(encryptionKey);
        inputBufferT2.put(p);
        inputBufferT2.put((byte) 0x03);
        final byte[] privacyKey = calculateCMAC(inputBufferT2.array(), t);

        return new K2Output(nid, encryptionKey, privacyKey);
    }

    /**
     * Calculate k3
     *
     * @param n network key
     * @return
     */
    public static byte[] calculateK3(final byte[] n) {

        final byte[] salt = calculateSalt(SMK3);

        final byte[] t = calculateCMAC(n, salt);

        ByteBuffer buffer = ByteBuffer.allocate(SMK3_DATA.length + 1);
        buffer.put(SMK3_DATA);
        buffer.put((byte) 0x01);
        final byte[] cmacInput = buffer.array();

        final byte[] result = calculateCMAC(cmacInput, t);

        //Only the least siginificant 8 bytes are returned
        final byte[] networkId = new byte[8];
        final int srcOffset = result.length - networkId.length;

        System.arraycopy(result, srcOffset, networkId, 0, networkId.length);
        //bBuffer.
        return networkId;
    }

    /**
     * Calculate k4
     *
     * @param n network key
     * @return
     */
    public static final byte calculateK4(final byte[] n) {

        byte[] salt = calculateSalt(SMK4);

        final byte[] t = calculateCMAC(n, salt);

        ByteBuffer buffer = ByteBuffer.allocate(SMK4_DATA.length + 1);
        buffer.put(SMK4_DATA);
        buffer.put((byte) 0x01);
        final byte[] cmacInput = buffer.array();

        final byte[] result = calculateCMAC(cmacInput, t);

        //Only the least siginificant 6 bytes are returned
        return (byte) ((result[15]) & 0x3F);
    }

    /**
     * Calculates the identity key
     *
     * @param n network key
     * @return hash value
     */
    public static final byte[] calculateIdentityKey(final byte[] n) {
        final byte[] salt = calculateSalt(NKIK);
        ByteBuffer buffer = ByteBuffer.allocate(ID128.length + 1);
        buffer.put(ID128);
        buffer.put((byte) 0x01);
        final byte[] p = buffer.array();
        return calculateK1(n, salt, p);
    }

    /**
     * Calculates hash value for advertising with node id
     *
     * @param identityKey resolving identity key
     * @param random      64-bit random value
     * @param src         unicast address of the node
     * @return hash value
     */
    public static byte[] calculateHash(final byte[] identityKey, final byte[] random, final byte[] src) {
        final int length = HASH_PADDING.length + random.length + src.length;
        final ByteBuffer bufferHashInput = ByteBuffer.allocate(length).order(ByteOrder.BIG_ENDIAN);
        bufferHashInput.put(HASH_PADDING);
        bufferHashInput.put(random);
        bufferHashInput.put(src);
        final byte[] hashInput = bufferHashInput.array();
        final byte[] hash = SecureUtils.encryptWithAES(hashInput, identityKey);

        final ByteBuffer buffer = ByteBuffer.allocate(HASH_LENGTH).order(ByteOrder.BIG_ENDIAN);
        buffer.put(hash, 8, HASH_LENGTH);

        return buffer.array();
    }

    public static final byte[] encryptWithAES(final byte[] data, final byte[] key) {
        final byte[] encrypted = new byte[data.length];
        final CipherParameters cipherParameters = new KeyParameter(key);
        final AESLightEngine engine = new AESLightEngine();
        engine.init(true, cipherParameters);
        engine.processBlock(data, 0, encrypted, 0);

        return encrypted;
    }

    public static final byte[] decryptWithAES(final byte[] data, final byte[] key) {
        final byte[] decrypted = new byte[data.length];
        final CipherParameters cipherParameters = new KeyParameter(key);
        final AESLightEngine engine = new AESLightEngine();
        engine.init(false, cipherParameters);
        engine.processBlock(data, 0, decrypted, 0);

        return decrypted;
    }

    public static int getNetMicLength(final int ctl) {
        if (ctl == 0) {
            return 4; //length;
        } else {
            return 8; //length
        }
    }

    /**
     * Gets the transport MIC length based on the aszmic value
     *
     * @param aszmic
     * @return
     */
    public static int getTransMicLength(final int aszmic) {
        if (aszmic == 0) {
            return 4; //length;
        } else {
            return 8; //length
        }
    }

    public static class K2Output implements Parcelable {
        public static final Creator<K2Output> CREATOR = new Creator<K2Output>() {
            @Override
            public K2Output createFromParcel(Parcel in) {
                return new K2Output(in);
            }

            @Override
            public K2Output[] newArray(int size) {
                return new K2Output[size];
            }
        };
        private byte nid;
        private byte[] encryptionKey;
        private byte[] privacyKey;

        private K2Output(final byte nid, final byte[] encryptionKey, final byte[] privacyKey) {
            this.nid = nid;
            this.encryptionKey = encryptionKey;
            this.privacyKey = privacyKey;
        }

        protected K2Output(Parcel in) {
            nid = in.readByte();
            encryptionKey = in.createByteArray();
            privacyKey = in.createByteArray();
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            dest.writeByte(nid);
            dest.writeByteArray(encryptionKey);
            dest.writeByteArray(privacyKey);
        }

        @Override
        public int describeContents() {
            return 0;
        }

        public byte getNid() {
            return nid;
        }

        public byte[] getEncryptionKey() {
            return encryptionKey;
        }

        public byte[] getPrivacyKey() {
            return privacyKey;
        }
    }
}
