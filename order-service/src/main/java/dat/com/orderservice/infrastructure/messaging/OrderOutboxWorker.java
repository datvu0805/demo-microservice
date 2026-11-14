package dat.com.orderservice.infrastructure.messaging;

import dat.com.orderservice.application.event.OrderCreatedevent;
import dat.com.orderservice.config.KafkaTopicConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

import java.util.List;
import java.util.concurrent.TimeUnit;

@Component
@RequiredArgsConstructor
@Slf4j
public class OrderOutboxWorker {

    private final OrderOutboxClaimRepository orderOutboxClaimRepository;
    private final OrderEventPublisher orderEventPublisher;
    private final ObjectMapper objectMapper;

    @Scheduled(fixedDelay = 1000)
    public void publishPendingEvent(){

        List<OrderOutboxMessage> messages = orderOutboxClaimRepository.claimBatch();

        for (OrderOutboxMessage message:messages){
            try {
                //publisher chỉ cho topic order.created
                if (!KafkaTopicConfig.ORDER_CREATED.equals(message.topic())){
                    throw new IllegalArgumentException("Topic không được hỗ trợ");
                }
                OrderCreatedevent event = objectMapper.readValue(message.payload(),OrderCreatedevent.class);

                orderEventPublisher.publish(event).get(5, TimeUnit.SECONDS);

                int updated = orderOutboxClaimRepository.markPublished(message);
                if(updated==1){
                    log.info(
                            "Order outbox published: id={}, orderId={}",
                            message.id(),
                            message.orderId()
                    );
                }else  {
                    log.warn(
                            "Order outbox claim lost: id={}",
                            message.id()
                    );
                }
            }  catch (InterruptedException ex) {
                Thread.currentThread().interrupt();

                log.warn(
                        "Order outbox worker interrupted: id={}",
                        message.id()
                );

                // Các bản ghi chưa hoàn tất sẽ được claim lại
                // sau khi lease hết hạn.
                return;

            } catch (Exception ex) {
                log.error(
                        "Order outbox processing failed: id={}",
                        message.id(),
                        ex
                );

                // Trả về NEW, tăng retry_count,
                // chờ 10 giây trước lần thử tiếp theo.
                orderOutboxClaimRepository.markFailed(message);
            }
        }


        }
    }
