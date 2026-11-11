package dat.com.inventory2service.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "inventory")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Inventory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String productName;

    @Column(unique = true)
    private Long productId;

    @Column(name = "reserved_quantity", nullable = false)
    private Integer reservedQuantity = 0;

    private Integer quantity;
}