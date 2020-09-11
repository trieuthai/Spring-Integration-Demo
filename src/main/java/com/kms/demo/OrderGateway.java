package com.kms.demo;

import com.kms.demo.entity.Order;
import org.springframework.integration.annotation.Gateway;
import org.springframework.integration.annotation.MessagingGateway;
import org.springframework.messaging.handler.annotation.Header;

@MessagingGateway
public interface OrderGateway {

    @Gateway(requestChannel = Channels.ORDER_INPUT_CHANNEL)
    public void process(Order msg, @Header(Channels.HEADER_SOURCE) String source);
}
