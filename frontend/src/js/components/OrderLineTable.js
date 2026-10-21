// components/OrderLineTable.js

import { formatPrice } from "../format.js";

// The lines of an order as a table, with the total at the bottom.
// Names and prices are the ones saved in the order, not today's catalog prices.
export function createOrderLineTable({ order }) {
    const element = document.createElement("table");

    element.className = "order-line-table";

    element.innerHTML = `
        <thead>
            <tr>
                <th scope="col">Product</th>
                <th scope="col" class="order-line-table__number">Quantity</th>
                <th scope="col" class="order-line-table__number">Price</th>
                <th scope="col" class="order-line-table__number">Total</th>
            </tr>
        </thead>
        <tbody></tbody>
        <tfoot>
            <tr>
                <th scope="row" colspan="3">Total</th>
                <td class="order-line-table__number order-line-table__total"></td>
            </tr>
        </tfoot>
      `;

    const body = element.querySelector("tbody");

    for (const line of order.lines) {
        const row = document.createElement("tr");

        row.innerHTML = `
            <td><a data-navigo></a></td>
            <td class="order-line-table__number"></td>
            <td class="order-line-table__number"></td>
            <td class="order-line-table__number"></td>
          `;

        const link = row.querySelector("a");
        link.setAttribute("href", `/products/${line.productId}`);
        link.textContent = line.productName;

        const [, quantity, price, total] = row.querySelectorAll("td");
        quantity.textContent = String(line.quantity);
        price.textContent = formatPrice(line.unitPrice);
        total.textContent = formatPrice(line.lineTotal);

        body.append(row);
    }

    element.querySelector(".order-line-table__total").textContent = formatPrice(order.total);

    return element;
}
