package com.example.eventsapp;

import android.graphics.Bitmap;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import com.google.zxing.BinaryBitmap;
import com.google.zxing.MultiFormatReader;
import com.google.zxing.RGBLuminanceSource;
import com.google.zxing.Result;
import com.google.zxing.common.HybridBinarizer;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * Instrumented test for QRCodeUtil.
 * Verifies that QR codes are generated correctly and contain the expected data.
 */
@RunWith(AndroidJUnit4.class)
public class QRCodeUtilTest {

    @Test
    public void testGenerateQRCodeContent() throws Exception {
        String testContent = "tigers-events://event/test_id_123";
        int size = 512;
        
        Bitmap bitmap = QRCodeUtil.generateQRCode(testContent, size, size);
        
        assertNotNull("Generated Bitmap should not be null", bitmap);
        assertEquals("Width should be 512", size, bitmap.getWidth());
        assertEquals("Height should be 512", size, bitmap.getHeight());
        
        // Decode the bitmap to verify its content
        String decodedContent = decodeQRCode(bitmap);
        assertEquals("Decoded content should match the input content", testContent, decodedContent);
    }

    @Test
    public void testEventLinkQRGeneration() throws Exception {
        // Create an event and get its deep link
        Event event = new Event("Test Event", 100);
        event.setId("event_xyz_789");
        String expectedLink = "tigers-events://event/event_xyz_789";
        
        assertEquals("Event deep link should follow the expected format", expectedLink, event.getEventDeepLink());
        
        // Generate QR code from the event's deep link
        Bitmap bitmap = QRCodeUtil.generateQRCode(event.getEventDeepLink(), 400, 400);
        assertNotNull(bitmap);
        
        String decodedContent = decodeQRCode(bitmap);
        assertEquals("QR code should contain the event's deep link", expectedLink, decodedContent);
    }

    /**
     * Helper method to decode a QR code Bitmap using ZXing.
     */
    private String decodeQRCode(Bitmap bitmap) throws Exception {
        int[] intArray = new int[bitmap.getWidth() * bitmap.getHeight()];
        bitmap.getPixels(intArray, 0, bitmap.getWidth(), 0, 0, bitmap.getWidth(), bitmap.getHeight());
        
        RGBLuminanceSource source = new RGBLuminanceSource(bitmap.getWidth(), bitmap.getHeight(), intArray);
        BinaryBitmap binaryBitmap = new BinaryBitmap(new HybridBinarizer(source));
        
        Result result = new MultiFormatReader().decode(binaryBitmap);
        return result.getText();
    }
}
