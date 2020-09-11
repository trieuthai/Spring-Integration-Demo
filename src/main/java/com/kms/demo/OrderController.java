package com.kms.demo;


import com.kms.demo.entity.Order;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;


@RestController
@RequestMapping("/order")
public class OrderController {

    @Autowired
    private ApplicationContext applicationContext;

    @RequestMapping(value = "/check", method = RequestMethod.GET)
    public String check() {
        return "Service is available";
    }

    @RequestMapping(method = RequestMethod.POST)
    public String postOrder(@RequestBody Order order) {
        OrderGateway orderGateway = applicationContext.getBean(OrderGateway.class);
        orderGateway.process(order, Channels.HEADER_SOURCE_GATEWAY);
        return "Sent Order ";
    }
}
