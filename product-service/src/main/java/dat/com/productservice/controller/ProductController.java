package dat.com.productservice.controller;

import dat.com.productservice.entity.Product;
import dat.com.productservice.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/product")
public class ProductController {
    private final ProductService productService;

    @GetMapping("/{id}")
    public Product getById(@PathVariable Long id){
        return productService.getProductById(id);
    }
}
