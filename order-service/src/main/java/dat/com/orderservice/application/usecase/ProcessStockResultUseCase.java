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

            boolean changed;

            if("stock.reserved".equals(topic)){

                if(order.getStatus() == OrderStatus.CANCELLING || order.getStatus() == OrderStatus.CANCELLED ){
                    log.info(
                            "Ignore late stock.reserved: orderId={}, status={}",
                            order.getId(),
                            order.getStatus()
                    );
                    return;

                }
                if(order.getStatus()==OrderStatus.CONFIRM){
                    return;
                }
                order.confirm();
                changed=true;

            }else {
                changed=order.rejectStock();
            }

            if(changed){
                orderRepository.save(order);
            }

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
