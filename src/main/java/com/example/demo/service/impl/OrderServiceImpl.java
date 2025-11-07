package com.example.demo.service.impl;

import com.example.demo.dao.OrderDAO;
import com.example.demo.dao.ProductDAO;
import com.example.demo.model.*;
import com.example.demo.service.AddressService;
import com.example.demo.service.OrderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

@Service
@Transactional
public class OrderServiceImpl implements OrderService {

    private final OrderDAO orderDAO;
    private final ProductDAO productDAO;
    private final AddressService addressService;

    @Autowired
    public OrderServiceImpl(OrderDAO orderDAO, ProductDAO productDAO, AddressService addressService) {
        this.orderDAO = orderDAO;
        this.productDAO = productDAO;
        this.addressService = addressService;
    }

    @Override
    public Order createOrder(Cart cart, User user, PaymentMethod paymentMethod) {
        if (cart == null || cart.isEmpty()) {
            throw new IllegalArgumentException("Cart must contain at least one item to create an order");
        }
        if (!cart.hasSelectedItems()) {
            throw new IllegalArgumentException("Please select at least one item to checkout");
        }
        if (user == null) {
            throw new IllegalArgumentException("User must not be null when creating an order");
        }
        if (paymentMethod == null) {
            throw new IllegalArgumentException("Payment method must be provided");
        }

        Order order = new Order();
        order.setUser(user);
        order.setStatus(OrderStatus.PENDING_PAYMENT);
        order.setPaymentMethod(paymentMethod);
        order.setCreatedAt(LocalDateTime.now());

        // 保存收件地址信息
        Long selectedAddressId = cart.getSelectedAddressId();
        if (selectedAddressId != null) {
            Address address = addressService.getById(selectedAddressId);
            if (address != null && address.getUser() != null && address.getUser().getId().equals(user.getId())) {
                order.setRecipientName(address.getRecipientName());
                order.setRecipientPhone(address.getPhoneNumber());
                // 組合完整地址
                StringBuilder fullAddress = new StringBuilder();
                fullAddress.append(address.getAddressLine1());
                if (address.getAddressLine2() != null && !address.getAddressLine2().trim().isEmpty()) {
                    fullAddress.append(", ").append(address.getAddressLine2());
                }
                fullAddress.append(", ").append(address.getCity());
                fullAddress.append(", ").append(address.getState());
                fullAddress.append(" ").append(address.getPostalCode());
                order.setRecipientAddress(fullAddress.toString());
            }
        }

        // 檢查庫存並驗證訂單商品
        validateAndReserveStock(cart);

        BigDecimal total = BigDecimal.ZERO;
        boolean hasOrderItems = false;
        for (CartItem cartItem : cart.getSelectedItems()) {
            Product product = productDAO.findById(cartItem.getProductId());
            if (product == null) {
                throw new IllegalArgumentException("Product not found for id: " + cartItem.getProductId());
            }
            if (cartItem.getQuantity() <= 0) {
                continue;
            }
            OrderItem orderItem = new OrderItem();
            orderItem.setProduct(product);
            orderItem.setQuantity(cartItem.getQuantity());
            orderItem.setUnitPrice(cartItem.getUnitPrice());
            order.addItem(orderItem);
            total = total.add(orderItem.getSubtotal());
            hasOrderItems = true;
        }
        order.setTotalPrice(total);

        if (!hasOrderItems) {
            throw new IllegalArgumentException("Cart must contain selected items with quantity greater than zero");
        }

        orderDAO.save(order);

        // 扣除庫存
        deductStock(cart);

        return order;
    }

    @Override
    public Order createOrder(Cart cart, User user, PaymentMethod paymentMethod, DeliveryMethod deliveryMethod,
                             String recipientName, String recipientPhone, String recipientEmail, String recipientAddress) {
        if (cart == null || cart.isEmpty()) {
            throw new IllegalArgumentException("Cart must contain at least one item to create an order");
        }
        if (!cart.hasSelectedItems()) {
            throw new IllegalArgumentException("Please select at least one item to checkout");
        }
        if (user == null) {
            throw new IllegalArgumentException("User must not be null when creating an order");
        }
        if (paymentMethod == null) {
            throw new IllegalArgumentException("Payment method must be provided");
        }
        if (deliveryMethod == null) {
            throw new IllegalArgumentException("Delivery method must be provided");
        }

        Order order = new Order();
        order.setUser(user);
        order.setStatus(OrderStatus.PENDING_PAYMENT);
        order.setPaymentMethod(paymentMethod);
        order.setDeliveryMethod(deliveryMethod);
        order.setRecipientName(recipientName);
        order.setRecipientPhone(recipientPhone);
        order.setRecipientEmail(recipientEmail);
        order.setRecipientAddress(recipientAddress);
        order.setCreatedAt(LocalDateTime.now());

        // 檢查庫存並驗證訂單商品
        validateAndReserveStock(cart);

        BigDecimal total = BigDecimal.ZERO;
        boolean hasOrderItems = false;
        for (CartItem cartItem : cart.getSelectedItems()) {
            Product product = productDAO.findById(cartItem.getProductId());
            if (product == null) {
                throw new IllegalArgumentException("Product not found for id: " + cartItem.getProductId());
            }
            if (cartItem.getQuantity() <= 0) {
                continue;
            }
            OrderItem orderItem = new OrderItem();
            orderItem.setProduct(product);
            orderItem.setQuantity(cartItem.getQuantity());
            orderItem.setUnitPrice(cartItem.getUnitPrice());
            order.addItem(orderItem);
            total = total.add(orderItem.getSubtotal());
            hasOrderItems = true;
        }
        order.setTotalPrice(total);

        if (!hasOrderItems) {
            throw new IllegalArgumentException("Cart must contain selected items with quantity greater than zero");
        }

        orderDAO.save(order);

        // 扣除庫存
        deductStock(cart);

        return order;
    }

    @Override
    public Order createOrder(Cart cart, User user, DeliveryPaymentMethod deliveryPaymentMethod,
                             String recipientName, String recipientPhone, String recipientEmail, String recipientAddress) {
        if (cart == null || cart.isEmpty()) {
            throw new IllegalArgumentException("Cart must contain at least one item to create an order");
        }
        if (!cart.hasSelectedItems()) {
            throw new IllegalArgumentException("Please select at least one item to checkout");
        }
        if (user == null) {
            throw new IllegalArgumentException("User must not be null when creating an order");
        }
        if (deliveryPaymentMethod == null) {
            throw new IllegalArgumentException("Delivery payment method must be provided");
        }

        Order order = new Order();
        order.setUser(user);
        order.setStatus(OrderStatus.PENDING_PAYMENT);
        order.setDeliveryPaymentMethod(deliveryPaymentMethod);
        
        // 根據配送付款方式設置對應的paymentMethod和deliveryMethod（用於兼容舊代碼）
        if (deliveryPaymentMethod == DeliveryPaymentMethod.CASH_ON_DELIVERY) {
            order.setPaymentMethod(PaymentMethod.CASH_ON_DELIVERY);
            order.setDeliveryMethod(DeliveryMethod.CASH_ON_DELIVERY);
        } else if (deliveryPaymentMethod == DeliveryPaymentMethod.PICKUP_CASH) {
            order.setPaymentMethod(PaymentMethod.PICKUP);
            order.setDeliveryMethod(DeliveryMethod.PICKUP);
        }
        
        order.setRecipientName(recipientName);
        order.setRecipientPhone(recipientPhone);
        order.setRecipientEmail(recipientEmail);
        order.setRecipientAddress(recipientAddress);
        order.setCreatedAt(LocalDateTime.now());

        // 檢查庫存並驗證訂單商品
        validateAndReserveStock(cart);

        BigDecimal total = BigDecimal.ZERO;
        boolean hasOrderItems = false;
        for (CartItem cartItem : cart.getSelectedItems()) {
            Product product = productDAO.findById(cartItem.getProductId());
            if (product == null) {
                throw new IllegalArgumentException("Product not found for id: " + cartItem.getProductId());
            }
            if (cartItem.getQuantity() <= 0) {
                continue;
            }
            OrderItem orderItem = new OrderItem();
            orderItem.setProduct(product);
            orderItem.setQuantity(cartItem.getQuantity());
            orderItem.setUnitPrice(cartItem.getUnitPrice());
            order.addItem(orderItem);
            total = total.add(orderItem.getSubtotal());
            hasOrderItems = true;
        }
        order.setTotalPrice(total);

        if (!hasOrderItems) {
            throw new IllegalArgumentException("Cart must contain selected items with quantity greater than zero");
        }

        orderDAO.save(order);

        // 扣除庫存
        deductStock(cart);

        return order;
    }

    @Override
    @Transactional(readOnly = true)
    public Order getOrderById(Long id) {
        if (id == null) {
            return null;
        }
        return orderDAO.findById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Order> listOrdersByUser(User user) {
        if (user == null) {
            return List.of();
        }
        return orderDAO.findByUser(user);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Order> getAllOrders() {
        return orderDAO.findAll();
    }

    @Override
    public Order updateStatus(Long orderId, OrderStatus newStatus) {
        Objects.requireNonNull(newStatus, "New status must not be null");
        if (orderId == null) {
            throw new IllegalArgumentException("Order id must not be null");
        }
        Order order = orderDAO.findById(orderId);
        if (order == null) {
            throw new IllegalArgumentException("Order not found for id: " + orderId);
        }
        OrderStatus currentStatus = order.getStatus();
        if (!isValidTransition(currentStatus, newStatus)) {
            throw new IllegalStateException("Invalid status transition from " + currentStatus + " to " + newStatus);
        }
        order.setStatus(newStatus);
        orderDAO.update(order);
        return order;
    }

    private boolean isValidTransition(OrderStatus currentStatus, OrderStatus nextStatus) {
        if (currentStatus == null) {
            return nextStatus == OrderStatus.PENDING_PAYMENT;
        }
        if (currentStatus == nextStatus) {
            return true;
        }
        switch (currentStatus) {
            case PENDING_PAYMENT:
                // 待付款之後可以進入：已付款（舊流程相容）/待確認/待處理
                return nextStatus == OrderStatus.PAID
                        || nextStatus == OrderStatus.PENDING_CONFIRMATION
                        || nextStatus == OrderStatus.PROCESSING;
            case PAID:
                // 已付款（相容舊流程）後可進入待出貨或已出貨
                return nextStatus == OrderStatus.PENDING_SHIPMENT
                        || nextStatus == OrderStatus.SHIPPED;
            case PENDING_CONFIRMATION:
                // 待確認後可進入待處理或待出貨
                return nextStatus == OrderStatus.PROCESSING
                        || nextStatus == OrderStatus.PENDING_SHIPMENT;
            case PROCESSING:
                // 待處理後可進入待出貨或已出貨
                return nextStatus == OrderStatus.PENDING_SHIPMENT
                        || nextStatus == OrderStatus.SHIPPED;
            case PENDING_SHIPMENT:
                // 待出貨後可進入已出貨
                return nextStatus == OrderStatus.SHIPPED;
            case SHIPPED:
                // 已出貨不可再往後
                return nextStatus == OrderStatus.SHIPPED;
            default:
                return false;
        }
    }

    /**
     * 檢查庫存是否足夠，如果不足則拋出異常
     */
    private void validateAndReserveStock(Cart cart) {
        for (CartItem cartItem : cart.getSelectedItems()) {
            if (cartItem.getQuantity() <= 0) {
                continue;
            }
            Product product = productDAO.findById(cartItem.getProductId());
            if (product == null) {
                throw new IllegalArgumentException("找不到商品 ID: " + cartItem.getProductId());
            }
            int requestedQuantity = cartItem.getQuantity();
            int availableStock = product.getStock() == null ? 0 : product.getStock();
            if (requestedQuantity > availableStock) {
                throw new IllegalArgumentException(
                    String.format("庫存不足，無法成立訂單。商品「%s」目前庫存：%d，訂購數量：%d",
                        product.getName(), availableStock, requestedQuantity)
                );
            }
        }
    }

    /**
     * 扣除庫存
     */
    private void deductStock(Cart cart) {
        for (CartItem cartItem : cart.getSelectedItems()) {
            if (cartItem.getQuantity() <= 0) {
                continue;
            }
            Product product = productDAO.findById(cartItem.getProductId());
            if (product != null) {
                int currentStock = product.getStock() == null ? 0 : product.getStock();
                int newStock = currentStock - cartItem.getQuantity();
                product.setStock(Math.max(0, newStock));
                productDAO.save(product);
            }
        }
    }
}
