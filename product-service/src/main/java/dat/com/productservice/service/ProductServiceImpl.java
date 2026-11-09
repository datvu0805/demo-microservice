package dat.com.productservice.service;

import dat.com.productservice.entity.Product;
import dat.com.productservice.repository.ProductRepository;
import jakarta.ws.rs.NotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.config.ConfigDataResourceNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ProductServiceImpl implements ProductService {

    private final ProductRepository productRepository;

    @Transactional
    @Override
    public Product getProductById(Long id) {
       Product product= productRepository.findById(id).orElseThrow(()-> new NotFoundException("Không có id :"+id));
        return new  Product(product.getId(),
                            product.getName(),
                            product.getSku(),
                            product.getDescription(),
                            product.getPrice(),
                            product.getStockQuantity());
    }
}