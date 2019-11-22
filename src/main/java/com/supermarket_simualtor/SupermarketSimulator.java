package com.supermarket_simualtor;

import com.supermarket_simualtor.cash_desk.CashDesk;
import com.supermarket_simualtor.customer.AdultCustomer;
import com.supermarket_simualtor.customer.ChildCustomer;
import com.supermarket_simualtor.customer.Customer;
import com.supermarket_simualtor.customer.RetiredCustomer;
import com.supermarket_simualtor.product.Product;
import com.supermarket_simualtor.product.ProductPermissions;
import com.supermarket_simualtor.random.CustomRandom;
import com.supermarket_simualtor.supermarket.Supermarket;
import com.supermarket_simualtor.supermarket.SupermarketAcceptor;
import com.supermarket_simualtor.supermarket.SupermarketRepository;
import lombok.val;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class SupermarketSimulator {
    private static final int TIMEOUT = 60 * 1000;
    private static final int SLEEP_TIMEOUT = 1000;

    private static final int MIN_ITEMS = 1000;
    private static final int MAX_ITEMS = MIN_ITEMS * 10;

    private static final double MIN_ITEM_PRICE = 0.25;
    private static final double MAX_ITEM_PRICE = 10.0;

    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass().getSimpleName());
    private static final CustomRandom random = CustomRandom.getInstance();

    public static void main(String[] args) {
        logger.info("<<< SIMULATION STARTED >>>");

        val deadline = System.currentTimeMillis() + TIMEOUT;

        val products = createProducts();

        logger.debug("{} items in repository", products.size());

        val repository = createRepository(products);
        val pricing = createPricing(repository.getAssortment());
        val desks = createCashDesks(pricing);
        val supermarket = createSupermarket(desks, repository);

        val customers = createCustomers();

        val pool = Executors.newFixedThreadPool(customers.size());

        for (val customer : customers) {
            pool.execute(() -> {
                while (System.currentTimeMillis() < deadline) {
                    val accepting = random.nextBoolean();
                    if (accepting) {
                        supermarket.accept(customer);
                    }
                    try {
                        Thread.sleep(SLEEP_TIMEOUT);
                    } catch (InterruptedException e) {
                        logger.error(e.getMessage());
                        Thread.currentThread().interrupt();
                    }
                }
            });
        }

        pool.shutdown();

        try {
            pool.awaitTermination(TIMEOUT, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            logger.error(e.getMessage());
            Thread.currentThread().interrupt();
        }

        logger.debug("{} items in repository", repository.getCount());

        logger.info("<<< SIMULATION ENDED >>>");
    }

    private static List<Customer> createCustomers() {
        val dasha = new AdultCustomer("Dasha", 20);
        val david = new AdultCustomer("David", 22);
        val boy = new ChildCustomer("Son", 14);
        val grandpa = new RetiredCustomer("Mom", 55);

        return Arrays.asList(dasha, david, boy, grandpa);
    }

    private static Map<String, Double> createPricing(Set<String> assortment) {
        val map = new HashMap<String, Double>();
        for (val item : assortment) {
            map.put(item, random.getRandomInRange(MIN_ITEM_PRICE, MAX_ITEM_PRICE));
        }
        return map;
    }

    private static List<CashDesk> createCashDesks(Map<String, Double> pricing) {
        return Arrays.asList(
                new CashDesk(0, pricing),
                new CashDesk(1, pricing),
                new CashDesk(2, pricing)
        );
    }

    private static SupermarketAcceptor createSupermarket(List<CashDesk> desks, SupermarketRepository repository) {
        return new Supermarket("FuzzBuzzShop", desks, repository);
    }

    private static SupermarketRepository createRepository(List<Product> products) {
        return new SupermarketRepository(products);
    }

    private static List<Product> createProducts() {
        val products = new ArrayList<Product>();
        ProductPermissions allowed = customer -> true;
        ProductPermissions disallowed = customer -> customer.getAge() >= 18;
        val meta = new HashMap<String, ProductPermissions>();
        meta.put("Apple", allowed);
        meta.put("Orange", allowed);
        meta.put("Banana", allowed);
        meta.put("Bread", allowed);
        meta.put("Butter", allowed);
        meta.put("Water", allowed);
        meta.put("Salad", allowed);
        meta.put("IceCream", allowed);
        meta.put("Chocolate", allowed);
        meta.put("Peanut", allowed);
        meta.put("Beer", disallowed);
        meta.put("Vodka", disallowed);
        meta.put("Cigarettes", disallowed);
        val range = random.getRandomInRange(MIN_ITEMS, MAX_ITEMS);
        for (int i = 0; i < range; i++) {
            val index = random.getRandomInRange(0, meta.size() - 1);
            val key = new ArrayList<>(meta.keySet()).get(index);
            val product = new Product(i, key, meta.get(key));
            products.add(product);
        }
        return products;
    }
}
