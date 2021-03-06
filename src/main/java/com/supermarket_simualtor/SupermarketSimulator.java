package com.supermarket_simualtor;

import com.supermarket_simualtor.cash_desk.CashDesk;
import com.supermarket_simualtor.customer.AdultCustomer;
import com.supermarket_simualtor.customer.ChildCustomer;
import com.supermarket_simualtor.customer.Customer;
import com.supermarket_simualtor.customer.RetiredCustomer;
import com.supermarket_simualtor.customer.storage.Bonuses;
import com.supermarket_simualtor.customer.storage.Card;
import com.supermarket_simualtor.customer.storage.Wallet;
import com.supermarket_simualtor.product.Product;
import com.supermarket_simualtor.product.ProductDiscounts;
import com.supermarket_simualtor.product.ProductPermissions;
import com.supermarket_simualtor.random.CustomRandom;
import com.supermarket_simualtor.report.Report;
import com.supermarket_simualtor.supermarket.Supermarket;
import com.supermarket_simualtor.supermarket.SupermarketAcceptor;
import com.supermarket_simualtor.supermarket.SupermarketRepository;
import com.supermarket_simualtor.utils.StringUtils;
import lombok.Value;
import lombok.val;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class SupermarketSimulator {
    private static final int SECOND = 1000;
    private static final int TIMEOUT = 60 * SECOND; // 60s
    private static final int SLEEP_TIMEOUT = SECOND; // 1s

    private static final int MIN_ITEMS = 10000;
    private static final int MAX_ITEMS = MIN_ITEMS * 10;

    private static final double MIN_ITEM_PRICE = 0.25;
    private static final double MAX_ITEM_PRICE = 10.0;

    private static final double MIN_WEIGHT = 100.0; // 100g
    private static final double MAX_WEIGHT = 20000.0; // 20kg

    private static final double MIN_MONEY_PER_CUSTOMER = 1000.0;
    private static final double MAX_MONEY_PER_CUSTOMER = 10000.0;

    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass().getSimpleName());
    private static final CustomRandom random = CustomRandom.getInstance();

    public static void main(String[] args) {
        logger.info("<<< SIMULATION STARTED >>>");

        val deadline = System.currentTimeMillis() + TIMEOUT;

        val products = createProducts();

        logger.debug("{} items in repository", products.size());

        val repository = createRepository(products);
        val pricing = createPricing(repository.getAssortment());
        val report = createReport();
        val desks = createCashDesks(pricing, report);
        val supermarket = createSupermarket(pricing, desks, repository);

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

        logger.info("{}$ total income for {} seconds", report, TIMEOUT / SECOND);

        logger.info("<<< SIMULATION ENDED >>>");
    }

    private static List<Customer> createCustomers() {
        val dasha = new AdultCustomer("Dasha", 20, createWallet(), createCard(), createBonuses());
        val david = new AdultCustomer("David", 22, createWallet(), createCard(), createBonuses());
        val boy = new ChildCustomer("Son", 14, createWallet(), createCard(), createBonuses());
        val grandpa = new RetiredCustomer("Mom", 55, createWallet(), createCard(), createBonuses());

        return Arrays.asList(dasha, david, boy, grandpa);
    }

    private static Wallet createWallet() {
        double total = random.getRandomInRange(MIN_MONEY_PER_CUSTOMER, MAX_MONEY_PER_CUSTOMER);
        return new Wallet(total);
    }

    private static Card createCard() {
        double total = random.getRandomInRange(MIN_MONEY_PER_CUSTOMER, MAX_MONEY_PER_CUSTOMER);
        return new Card(total);
    }

    private static Bonuses createBonuses() {
        return new Bonuses(0.0);
    }

    private static Map<String, Double> createPricing(Set<String> assortment) {
        val map = new HashMap<String, Double>();
        for (val item : assortment) {
            val price = random.getRandomInRange(MIN_ITEM_PRICE, MAX_ITEM_PRICE);
            logger.info("Selling {} for {}$ per unit", item, StringUtils.friendlyDouble(price));
            map.put(item, price);
        }
        return map;
    }

    private static Report createReport() {
        return new Report();
    }

    private static List<CashDesk> createCashDesks(Map<String, Double> pricing, Report report) {
        var i = 0;
        return Arrays.asList(
            new CashDesk(i++, pricing, report),
            new CashDesk(i++, pricing, report),
            new CashDesk(i, pricing, report)
        );
    }

    private static SupermarketAcceptor createSupermarket(
        Map<String, Double> pricing,
        List<CashDesk> desks,
        SupermarketRepository repository
    ) {
        return new Supermarket("FuzzBuzzShop", pricing, desks, repository);
    }

    private static SupermarketRepository createRepository(List<Product> products) {
        return new SupermarketRepository(products);
    }

    private static List<Product> createProducts() {
        val products = new ArrayList<Product>();

        // permissions
        ProductPermissions allowedForAll = customer -> true;
        ProductPermissions allowedForAdult = Customer::isAdult;

        // discounts & bonuses
        ProductDiscounts noDiscountNoBonuses = new ProductDiscounts() {
            @Override
            public double discountForRetired(Customer customer) {
                return 1.0;
            }

            @Override
            public double applyBonuses() {
                return 0;
            }
        };
        ProductDiscounts discountForRetiredNoBonuses = new ProductDiscounts() {
            @Override
            public double discountForRetired(Customer customer) {
                return customer.isRetired() ? 0.8 : 1.0;
            }

            @Override
            public double applyBonuses() {
                return 0;
            }
        };
        ProductDiscounts noDiscountWithBonuses = new ProductDiscounts() {
            @Override
            public double discountForRetired(Customer customer) {
                return 1.0;
            }

            @Override
            public double applyBonuses() {
                return 0.5;
            }
        };

        // meta information
        val meta = new HashMap<String, MetaInfo>();
        meta.put("Apple", new MetaInfo(allowedForAll, noDiscountWithBonuses, false));
        meta.put("Orange", new MetaInfo(allowedForAll, noDiscountWithBonuses, false));
        meta.put("Banana", new MetaInfo(allowedForAll, noDiscountNoBonuses, false));
        meta.put("Bread", new MetaInfo(allowedForAll, discountForRetiredNoBonuses, false));
        meta.put("Butter", new MetaInfo(allowedForAll, noDiscountNoBonuses, false));
        meta.put("Water", new MetaInfo(allowedForAll, discountForRetiredNoBonuses, false));
        meta.put("Salad", new MetaInfo(allowedForAll, noDiscountNoBonuses, false));
        meta.put("IceCream", new MetaInfo(allowedForAll, noDiscountNoBonuses, false));
        meta.put("Chocolate", new MetaInfo(allowedForAll, noDiscountNoBonuses, false));
        meta.put("Peanut", new MetaInfo(allowedForAll, noDiscountNoBonuses, false));
        meta.put("Beer", new MetaInfo(allowedForAdult, noDiscountNoBonuses, false));
        meta.put("Vodka", new MetaInfo(allowedForAdult, noDiscountNoBonuses, false));
        meta.put("Cigarettes", new MetaInfo(allowedForAdult, noDiscountWithBonuses, false));
        meta.put("Rice", new MetaInfo(allowedForAll, noDiscountNoBonuses, true));
        meta.put("Nuts", new MetaInfo(allowedForAll, noDiscountNoBonuses, true));

        // create product list
        val range = random.getRandomInRange(MIN_ITEMS, MAX_ITEMS);
        for (int i = 0; i < range; i++) {
            val index = random.getRandomInRange(0, meta.size() - 1);
            val key = new ArrayList<>(meta.keySet()).get(index);
            val weight = random.getRandomInRange(MIN_WEIGHT, MAX_WEIGHT);
            val payload = meta.get(key);
            val product = payload.weighted
                ? new Product(i, key, weight, payload.permissions, payload.discounts)
                : new Product(i, key, payload.permissions, payload.discounts);
            products.add(product);
        }
        return products;
    }

    @Value
    private static class MetaInfo {
        private final ProductPermissions permissions;
        private final ProductDiscounts discounts;
        private final boolean weighted;
    }
}
