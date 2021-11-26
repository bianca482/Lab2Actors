package at.fhv.sysarch.lab2.homeautomation.domain;

import java.util.HashMap;
import java.util.Map;

public class ProductCatalog {

    private final Map<String, Product> productMap;

    public ProductCatalog() {
        productMap = new HashMap<>();
        initializeProductMap();
    }

    private void initializeProductMap() {
        productMap.put("milk", new Product("milk", 1.00, 1.00));
        productMap.put("cheese", new Product("cheese", 8.00, 0.50));
        productMap.put("yogurt", new Product("yogurt", 0.49, 0.20));
        productMap.put("butter", new Product("butter", 2.50, 0.25));
        productMap.put("chicken", new Product("chicken", 18.00, 1.25));
        productMap.put("coke", new Product("coke", 1.79, 1.50));
        productMap.put("salad", new Product("salad", 1.50, 1.00));
        productMap.put("beef", new Product("beef", 200.00, 50));
    }

    public Map<String, Product> getProductMap() {
        return productMap;
    }

    public void addProduct(Product product) {
        productMap.put(product.getName(), product);
    }
}
