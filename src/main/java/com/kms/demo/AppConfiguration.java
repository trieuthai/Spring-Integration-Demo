package com.kms.demo;

import com.kms.demo.entity.DeliveryOrder;
import com.kms.demo.entity.Order;
import com.kms.demo.entity.OrderItem;
import com.kms.demo.entity.PickupOrder;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.integration.annotation.*;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.config.EnableIntegration;
import org.springframework.integration.dsl.*;
import org.springframework.integration.file.DefaultFileNameGenerator;
import org.springframework.integration.file.FileWritingMessageHandler;
import org.springframework.integration.file.dsl.Files;
import org.springframework.integration.handler.LoggingHandler;
import org.springframework.integration.json.ObjectToJsonTransformer;
import org.springframework.integration.transaction.DefaultTransactionSynchronizationFactory;
import org.springframework.integration.transaction.ExpressionEvaluatingTransactionSynchronizationProcessor;
import org.springframework.integration.transaction.PseudoTransactionManager;
import org.springframework.integration.transaction.TransactionSynchronizationFactory;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;
import org.springframework.transaction.TransactionManager;

import java.io.File;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

import static com.kms.demo.Channels.*;

@Configuration
@ComponentScan("com.kms.demo")
@EnableIntegration
@IntegrationComponentScan("com.kms.demo")
public class AppConfiguration {

    protected final Log logger = LogFactory.getLog(AppConfiguration.class);

    public static final String DESTINATION_DELIVERY_DIR = "destination/delivery";
    public static final String DESTINATION_PICKUP_DIR = "destination/pickup";

    @Autowired
    private ApplicationContext context;

    @Bean
    public TransactionManager transactionManager(){
        return new PseudoTransactionManager();
    }

    @Bean(name = ORDER_INPUT_CHANNEL)
    public DirectChannel orderInputChannel() {
        return MessageChannels.direct(ORDER_INPUT_CHANNEL).get();
    }

    @Bean(name = ORDER_DELIVERY_CHANNEL)
    public DirectChannel orderDeliveryChannel() {
        return MessageChannels.direct(ORDER_DELIVERY_CHANNEL).get();
    }

    @Bean(name = ORDER_PICKUP_CHANNEL)
    public DirectChannel orderPickupChannel() {
        return MessageChannels.direct(ORDER_PICKUP_CHANNEL).get();
    }

    @Bean
    public IntegrationFlow orderFileReadingChain() {
        return IntegrationFlows
                .from(Files.inboundAdapter(new File(SOURCE_DIR))
                                .patternFilter("*.txt"),
                        e -> e.poller(Pollers.fixedDelay(10000)
                                .transactional(transactionManager())
                                .transactionSynchronizationFactory(transactionSynchronizationFactory())
                                .get()))
                .enrichHeaders(h -> h.header(HEADER_SOURCE, HEADER_SOURCE_FILE))
                .transform(Files.toStringTransformer())
                .transform(Transformers.fromJson(Order.class))
                .channel(ORDER_INPUT_CHANNEL)
                .get();
    }

    @Bean
    public IntegrationFlow preProcessOrderChain(){
        return IntegrationFlows.from(ORDER_INPUT_CHANNEL)
                .enrichHeaders(h -> h.headerExpression(HEADER_OUTPUT_FILE_NAME, "'order_' + payload.id + '.txt'")
                                        .headerExpression(HEADER_ORDER_TYPE, "payload.type")
                                        .headerExpression(HEADER_ORDER_ID, "payload.id"))
                .split(Order.class, Order::getItems)
                .channel(c -> c.executor(Executors.newCachedThreadPool()))
                .<OrderItem, Boolean>route(item -> item.getName().contains("book"),
                        mapping -> mapping
                                .subFlowMapping(true, sf -> sf.handle(this::processBook))
                                .subFlowMapping(false, sf -> sf.handle(this::processItem)))
                .aggregate(aggregatorSpec -> aggregatorSpec
                        .outputProcessor(group ->
                                new Order(group.getMessages()
                                .stream()
                                .map(message -> (OrderItem) message.getPayload())
                                .collect(Collectors.toList()))))
                .handle(this::postProcessOrderItems)
                .channel(ORDER_ROUTER_CHANNEL)
                .get();
    }

    private OrderItem processBook(OrderItem payload, Map<String, Object> headers){
        payload.setStatus("covered");
        return payload;
    }

    private OrderItem processItem(OrderItem payload, Map<String, Object> headers){
        payload.setStatus("processed");
        return payload;
    }

    private Order postProcessOrderItems(Order payload, Map<String, Object> headers){
        payload.setType((String)headers.get(HEADER_ORDER_TYPE));
        payload.setId((String)headers.get(HEADER_ORDER_ID));
        return payload;
    }


    @Router(inputChannel = ORDER_ROUTER_CHANNEL)
    public String orderRouter(Message<Order> message){
        String routeToChannel = ORDER_DELIVERY_CHANNEL;
        Order order = message.getPayload();
        if ("pickup".equals(order.getType())) {
            routeToChannel = ORDER_PICKUP_CHANNEL;
        }
        return routeToChannel;
    }

    @Transformer(inputChannel = ORDER_PICKUP_CHANNEL, outputChannel = ORDER_PICKUP_PROCESS_CHANNEL)
    public PickupOrder orderPickupTransformer(Order order){
        return new PickupOrder(order);
    }

    @ServiceActivator(inputChannel = ORDER_PICKUP_PROCESS_CHANNEL, outputChannel = ORDER_PICKUP_JSON_CHANNEL)
    public Message<PickupOrder> processOrderPickup(Message<PickupOrder> orderMessage){
        PickupOrder order = orderMessage.getPayload();
        logger.info("Processing Pickup Order: " + order.getId());
        order.setStore(PICK_UP_STORE);
        return orderMessage;
    }

    @Bean
    @Transformer(inputChannel = ORDER_PICKUP_JSON_CHANNEL, outputChannel = ORDER_SERIALIZE_CHANNEL)
    public ObjectToJsonTransformer orderPickupJsonTransformer(){
        return new ObjectToJsonTransformer();
    }

    @Bean
    @ServiceActivator(inputChannel = ORDER_SERIALIZE_CHANNEL)
    public FileWritingMessageHandler fileWriterAdapterOutbound() {
        FileWritingMessageHandler handler = new FileWritingMessageHandler(
                new File(DESTINATION_PICKUP_DIR)
        );
        handler.setExpectReply(false);
        handler.setFileNameGenerator(defaultFileNameGenerator());
        return handler;
    }

    @Bean
    public IntegrationFlow orderDeliveryChain() {
        return IntegrationFlows.from(ORDER_DELIVERY_CHANNEL)
                .transform(DeliveryOrder::new)
                .log(LoggingHandler.Level.INFO, AppConfiguration.class.getCanonicalName(),
                        m -> "Processing Delivery Order: " + ((DeliveryOrder)m.getPayload()).getId())
                .handle(DeliveryOrder.class, this::handleDeliveryOrder)
                .transform(Transformers.toJson())
                .handle(Files.outboundAdapter(
                        new File(DESTINATION_DELIVERY_DIR))
                            .fileNameGenerator(defaultFileNameGenerator()))
                .get();
    }

    private DeliveryOrder handleDeliveryOrder(DeliveryOrder order, MessageHeaders headers) {
        order.setFrom((String) headers.get(HEADER_SOURCE));
        return order;
    }

    @Bean
    public DefaultFileNameGenerator defaultFileNameGenerator() {
        DefaultFileNameGenerator defaultFileNameGenerator = new DefaultFileNameGenerator();
        defaultFileNameGenerator.setExpression("headers['" + HEADER_OUTPUT_FILE_NAME + "']");
        return defaultFileNameGenerator;
    }

    @Bean
    public TransactionSynchronizationFactory transactionSynchronizationFactory() {
        ExpressionParser parser = new SpelExpressionParser();
        ExpressionEvaluatingTransactionSynchronizationProcessor syncProcessor
                = new ExpressionEvaluatingTransactionSynchronizationProcessor();
        syncProcessor.setBeanFactory(context.getAutowireCapableBeanFactory());
        syncProcessor.setAfterCommitExpression(parser.parseExpression("payload.renameTo(new java.io.File('source/success/' + payload.name))"));
        return new DefaultTransactionSynchronizationFactory(syncProcessor);
    }

}
