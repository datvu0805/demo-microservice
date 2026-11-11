package dat.com.inventory2service.kafka.messaging;

import dat.com.inventory2service.entity.Inventory;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.TimeUnit;

@Component
@Slf4j
public class InventoryOutBoxWorker {
    private final OutboxClaimRepository outboxClaimRepository;
    private final KafkaTemplate<String , String> kafkaTemplate;

    public InventoryOutBoxWorker(
            OutboxClaimRepository outboxClaimRepository,
            @Qualifier("outboxKafkaTemplate")
            KafkaTemplate<String,String > kafkaTemplate
    ){
        this.kafkaTemplate=kafkaTemplate;
        this.outboxClaimRepository=outboxClaimRepository;
    }

    @Scheduled(fixedDelay = 1000)
    public void publishPendingEvents(){List<OutboxMessage> messages = outboxClaimRepository.claimBatch();

        for (OutboxMessage message:messages){
            try{
                //1. publish lên kafka
                //get() set timeout 5 giây cho thread hiện tại,
                // nếu k có phản hồi lại từ kafka thì ném excaption

                kafkaTemplate.send(
                        message.topic(),
                        message.orderId().toString(),
                        message.payload()
                ).get(5, TimeUnit.SECONDS);

                //2. kafka xác nhận thành công
                int updated = outboxClaimRepository.markPublished(message);

                if(updated==1){
                    log.info(
                            "Outbox published ; id={}, topic ={}",
                            message.id(),message.topic()
                    );
                }else {
                    log.warn("Outbox claim no longer owned: id={}",
                            message.id()
                    );
                }
            }catch (InterruptedException ex){
                Thread.currentThread().interrupt();
                outboxClaimRepository.markFailed(message);
                return;
            }catch (Exception ex){
                log.error("outbox published failed: id={}",message.id(),ex);
                outboxClaimRepository.markFailed(message);
            }
        }
    }
}
