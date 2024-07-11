package com.udacity.pricing;

import com.udacity.pricing.domain.price.Price;
import com.udacity.pricing.domain.price.PriceRepository;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.math.BigDecimal;

@RunWith(SpringRunner.class)
@DataJpaTest
public class PricingServiceCrudTests {

    @Autowired
    private PriceRepository repository;

    private final Price testPrice = new Price("USD", BigDecimal.valueOf(3500.0), 100L);

    @Test
    public void priceCreateTest() throws Exception {
        repository.save(testPrice);
        assert repository.count() == 1;
        assert repository.existsById(100L);
        assert repository.findById(testPrice.getVehicleId()).orElseThrow().equals(testPrice);
    }

    @Test
    public void priceDeleteTest() throws Exception {
        priceCreateTest();
        repository.deleteAll();
        assert repository.count() == 0;
    }

    @Test
    public void priceUpdateTest() throws Exception {
        priceCreateTest();
        Price newPrice = new Price("USD", BigDecimal.valueOf(1000.0), 100L);
        repository.save(newPrice);
        assert repository.count() == 1;
        assert repository.findById(newPrice.getVehicleId()).orElseThrow().equals(newPrice);
    }
}
