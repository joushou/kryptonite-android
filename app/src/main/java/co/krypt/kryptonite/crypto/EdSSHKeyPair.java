package co.krypt.kryptonite.crypto;

import android.support.annotation.NonNull;
import android.util.Base64;
import android.util.Log;

import org.libsodium.jni.Sodium;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.SignatureException;
import java.security.spec.InvalidKeySpecException;

import co.krypt.kryptonite.exception.CryptoException;

/**
 * Created by Kevin King on 11/30/16.
 * Copyright 2016. KryptCo, Inc.
 */

public class EdSSHKeyPair implements SSHKeyPairI {
    private final @NonNull byte[] pk;
    private final @NonNull byte[] sk;
    private static final String TAG = "SSHKeyPair";

    EdSSHKeyPair(@NonNull byte[] pk, @NonNull byte[] sk) {
        this.pk = pk;
        this.sk = sk;
    }

    public String publicKeyDERBase64() {
        return Base64.encodeToString(pk, Base64.DEFAULT);
    }

    public byte[] publicKeySSHWireFormat() throws InvalidKeyException, IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        out.write(SSHWire.encode("ssh-ed25519".getBytes()));
        out.write(SSHWire.encode(pk));

        return out.toByteArray();
    }

    public byte[] publicKeyFingerprint() throws IOException, InvalidKeyException, CryptoException {
        return SHA256.digest(publicKeySSHWireFormat());
    }

    @Override
    public byte[] signDigest(String digest, byte[] data) throws NoSuchAlgorithmException, InvalidKeyException, SignatureException, CryptoException, NoSuchProviderException, InvalidKeySpecException {
        //  digest part of ed25519 spec
        return signDigest(data);
    }

    @Override
    public byte[] signDigestAppendingPubkey(byte[] data, String algo) throws SignatureException, IOException, InvalidKeyException, NoSuchAlgorithmException, CryptoException, NoSuchProviderException, InvalidKeySpecException {
        ByteArrayOutputStream dataWithPubkey = new ByteArrayOutputStream();
        dataWithPubkey.write(data);
        dataWithPubkey.write(SSHWire.encode(publicKeySSHWireFormat()));

        byte[] signaturePayload = dataWithPubkey.toByteArray();

        return signDigest(signaturePayload);
    }

    @Override
    public boolean verifyDigest(String digest, byte[] signature, byte[] data) throws NoSuchAlgorithmException, InvalidKeyException, SignatureException, CryptoException, InvalidKeySpecException, NoSuchProviderException {
        //  digest part of ed25519 spec
        return verifyDigest(signature, data);
    }

    public byte[] signDigest(byte[] data) throws SignatureException {
        long start = System.currentTimeMillis();
        byte[] signature = new byte[Sodium.crypto_sign_ed25519_bytes()];
        int[] signatureLengthPointer = new int[1];
        int result = Sodium.crypto_sign_ed25519_detached(signature, signatureLengthPointer, data, data.length, sk);
        if (result != 0) {
            throw new SignatureException("non-zero sodium return: " + result);
        }
        long stop = System.currentTimeMillis();
        Log.d(TAG, "signature took " + String.valueOf((stop - start) / 1000.0) + " seconds");
        return signature;
    }

    public boolean verifyDigest(byte[] signature, byte[] data) throws NoSuchAlgorithmException, InvalidKeyException, SignatureException {
        return 0 == Sodium.crypto_sign_ed25519_verify_detached(signature, data, data.length, pk);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        EdSSHKeyPair that = (EdSSHKeyPair) o;

        return publicKeyDERBase64().equals(that.publicKeyDERBase64());
    }
}
