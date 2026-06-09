package com.yxshop.Module.Payment.Controller;

import com.yxshop.Module.Payment.Service.PaymentRecordService;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/app/payments")
public class PaymentRecordController {
    private final PaymentRecordService paymentRecordService;

    public PaymentRecordController(PaymentRecordService paymentRecordService) {
        this.paymentRecordService = paymentRecordService;
    }
}
