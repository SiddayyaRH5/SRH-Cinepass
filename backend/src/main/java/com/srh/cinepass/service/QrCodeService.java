package com.srh.cinepass.service;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;
import com.srh.cinepass.exception.TicketOperationException;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.EnumMap;
import java.util.Map;

@Service
public class QrCodeService {

    private final int qrSize;

    public QrCodeService(@Value("${app.tickets.qr-size:512}") int qrSize) {
        this.qrSize = Math.max(256, Math.min(qrSize, 1024));
    }

    /**
     * The payload is only the opaque verification token. No customer data,
     * booking identifier or show information is embedded in the QR code.
     */
    public byte[] generatePng(String verificationToken) {
        if (verificationToken == null || verificationToken.isBlank()) {
            throw new TicketOperationException("A verification token is required to generate a QR code");
        }

        Map<EncodeHintType, Object> hints = new EnumMap<>(EncodeHintType.class);
        hints.put(EncodeHintType.CHARACTER_SET, "UTF-8");
        hints.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.H);
        hints.put(EncodeHintType.MARGIN, 1);

        try {
            BitMatrix matrix = new QRCodeWriter().encode(
                    verificationToken,
                    BarcodeFormat.QR_CODE,
                    qrSize,
                    qrSize,
                    hints);

            ByteArrayOutputStream output = new ByteArrayOutputStream();
            MatrixToImageWriter.writeToStream(matrix, "PNG", output);
            return output.toByteArray();

        } catch (WriterException | IOException exception) {
            throw new TicketOperationException(
                    "The ticket QR code could not be generated",
                    exception);
        }
    }
}
