package com.kuralpay.app.paymentdetection

import com.kuralpay.app.domain.model.ParsedPayment
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

object PaymentEventBus {
    private val _events = MutableSharedFlow<ParsedPayment>(replay = 0, extraBufferCapacity = 64)
    val events: SharedFlow<ParsedPayment> = _events.asSharedFlow()

    suspend fun emit(payment: ParsedPayment) {
        _events.emit(payment)
    }
}
