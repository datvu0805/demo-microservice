package dat.com.orderservice.application.idempotency;

import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

@Component
public class OrderRequestHasher {

    public String hash(Long productId, Integer quantity) {

        if (productId == null || productId <= 0
                || quantity == null || quantity <= 0) {
            throw new IllegalArgumentException(
                    "productId và quantity phải lớn hơn 0"
            );
        }

        //Chuỗi này được gọi là Canonical Payload — biểu diễn dữ liệu theo một định dạng chuẩn và ổn định.
        String canonicalPayload =
                "create-order:v1"
                        + "|productId=" + productId
                        + "|quantity=" + quantity;

        try {
            //băm mật mã
            MessageDigest digest =
                    MessageDigest.getInstance("SHA-256");

            //tính hash và trả về  byte dài 32 byte
            byte[] bytes = digest.digest(
                    //chuyển string thành byte
                    canonicalPayload.getBytes(StandardCharsets.UTF_8)
            );

            //Nó chuyển 32 bytes thành chuỗi hexadecimal 64 ký tự.
            return HexFormat.of().formatHex(bytes);

        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException(
                    "Không thể khởi tạo SHA-256",
                    ex
            );
        }
    }
}