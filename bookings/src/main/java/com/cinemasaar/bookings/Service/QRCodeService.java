package com.cinemasaar.bookings.Service;

import com.cinemasaar.bookings.Exception.ApiException;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Base64;

@Service
public class QRCodeService {

    public byte[] toPngBytes(String text) {
        try {
            BitMatrix matrix = new QRCodeWriter().encode(text, BarcodeFormat.QR_CODE, 300, 300);
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            MatrixToImageWriter.writeToStream(matrix, "PNG", out);
            return out.toByteArray();
        } catch (WriterException | IOException e) {
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "Could not generate QR code");
        }
    }

    public String toPngDataUrl(String text) {
        return "data:image/png;base64," + Base64.getEncoder().encodeToString(toPngBytes(text));
    }
}