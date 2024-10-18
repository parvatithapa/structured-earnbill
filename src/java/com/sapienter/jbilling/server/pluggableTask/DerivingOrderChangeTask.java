package com.sapienter.jbilling.server.pluggableTask;

import com.sapienter.jbilling.server.item.db.ItemDAS;
import com.sapienter.jbilling.server.item.db.ItemDTO;
import com.sapienter.jbilling.server.pluggableTask.admin.ParameterDescription;
import com.sapienter.jbilling.server.pricing.db.PriceModelDTO;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;
import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.apache.commons.collections.CollectionUtils;

import com.sapienter.jbilling.server.pluggableTask.admin.PluggableTaskException;

public class DerivingOrderChangeTask extends PluggableTask implements IOrderChangeTask {

    public static final ParameterDescription PARAMETER_PRODUCT_CODES =
            new ParameterDescription("productCode", true, ParameterDescription.Type.STR);
    public static final ParameterDescription PARAMETER_MAX_DISCOUNT =
            new ParameterDescription("maxDiscount", true, ParameterDescription.Type.INT);
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    private String productCodes;
    private BigDecimal maxDiscount;

    {
        descriptions.add(PARAMETER_PRODUCT_CODES);
        descriptions.add(PARAMETER_MAX_DISCOUNT);
    }


    void init() {
        productCodes = parameters.get(PARAMETER_PRODUCT_CODES.getName());
        maxDiscount = new BigDecimal(parameters.get(PARAMETER_MAX_DISCOUNT.getName()));
    }

    public String guessProductByAmount(BigDecimal amount, Integer entityId) throws PluggableTaskException {

        init();
        List<Product> productList = getProducts(entityId);
        logger.info("Product List: " + productList);

        String result = null;
        String productCode = null;
        if (CollectionUtils.isNotEmpty(productList)) {
            //  sort the productlist in descending order based on max price of the product.
            Collections.sort(productList, (p1, p2) -> p2.getMaxPrice().compareTo(p1.getMaxPrice()));

            Integer count = 0;
            Product minPriceProduct = productList.get(productList.size() - 1);
            BigDecimal multipleQntAmount;
            do {
                count++;
                multipleQntAmount = amount.divide(BigDecimal.valueOf(count), 2, BigDecimal.ROUND_HALF_UP);
                productCode = process(productList, multipleQntAmount);
            } while (multipleQntAmount.compareTo(minPriceProduct.getMinPrice()) > 0 && productCode == null);

            result = (StringUtils.isNotBlank(productCode) ? productCode : minPriceProduct.getProductCode()) + ":" + (count);
        } else {
            logger.error("Product/s is/are not available {}", DerivingOrderChangeTask.class);
            throw new PluggableTaskException("Product/s is/are not available.");
        }
        return result;
    }

    private List<Product> getProducts(Integer entityId) {
        String[] productCodesArr = productCodes.split(",");
        Set<String> collectedProductCodes = Arrays.stream(productCodesArr)
                .filter(pc -> StringUtils.isNotBlank(pc))
                .collect(Collectors.toSet());

        List<Product> list = new ArrayList<>();

        for (String code : collectedProductCodes) {
            ItemDTO item = new ItemDAS().findItemByInternalNumber(code, entityId);
            if (null != item) {
                Product product = new Product();
                product.setProductCode(item.getInternalNumber());
                PriceModelDTO itemPrice = item.getPrice(new Date(), entityId);
                BigDecimal price = itemPrice.getRate();

                product.setMaxPrice(price);
                product.setMinPrice(calculateDiscount(price, maxDiscount));

                list.add(product);
            }
        }
        return list;
    }

    private String process(List<Product> list, BigDecimal amount) {
        return list.stream()
                .filter(p -> p.getMinPrice().compareTo(amount) <= 0 && p.getMaxPrice().compareTo(amount) >= 0)
                .map(Product::getProductCode)
                .findFirst().orElse(null);
    }

    private BigDecimal calculateDiscount(BigDecimal originalPrice, BigDecimal discountPercentage) {
        BigDecimal discountAmount = originalPrice.multiply(discountPercentage.divide(new BigDecimal("100")));
        return originalPrice.subtract(discountAmount);
    }

}

@Data
@NoArgsConstructor
class Product {
    private String productCode;
    private BigDecimal maxPrice;
    private BigDecimal minPrice;

}
