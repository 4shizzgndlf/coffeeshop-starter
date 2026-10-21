package ek.osnb.demo.coffeeshop.order;

import java.util.ArrayList;
import java.util.List;

final class OrderMapper {

    private OrderMapper() {
    }

    static OrderResponse toResponse(Order order) {
        List<OrderLineResponse> lines = new ArrayList<>();

        for (OrderLine line : order.getLines()) {
            lines.add(new OrderLineResponse(
                    line.getProductId(),
                    line.getProductName(),
                    line.getUnitPrice(),
                    line.getQuantity(),
                    line.lineTotal()
            ));
        }

        ShippingAddress address = order.getShippingAddress();

        return new OrderResponse(
                order.getId(),
                order.getCustomerId(),
                order.getStatus(),
                order.getPlacedAt(),
                new ShippingAddressResponse(
                        address.getRecipientName(),
                        address.getStreet(),
                        address.getPostalCode(),
                        address.getCity()
                ),
                lines,
                order.getTotal()
        );
    }

    static OrderSummaryResponse toSummary(Order order) {
        return new OrderSummaryResponse(
                order.getId(),
                order.getStatus(),
                order.getPlacedAt(),
                order.itemCount(),
                order.getTotal()
        );
    }
}
