package dat.com.orderservice.domain.model;


import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.Objects;
import java.util.UUID;


public class Order {

    private UUID id;


    private Long productId;


    private String productName;

    private BigDecimal price;


    private Integer quantity;

    private OrderStatus status;

    private Long version;

    public Order(UUID id, Long productId, String productName, BigDecimal price, Integer quantity, OrderStatus status,Long version) {
        this.id = id;
        this.productId = productId;
        this.productName = productName;
        this.price = price;
        this.quantity = quantity;
        this.status = status;
        this.version= version;
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
        if(price == null || price.signum() < 0){
            throw new IllegalArgumentException("Giá sản phẩm không hợp lệ");
        }
        if(quantity == null || quantity <= 0){
            throw new IllegalArgumentException("Số lượng k hợp le, phải >0");
        }

        return new Order(
                null,productId,productName,price,quantity,OrderStatus.PENDING,null
        );
    }

    //Khôi phục order đc lưu  DB
    public static Order rehydrate(
            UUID id,Long productId,String productName,BigDecimal price,int quantity,OrderStatus status,Long version
    ){
        if(id==null){
            throw new IllegalArgumentException("id không hợp lệ");
        }

        Objects.requireNonNull(status,"status là bắt buộc");
        Order order = create(productId,productName,price,quantity);

        return new Order(id, order.productId,order.productName,order.price,order.quantity,status,version);
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

    //ghi nhận ý định hủy

    public  boolean requestCancellation(){
        if(status==OrderStatus.CANCELLING || status == OrderStatus.CANCELLED){
            return false;
        }

        if(status != OrderStatus.PENDING && status != OrderStatus.CONFIRM){
            throw new IllegalArgumentException("Không thể hủy đơn hàng đang ở trạng thái: "+status);
        }

        status=OrderStatus.CANCELLING;

        return true;
    }

    //hoàn tất hủy sau khi inventory xác nhận
    //reservation đc giải phóng hoặc k thể giữ hàng nx
    public  boolean completeCancelaion(){
        if(status == OrderStatus.CANCELLED){
            return false;
        }

        if(status!=OrderStatus.CANCELLING){
            throw new IllegalArgumentException("đơn hàng trạng thái cancelling mơới  đc phép hủy");
        }

        status=OrderStatus.CANCELLED;
        return true;
    }


    public boolean rejectStock() {

        // Đơn đã hủy: không thay đổi nữa.
        if (status == OrderStatus.CANCELLED) {
            return false;
        }

        // Đã yêu cầu giải phóng kho:
        // tiếp tục chờ stock.released để hoàn tất hủy.
        if (status == OrderStatus.CANCELLING) {
            return false;
        }

        // Chỉ đơn đang chờ giữ kho mới được hủy vì hết hàng.
        if (status != OrderStatus.PENDING) {
            throw new IllegalStateException(
                    "Không thể từ chối giữ kho cho Order ở trạng thái: "
                            + status
            );
        }

        status = OrderStatus.CANCELLED;
        return true;
    }

    public UUID getId() {
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

    public Long getVersion() {
        return version;
    }

    public OrderStatus getStatus() {
        return status;
    }

    public BigDecimal getTotalAmount(){
        return price.multiply(BigDecimal.valueOf(quantity));
    }
}

