package dat.com.orderservice.application.usecase;

import dat.com.orderservice.application.event.StockeResultEvent;
import dat.com.orderservice.domain.model.Order;
import dat.com.orderservice.domain.model.OrderStatus;
import dat.com.orderservice.domain.repository.OrderRepository;
import dat.com.orderservice.infrastructure.persistence.entity.OrderInboxEntity;
import dat.com.orderservice.infrastructure.persistence.repository.OrderInboxJpaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProcessStockResultUseCase{

        private static final String STOCK_REVERSED = "stock.reserved";
        private static final String STOCK_REJECTED = "stock.reject";

        private final OrderInboxJpaRepository inboxJpaRepository;
        private final OrderRepository orderRepository;


        @Transactional
        public void execute(StockeResultEvent event, String topic){

            //1. kiểm tra event,topic
            validate(event,topic);

            //2.claim event: chỉ transaction đầu tiên đc xử lý
            int inserted = inboxJpaRepository.claimEvent(
                    event.eventId().toString(), event.orderId().toString(), topic);
            if(inserted==0){
                log.info("Skip duplicate stockResult: eventId{} , orderId{}",event.eventId(),event.orderId());
                return;
            }

            //3.Tìm order  qua domain Repository
            Order order = orderRepository.findById(event.orderId())
                    .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy order: "+event.orderId()));

            //4.Check kết quả có khớp nội dung order k
            if (!Objects.equals(order.getQuantity(),event.quantity())||
                !Objects.equals(order.getProductId(),event.productId())){
                throw new IllegalArgumentException("Kết quả giữ kho không khớp với order: "+event.orderId());
            }

            //5.Xác định trạng thái cần chuyển đến
            OrderStatus targetStatus = STOCK_REVERSED.equals(topic)?OrderStatus.CONFIRM:OrderStatus.CANCELLED;

            //event khác ID nhưng cùng kết quả nghiệp vụ->k cập nhật lần nx
            if(order.getStatus() == targetStatus){
                log.info(
                        "Order đã tồn tại target status: orderId={}, status={}",
                        order.getId(),
                        targetStatus
                );
                return;
            }

            if(order.getStatus() != OrderStatus.PENDING){
                throw new IllegalArgumentException("Kết quả giữ kho xung đột với trạng thái order:"
                                                        +order.getId()
                                                        +"current = "+order.getStatus()
                                                        +"target="+targetStatus
                );
            }

            //6.dOMAIN QUYẾT ĐỊNH và thực hiện trạng thái
            if(targetStatus == OrderStatus.CONFIRM){
                order.confirm();
            }else {
                order.cancel();
            }

            //7.adapter lưu trạng thái và kiểm tra optimistic locking
            Order saved = orderRepository.save(order);

            log.info(
                    "Processed stock result: eventId={}, orderId={}, status={}",
                    event.eventId(),
                    saved.getId(),
                    saved.getStatus()
            );
        }


        private void validate(StockeResultEvent event, String topic){
            if(event==null|| event.eventId()==null || event.orderId()==null
                    || event.productId()==null|| event.quantity()<=0
                    || event.productId()<=0){
                throw new IllegalArgumentException("StockResult không hượp lệ");
            }

            if(topic == null || (!topic.equals(STOCK_REJECTED) && !topic.equals(STOCK_REVERSED))){
                throw new IllegalArgumentException("Topic không hợp lệ"+topic);
            }
        }
}
