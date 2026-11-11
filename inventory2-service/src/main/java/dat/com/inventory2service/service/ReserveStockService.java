package dat.com.inventory2service.service;

import dat.com.inventory2service.entity.InventoryReservation;
import dat.com.inventory2service.entity.ReservationStatus;
import dat.com.inventory2service.repository.InventoryRepository;
import dat.com.inventory2service.repository.InventoryReservationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ReserveStockService {
    private final InventoryRepository inventoryRepository;
    private final InventoryReservationRepository inventoryReservationRepository;
    private final JdbcTemplate jdbcTemplate;

    @Transactional
    public ReservationStatus reserve(Long orderId,Long productId,int quantity){

        //1. validate request
        if(orderId == null || orderId<=0||
                productId==null||productId<=0
                ||quantity<=0){
            throw new IllegalArgumentException("Request không hợp lệ , do id hoặc quantity");
        }

        //2. claim(xác định transaction nào được quyền tạo reservation cho một orderId )

        //inserted trả về 1 nếu thành công , về 0 nếu như xung đột với orderId
        //bước đầu idempotece
        int inserted = jdbcTemplate.update("""
                INSERT INTO  inventory_reservations(order_id, product_id, quantity, status)
                VALUES (?,?,?,'PROCESSING')
                ON CONFLICT (order_id) DO NOTHING
                """, orderId,productId,quantity);

        //3.check câu insert trước đó, triển khai idempotence
        if(inserted==0){
            InventoryReservation existing = inventoryReservationRepository.findByOrderId(orderId).orElseThrow(()
            -> new IllegalArgumentException("reserve khong tồn tại "));
            //payload k trùng nhưng id trùng nhau
            if(!existing.getProductId().equals(productId)||existing.getQuantity() != quantity){
                throw new IllegalArgumentException("payload bị trùng");
            }

            //Không xử lý lại yêu cầu
            return existing.getStatus();
        }

        //4.Atomic condition Update(tránh overselling)
        int upodateRows = inventoryRepository.reserveStock(productId, quantity);
        ReservationStatus status = upodateRows==1?ReservationStatus.RESERVED:ReservationStatus.REJECTED;

        //5.Luu kết quả của orderId
        jdbcTemplate.update("""
                UPDATE inventory_reservations
                SET status = ?
                WHERE order_id =?
                """,status.name(),orderId);

        return status;
    }
}
