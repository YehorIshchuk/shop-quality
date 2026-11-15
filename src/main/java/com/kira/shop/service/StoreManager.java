package com.kira.shop.service;

import com.kira.shop.domain.Address;
import com.kira.shop.domain.Category;
import com.kira.shop.domain.Customer;
import com.kira.shop.domain.CustomerTier;
import com.kira.shop.domain.Order;
import com.kira.shop.domain.OrderItem;
import com.kira.shop.domain.OrderStatus;
import com.kira.shop.domain.Product;
import com.kira.shop.dto.OrderDto;
import com.kira.shop.dto.OrderMapper;
import com.kira.shop.infrastructure.ConsoleNotificationSender;
import com.kira.shop.infrastructure.FakePaymentGateway;
import com.kira.shop.infrastructure.FileOrderRepository;
import com.kira.shop.infrastructure.InventoryStore;
import com.kira.shop.infrastructure.PaymentResult;
import com.kira.shop.infrastructure.ProductRepository;
import com.kira.shop.report.MonthlySalesReport;
import com.kira.shop.util.CsvUtils;
import com.kira.shop.util.MoneyUtils;

import java.math.BigDecimal;
import java.nio.file.Path;
import java.security.SecureRandom;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;

/**
 * Центральний сервіс магазину: каталог, склад, замовлення, оплата,
 * знижки, доставка, сповіщення та звітність.
 */
public class StoreManager {
    private static final BigDecimal FREE_SHIPPING_THRESHOLD = new BigDecimal("2000");
    private static final int LOW_STOCK_LEVEL = 3;
    private static final Map<String, BigDecimal> COUPON_RATES = Map.of(
            "WELCOME10", new BigDecimal("0.10"),
            "BLACKFRIDAY", new BigDecimal("0.25"));

    private final ProductRepository productRepository;
    private final InventoryStore inventory;
    private final FileOrderRepository orderRepository;
    private final FakePaymentGateway paymentGateway;
    private final ConsoleNotificationSender notificationSender;
    private final Map<String, String> transactions = new HashMap<>();
    private final Map<String, Integer> soldByProduct = new HashMap<>();
    private final SecureRandom random = new SecureRandom();
    private int orderSequence = 1;

    public StoreManager(ProductRepository productRepository, InventoryStore inventory,
                        FileOrderRepository orderRepository, FakePaymentGateway paymentGateway,
                        ConsoleNotificationSender notificationSender) {
        this.productRepository = productRepository;
        this.inventory = inventory;
        this.orderRepository = orderRepository;
        this.paymentGateway = paymentGateway;
        this.notificationSender = notificationSender;
    }

    // ------------------------------------------------------------ catalog & stock

    public void registerProduct(Product product, int initialStock) {
        productRepository.save(product);
        if (initialStock > 0) {
            inventory.add(product.getId(), initialStock);
        }
    }

    public void restock(String productId, int quantity) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new IllegalArgumentException("Unknown product " + productId));
        inventory.add(product.getId(), quantity);
    }

    public boolean isAvailable(String productId, int quantity) {
        return inventory.getStock(productId) >= quantity;
    }

    public List<String> lowStockReport() {
        List<String> lines = new ArrayList<>();
        for (Product product : productRepository.findAll()) {
            int stock = inventory.getStock(product.getId());
            if (stock <= LOW_STOCK_LEVEL) {
                lines.add(product.getName() + ": " + stock);
            }
        }
        return lines;
    }

    // ------------------------------------------------------------ ordering

    /**
     * Оформлює замовлення: перевіряє товари і залишки, резервує склад,
     * рахує знижку і доставку, зберігає та повідомляє клієнта.
     */
    public Order placeOrder(Customer customer, Map<String, Integer> cart) {
        if (customer != null) {
            if (cart != null && !cart.isEmpty()) {
                Order order = new Order("ORD-" + (orderSequence++), customer, LocalDate.now());
                for (Map.Entry<String, Integer> entry : cart.entrySet()) {
                    Optional<Product> product = productRepository.findById(entry.getKey());
                    if (product.isPresent()) {
                        if (entry.getValue() != null && entry.getValue() > 0) {
                            if (inventory.getStock(product.get().getId()) >= entry.getValue()) {
                                order.addItem(product.get(), entry.getValue());
                            } else {
                                if (customer.getTier() == CustomerTier.GOLD || customer.getTier() == CustomerTier.PLATINUM) {
                                    int available = inventory.getStock(product.get().getId());
                                    if (available > 0) {
                                        order.addItem(product.get(), available);
                                    } else {
                                        throw new IllegalStateException("Out of stock: " + entry.getKey());
                                    }
                                } else {
                                    throw new IllegalStateException("Not enough stock: " + entry.getKey());
                                }
                            }
                        } else {
                            throw new IllegalArgumentException("Bad quantity for " + entry.getKey());
                        }
                    } else {
                        throw new IllegalArgumentException("Unknown product " + entry.getKey());
                    }
                }
                for (OrderItem item : order.getItems()) {
                    inventory.reserve(item.getProduct().getId(), item.getQuantity());
                }
                order.applyDiscount(calculateDiscount(customer, order));
                order.setShippingCost(calculateShipping(customer.getAddress(), order));
                orderRepository.save(order);
                notificationSender.send(customer.getEmail(), "Order " + order.getId(),
                        "Total: " + MoneyUtils.format(order.total()));
                return order;
            } else {
                throw new IllegalArgumentException("Cart is empty");
            }
        } else {
            throw new IllegalArgumentException("Customer is required");
        }
    }

    public BigDecimal calculateDiscount(Customer customer, Order order) {
        BigDecimal subtotal = order.subtotal();
        int percent;
        switch (customer.getTier()) {
            case SILVER:
                percent = 3;
                break;
            case GOLD:
                percent = 5;
                break;
            case PLATINUM:
                percent = 10;
                break;
            default:
                percent = 0;
        }
        boolean hasBooks = order.getItems().stream()
                .anyMatch(i -> i.getProduct().getCategory() == Category.BOOKS);
        if (hasBooks && order.totalQuantity() >= 3) {
            percent += 2;
        }
        return MoneyUtils.percentOf(subtotal, percent);
    }

    public BigDecimal calculateShipping(Address address, Order order) {
        if (order.subtotal().compareTo(FREE_SHIPPING_THRESHOLD) >= 0 && address.isDomestic()) {
            return BigDecimal.ZERO;
        }
        BigDecimal base = address.isDomestic() ? new BigDecimal("70") : new BigDecimal("450");
        BigDecimal perKg = address.isDomestic() ? new BigDecimal("10") : new BigDecimal("120");
        return MoneyUtils.round(base.add(perKg.multiply(BigDecimal.valueOf(Math.ceil(order.totalWeight())))));
    }

    // ------------------------------------------------------------ payment & lifecycle

    public boolean payOrder(String orderId) {
        Order order = requireOrder(orderId);
        PaymentResult result = paymentGateway.charge(order.getCustomer().getId(), order.total());
        if (!result.isSuccess()) {
            notificationSender.send(order.getCustomer().getEmail(), "Payment failed", result.getMessage());
            return false;
        }
        order.markPaid();
        transactions.put(orderId, result.getTransactionId());
        for (OrderItem item : order.getItems()) {
            soldByProduct.merge(item.getProduct().getId(), item.getQuantity(), Integer::sum);
        }
        order.getCustomer().addLoyaltyPoints(order.total().intValue() / 100);
        notificationSender.send(order.getCustomer().getEmail(), "Payment received", result.getTransactionId());
        return true;
    }

    public void shipOrder(String orderId) {
        Order order = requireOrder(orderId);
        order.markShipped();
        notificationSender.send(order.getCustomer().getEmail(), "Order shipped",
                "To: " + order.getCustomer().getAddress());
    }

    public void cancelOrder(String orderId) {
        Order order = requireOrder(orderId);
        boolean wasPaid = order.getStatus() == OrderStatus.PAID;
        order.cancel();
        for (OrderItem item : order.getItems()) {
            inventory.release(item.getProduct().getId(), item.getQuantity());
        }
        if (wasPaid) {
            paymentGateway.refund(transactions.remove(orderId));
        }
        notificationSender.send(order.getCustomer().getEmail(), "Order cancelled", orderId);
    }

    // ------------------------------------------------------------ analytics & reports

    /** Формує текстове резюме клієнта з усіма його замовленнями та рекомендацією рівня. */
    public String buildCustomerSummary(Customer customer) {
        return summarize(customer, order -> true, "");
    }

    /** Те саме резюме, але лише за обраний місяць (для щомісячної розсилки). */
    public String buildCustomerSummaryForMonth(Customer customer, YearMonth month) {
        return summarize(customer, order -> YearMonth.from(order.getCreatedAt()).equals(month), " (" + month + ")");
    }

    private String summarize(Customer customer, Predicate<Order> filter, String label) {
        StringBuilder sb = new StringBuilder();
        BigDecimal spent = BigDecimal.ZERO;
        int paid = 0;
        int cancelled = 0;
        for (Order order : orderRepository.findAll()) {
            if (order.getCustomer().getId().equals(customer.getId()) && filter.test(order)) {
                if (order.getStatus() == OrderStatus.CANCELLED) {
                    cancelled++;
                } else {
                    if (order.getStatus() != OrderStatus.NEW) {
                        paid++;
                        spent = spent.add(order.total());
                        for (OrderItem item : order.getItems()) {
                            if (item.getProduct().getCategory() == Category.ELECTRONICS) {
                                if (item.lineTotal().compareTo(new BigDecimal("10000")) > 0) {
                                    sb.append("Premium purchase: ").append(item.getProduct().getName()).append('\n');
                                }
                            }
                        }
                    }
                }
            }
        }
        sb.append("Customer: ").append(customer.getName()).append(label).append('\n');
        sb.append("Paid orders: ").append(paid).append(", cancelled: ").append(cancelled).append('\n');
        sb.append("Spent: ").append(MoneyUtils.format(spent)).append('\n');
        if (spent.compareTo(new BigDecimal("50000")) > 0) {
            if (customer.getTier() != CustomerTier.PLATINUM) {
                sb.append("Recommendation: upgrade to PLATINUM");
            } else {
                sb.append("Recommendation: keep PLATINUM");
            }
        } else if (spent.compareTo(new BigDecimal("20000")) > 0) {
            if (customer.getTier() == CustomerTier.REGULAR || customer.getTier() == CustomerTier.SILVER) {
                sb.append("Recommendation: upgrade to GOLD");
            } else {
                sb.append("Recommendation: keep ").append(customer.getTier());
            }
        } else {
            sb.append("Recommendation: keep ").append(customer.getTier());
        }
        return sb.toString();
    }

    // ------------------------------------------------------------ coupons

    public BigDecimal applyCoupon(String orderId, String code) {
        Order order = requireOrder(orderId);
        BigDecimal rate = COUPON_RATES.getOrDefault(code, BigDecimal.ZERO);
        BigDecimal extra = MoneyUtils.round(order.subtotal().multiply(rate));
        order.applyDiscount(order.getDiscount().add(extra));
        return extra;
    }

    public String generateCouponCode(Customer customer) {
        return customer.getId() + "-" + (100000 + random.nextInt(900000));
    }

    public Map<String, Integer> topProducts(int limit) {
        Map<String, Integer> result = new LinkedHashMap<>();
        soldByProduct.entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                .limit(limit)
                .forEach(e -> result.put(e.getKey(), e.getValue()));
        return result;
    }

    public String monthlyReport(YearMonth month) {
        MonthlySalesReport report = new MonthlySalesReport(month);
        for (Order order : orderRepository.findAll()) {
            if (order.getStatus() != OrderStatus.NEW) {
                report.addSale(order);
            }
        }
        return report.render();
    }

    public List<String> exportOrderLines() {
        List<String> lines = new ArrayList<>();
        for (Order order : orderRepository.findAll()) {
            OrderDto dto = OrderMapper.toDto(order);
            lines.add(CsvUtils.join(dto.getOrderId(), dto.getCustomerName(), dto.getStatus()));
        }
        return lines;
    }

    public void exportOrders(Path file) {
        orderRepository.exportCsv(file);
    }

    public void sendPromo(List<Customer> customers, Category category) {
        List<Product> products = productRepository.findByCategory(category);
        if (products.isEmpty()) {
            return;
        }
        for (Customer customer : customers) {
            notificationSender.send(customer.getEmail(), "New in " + category,
                    products.size() + " products waiting for you");
        }
    }

    private Order requireOrder(String orderId) {
        return orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Unknown order " + orderId));
    }
}
