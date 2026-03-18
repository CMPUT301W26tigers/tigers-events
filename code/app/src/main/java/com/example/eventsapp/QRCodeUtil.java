package com.example.eventsapp;

import android.graphics.Bitmap;
import android.graphics.Color;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;

import java.util.HashMap;
import java.util.Map;

/**
 * Utility class for generating QR codes.
 * This class uses the ZXing library to create QR code Bitmaps from string content,
 * specifically for event deep links.
 */
public class QRCodeUtil {

    /**
     * Generates a QR code Bitmap for the given content.
     *
     * @param content The string to encode (e.g., a deep link like tigers-events://event/xyz).
     * @param width   The desired width of the generated Bitmap in pixels.
     * @param height  The desired height of the generated Bitmap in pixels.
     * @return A Bitmap of the QR code, or null if an error occurs during encoding.
     */
    public static Bitmap generateQRCode(String content, int width, int height) {
        try {
            Map<EncodeHintType, Object> hints = new HashMap<>();
            hints.put(EncodeHintType.CHARACTER_SET, "UTF-8");
            hints.put(EncodeHintType.MARGIN, 2);

            QRCodeWriter writer = new QRCodeWriter();
            BitMatrix bitMatrix = writer.encode(content, BarcodeFormat.QR_CODE, width, height, hints);

            Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565);
            for (int x = 0; x < width; x++) {
                for (int y = 0; y < height; y++) {
                    bitmap.setPixel(x, y, bitMatrix.get(x, y) ? Color.BLACK : Color.WHITE);
                }
            }
            return bitmap;
        } catch (WriterException e) {
            return null;
        }
    }
}
