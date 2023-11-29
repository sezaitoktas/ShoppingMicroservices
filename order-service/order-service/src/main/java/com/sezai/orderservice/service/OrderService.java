package com.sezai.orderservice.service;

import com.sezai.orderservice.dto.InventoryResponse;
import com.sezai.orderservice.dto.OrderLineItemsDto;
import com.sezai.orderservice.dto.OrderRequest;
import com.sezai.orderservice.model.Order;
import com.sezai.orderservice.model.OrderLineItems;
import com.sezai.orderservice.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class OrderService {
    private final OrderRepository orderRepository;
    private final WebClient webClient;
    public void placeOrder(OrderRequest orderRequest){
        Order order= new Order();
        order.setOrderNumber(UUID.randomUUID().toString());
        List<OrderLineItems> orderLineItems=orderRequest.getOrderLineItemsDtoList()
                .stream()
                .map(this::mapToDto).collect(Collectors.toList());
        order.setOrderLineItemsList(orderLineItems);
     List<String> skuCodes=  order.getOrderLineItemsList().stream().map((OrderLineItems::getSkuCode)).toList();
        //call inventory service and place order if product is in stock
     InventoryResponse[] inventoryResponsArray=   webClient.get().uri("http://inventory-service/api/inventory",
                     uriBuilder -> uriBuilder.queryParam("skuCode",skuCodes).build())
                        .retrieve()
                                .bodyToMono(InventoryResponse[].class)
                                        .block();
       boolean allProductsInStock= Arrays.stream(inventoryResponsArray).allMatch(InventoryResponse::isInInStock);
if(allProductsInStock){
    orderRepository.save(order);
}
else throw new IllegalArgumentException("Product is not in stock, please try again later");

    }
    private OrderLineItems mapToDto(OrderLineItemsDto orderLineItemsDto){
//        OrderLineItems orderLineItems= new OrderLineItems();
//        orderLineItems.setPrice(orderLineItemsDto.getPrice());
//        orderLineItems.setQuantity(orderLineItemsDto.getQuantity());
//        orderLineItems.setSkuCode(orderLineItemsDto.getSkuCode());
//        return orderLineItems;
        OrderLineItems orderLineItems=OrderLineItems.builder()
                .price(orderLineItemsDto.getPrice())
                .skuCode(orderLineItemsDto.getSkuCode())
                .quantity(orderLineItemsDto.getQuantity())
                .build();
        return orderLineItems;
    }
}
