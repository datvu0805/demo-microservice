package dat.com.orderservice.domain.model;


import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.Objects;


public class Order {

    private Long id;


    private Long productId;


    private String productName;

    private BigDecimal price;


    private Integer quantity;

    private OrderStatus status;

    public Order(Long id, Long productId, String productName, BigDecimal price, Integer quantity, OrderStatus status) {
        this.id = id;
        this.productId = productId;
        this.productName = productName;
        this.price = price;
        this.quantity = quantity;
        this.status = status;
    }
    // factory method tạo đơn hàng
    public static Order create(Long productId,String productName,
                               BigDecimal price,Integer quantity){
        if(productId == null || productId<=0){
            throw new IllegalArgumentException("ProductId khôn ghợp lệ");
        }
        if(productName == null || productName.isBlank()){
            throw new IllegalArgumentException("Tên sản phân k hợp lệ");
        }
        if(quantity<0){
            throw new IllegalArgumentException("Số lượng k hợp le, phải >0");
        }

        return new Order(
                null,productId,productName,price,quantity,OrderStatus.PENDING
        );
    }

    //Khôi phục order đc lưu  DB
    public static Order rehydrate(
            Long id,Long productId,String productName,BigDecimal price,int quantity,OrderStatus status
    ){
        if(id==null || id <=0){
            throw new IllegalArgumentException("id không hợp lệ");
        }

        Objects.requireNonNull(status,"status là bắt buộc");
        Order order = create(productId,productName,price,quantity);

        return new Order(id, order.productId,order.productName,order.price,order.quantity,status);
    }

    //xác nhận đơn hàng(Business Behavior)
    public void confirm(){
        if(status != OrderStatus.PENDING){
            throw new IllegalArgumentException("Chỉ đơn hàng ở trạng thái PENDING mới đc confirm");
        }

        this.status=OrderStatus.CONFIRM;
    }

    //Hủy đơn(Business Behavior)
    public void cancel(){
        if(status != OrderStatus.PENDING){
            throw new IllegalArgumentException("Chỉ đơn hàng ở trạng thái PENDING mới đc confirm");
        }

        this.status=OrderStatus.CANCELLED;
    }

    public Long getId() {
        return id;
    }

    public Long getProductId() {
        return productId;
    }

    public String getProductName() {
        return productName;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public OrderStatus getStatus() {
        return status;
    }

    public BigDecimal getTotalAmount(){
        return price.multiply(BigDecimal.valueOf(quantity));
    }
}

