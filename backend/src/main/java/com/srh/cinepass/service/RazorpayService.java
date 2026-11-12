
package com.srh.cinepass.service;

import com.razorpay.Order;
import com.razorpay.RazorpayClient;
import com.razorpay.RazorpayException;
import com.razorpay.Utils;

import org.json.JSONObject;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class RazorpayService {

    @Value("${razorpay.key-id:}")
    private String keyId;

    @Value("${razorpay.key-secret:}")
    private String keySecret;

    private RazorpayClient getClient() throws RazorpayException {

        if (keyId == null || keyId.isBlank()
                || keySecret == null || keySecret.isBlank()) {

            throw new IllegalStateException(
                    "Razorpay credentials are not configured");
        }

        return new RazorpayClient(keyId, keySecret);
    }

    // Create a Razorpay order. Amount must be in paise.
    public JSONObject createOrder(
            int amountInPaise,
            String receipt) throws RazorpayException {

        if (amountInPaise <= 0) {
            throw new IllegalArgumentException(
                    "Payment amount must be greater than zero");
        }

        JSONObject orderRequest = new JSONObject();

        orderRequest.put("amount", amountInPaise);
        orderRequest.put("currency", "INR");
        orderRequest.put("receipt", receipt);

        Order order = getClient().orders.create(orderRequest);

        return order.toJson();
    }

    // Verify Razorpay's payment signature on the backend.
    public void verifyPaymentSignature(
            String orderId,
            String paymentId,
            String signature) throws RazorpayException {

        if (orderId == null || orderId.isBlank()
                || paymentId == null || paymentId.isBlank()
                || signature == null || signature.isBlank()) {

            throw new IllegalArgumentException(
                    "Payment verification details are incomplete");
        }

        JSONObject verificationData = new JSONObject();

        verificationData.put("razorpay_order_id", orderId);
        verificationData.put("razorpay_payment_id", paymentId);
        verificationData.put("razorpay_signature", signature);

        Utils.verifyPaymentSignature(
                verificationData,
                keySecret);
    }

    public void verifyPaymentCaptured(String paymentId)
            throws RazorpayException {

        com.razorpay.Payment payment = getClient().payments.fetch(paymentId);

        String status = payment.get("status");

        if (!"captured".equalsIgnoreCase(status)) {
            throw new IllegalStateException(
                    "Payment has not been captured. Current status: " + status);
        }
    }

    // Only the public Key ID may be sent to the frontend.
    public String getKeyId() {
        return keyId;
    }
}
