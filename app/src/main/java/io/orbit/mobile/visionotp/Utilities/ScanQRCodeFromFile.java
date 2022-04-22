/*
 * Copyright (C) 2017-2020 Jakob Nixdorf
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */


package io.orbit.mobile.visionotp.Utilities;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.widget.Toast;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.BinaryBitmap;
import com.google.zxing.ChecksumException;
import com.google.zxing.DecodeHintType;
import com.google.zxing.FormatException;
import com.google.zxing.LuminanceSource;
import com.google.zxing.NotFoundException;
import com.google.zxing.RGBLuminanceSource;
import com.google.zxing.ReaderException;
import com.google.zxing.Result;
import com.google.zxing.common.HybridBinarizer;
import com.google.zxing.qrcode.QRCodeReader;

import org.shadowice.flocke.andotp.R;

import java.io.IOException;
import java.util.EnumMap;
import java.util.Map;
import java.util.Vector;

public class ScanQRCodeFromFile {

    private static final Map<DecodeHintType, Object> HINTS;

    private static final Map<DecodeHintType, Object> HINTS_HARDER;

    static {
        Vector<BarcodeFormat> barcodeFormats = new Vector<>();
        barcodeFormats.add(BarcodeFormat.QR_CODE);

        HINTS = new EnumMap<>(DecodeHintType.class);
        HINTS.put(DecodeHintType.POSSIBLE_FORMATS, barcodeFormats);

        HINTS_HARDER = new EnumMap<>(HINTS);
        HINTS_HARDER.put(DecodeHintType.TRY_HARDER, Boolean.TRUE);
    }


    public static String scanQRImage(Context context, Uri uri) {
        //Check if external storage is accessible
        if (!Tools.isExternalStorageReadable()) {
            Toast.makeText(context, R.string.backup_toast_storage_not_accessible, Toast.LENGTH_LONG).show();
            return null;
        }
        //Get image in bytes
        byte[] imageInBytes;
        try {
            imageInBytes = StorageAccessHelper.loadFile(context, uri);
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(context, R.string.toast_file_load_error, Toast.LENGTH_LONG).show();
            return null;
        }

        Bitmap bMap = BitmapFactory.decodeByteArray(imageInBytes, 0, imageInBytes.length);
        String contents = null;
        int[] intArray = new int[bMap.getWidth() * bMap.getHeight()];

        bMap.getPixels(intArray, 0, bMap.getWidth(), 0, 0, bMap.getWidth(), bMap.getHeight());
        LuminanceSource source = new RGBLuminanceSource(bMap.getWidth(), bMap.getHeight(), intArray);
        BinaryBitmap bitmap = new BinaryBitmap(new HybridBinarizer(source));

        Result result = null;


        QRCodeReader reader = new QRCodeReader();
        ReaderException savedException = null;

        try {
            //Try finding QR code
            result = reader.decode(bitmap, HINTS);
            contents = result.getText();
        } catch (ReaderException re) {
            savedException = re;
        }

        if (contents == null) {
            try {
                //Try finding QR code really hard
                result = reader.decode(bitmap, HINTS_HARDER);
                contents = result.getText();
            } catch (ReaderException re) {
                savedException = re;
            }
        }

        if (contents == null) {
            try {
                throw savedException == null ? NotFoundException.getNotFoundInstance() : savedException;
            } catch (ChecksumException e) {
                e.printStackTrace();
                Toast.makeText(context, R.string.toast_qr_checksum_exception, Toast.LENGTH_LONG).show();
            } catch (FormatException e) {
                e.printStackTrace();
                Toast.makeText(context, R.string.toast_qr_format_error, Toast.LENGTH_LONG).show();
            } catch (ReaderException e) {  // Including NotFoundException
                e.printStackTrace();
                Toast.makeText(context, R.string.toast_qr_error, Toast.LENGTH_LONG).show();
            }
        }

        //Return QR code (if found)
        return contents;
    }
}
