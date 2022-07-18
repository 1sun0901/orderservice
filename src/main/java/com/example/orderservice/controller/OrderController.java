package com.example.orderservice.controller;

import com.example.orderservice.dto.OrderDto;
import com.example.orderservice.jpa.OrderEntity;
import com.example.orderservice.messagequeue.KafkaProducer;
import com.example.orderservice.messagequeue.OrderProducer;
import com.example.orderservice.service.OrderService;
import com.example.orderservice.vo.RequestOrder;
import com.example.orderservice.vo.ResponseOrder;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.modelmapper.convention.MatchingStrategies;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("order-service")
@RequiredArgsConstructor
public class OrderController {
    private final Environment env;
    private final OrderService orderService;
    private final KafkaProducer kafkaProducer;
    private final OrderProducer orderProducer;

    @GetMapping("/health_check")
    public String status() {
        return String.format("It is working in Order Service on Port %s", env.getProperty("local.server.port"));
    }

    @PostMapping("/{userId}/orders")
    public ResponseEntity<ResponseOrder> createOrder(@PathVariable("userId") String userId, @RequestBody RequestOrder orderDetails) {
        ModelMapper mapper = new ModelMapper();
        mapper.getConfiguration().setMatchingStrategy(MatchingStrategies.STRICT);
        OrderDto orderDto = mapper.map(orderDetails, OrderDto.class);
        orderDto.setUserId(userId);
        /* jpa */
        OrderDto createOrderDto = orderService.createOrder(orderDto);

        ResponseOrder responseOrder = mapper.map(createOrderDto, ResponseOrder.class);

        /* kafka
        orderDto.setOrderId(UUID.randomUUID().toString());
        orderDto.setTotalPrice(orderDetails.getQty() * orderDetails.getUnitPrice());

        // send this order to the kafka 카프카 실행 안됨.ㅠㅠ
//        kafkaProducer.send("example-catalog-topic", orderDto);
//        orderProducer.send("orders", orderDto);
        ResponseOrder responseOrder = mapper.map(orderDto, ResponseOrder.class);
        */

        return ResponseEntity.status(HttpStatus.CREATED).body(responseOrder);
    }

    @GetMapping("/{userId}/orders")
    public ResponseEntity<List<ResponseOrder>> getOrder(@PathVariable("userId") String userId) {

        System.out.println("OrderController.getOrder<<<<<<<<<<<<<");

        Iterable<OrderEntity> orderList = orderService.getOrderByUserId(userId);

        System.out.println("userId<<<<<<<<<<<<<"+userId);
        System.out.println("orderList<<<<<<<<<<<<<"+orderList);
        orderList.forEach(System.out::println);

        ModelMapper mapper = new ModelMapper();

        List<ResponseOrder> result = new ArrayList<>();

        orderList.forEach(v -> {
            result.add(mapper.map(v,ResponseOrder.class));
        });

        return ResponseEntity.status(HttpStatus.OK).body(result);
    }
}
