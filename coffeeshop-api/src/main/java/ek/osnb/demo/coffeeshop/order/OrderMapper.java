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

    // For other features (through OrderApi)
    static OrderInfo toInfo(Order order) {
        List<OrderLineInfo> lines = new ArrayList<>();

        for (OrderLine line : order.getLines()) {
            lines.add(new OrderLineInfo(
                    line.getProductId(),
                    line.getProductName(),
                    line.getUnitPrice(),
                    line.getQuantity(),
                    line.lineTotal()
            ));
        }

        ShippingAddress address = order.getShippingAddress();
        String shipTo = address.getRecipientName() + ", " + address.getStreet() + ", "
                + address.getPostalCode() + " " + address.getCity();

        return new OrderInfo(
                order.getId(),
                order.getStatus().name(),
                order.getPlacedAt(),
                shipTo,
                lines,
                order.getTotal()
        );
    }

    static OrderSummaryInfo toSummaryInfo(Order order) {
        return new OrderSummaryInfo(
                order.getId(),
                order.getStatus().name(),
                order.getPlacedAt(),
                order.itemCount(),
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
