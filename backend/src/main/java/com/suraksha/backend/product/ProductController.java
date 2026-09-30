package com.suraksha.backend.product;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** Public catalogue of insurance lines: the landing page and the claim forms are built from this. */
@RestController
@RequestMapping("/api/products")
public class ProductController {

    @GetMapping
    public List<ProductDefinition> products() {
        return ProductRegistry.ALL;
    }
}
