package ca.weblite.iapdemo;


import com.codename1.components.MultiButton;
import com.codename1.components.ToastBar;
import com.codename1.components.ToastBar.Status;
import com.codename1.ui.Display;
import com.codename1.ui.Form;
import com.codename1.ui.Dialog;
import com.codename1.ui.Label;
import com.codename1.ui.plaf.UIManager;
import com.codename1.ui.util.Resources;
import com.codename1.io.Log;
import com.codename1.payment.PurchaseCallback;
import com.codename1.processing.Result;
import com.codename1.ui.Container;
import com.codename1.ui.Toolbar;
import com.codename1.ui.layouts.BorderLayout;
import com.codename1.ui.layouts.BoxLayout;
import com.codename1.util.SuccessCallback;
import com.codename1.ws.RESTfulWebServiceClient;
import com.codename1.ws.RESTfulWebServiceClient.Query;
import java.io.IOException;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * This file was generated by <a href="https://www.codenameone.com/">Codename One</a> for the purpose 
 * of building native mobile applications using Java.
 */
public class IAPDemo implements PurchaseCallback {

    private Form current;
    private Resources theme;

    

    public void init(Object context) {
        theme = UIManager.initFirstTheme("/theme");

        // Enable Toolbar on all Forms by default
        Toolbar.setGlobalToolbar(true);

        // Pro only feature, uncomment if you have a pro subscription
        // Log.bindCrashProtection(true);
    }
    
    public void start() {
        if(current != null){
            current.show();
            return;
        }
        Form hi = new Form("Hi World");
        hi.addComponent(new Label("Hi World"));
        hi.show();
    }

    public void stop() {
        current = Display.getInstance().getCurrent();
        if(current instanceof Dialog) {
            ((Dialog)current).dispose();
            current = Display.getInstance().getCurrent();
        }
    }
    
    public void destroy() {
    }

    @Override
    public void itemPurchased(String sku) {
        Store store = Store.getInstance();
        store.findProduct(sku, product->{
            
        });
    }

    @Override
    public void itemPurchaseError(String sku, String errorMessage) {
        throw new RuntimeException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void itemRefunded(String sku) {
        throw new RuntimeException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void subscriptionStarted(String sku) {
        throw new RuntimeException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void subscriptionCanceled(String sku) {
        throw new RuntimeException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void paymentFailed(String paymentCode, String failureReason) {
        throw new RuntimeException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void paymentSucceeded(String paymentCode, double amount, String currency) {
        throw new RuntimeException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    public static enum TimeUnit {
        DAYS,
        WEEKS,
        MONTHs,
        YEARS;
    }
    
    public static class SubscriptionPeriod {
        int value;
        TimeUnit unit;
        
        public String toString() {
            return value + unit.toString();
        }
    }
    
    public static class Product {
        String sku;
        String name;
        String description;
        double price;
        SubscriptionPeriod subscriptionPeriod;
        boolean autoRenew;
        
    }
    
    public static class Receipt {
        String token;
        String transactionId;
        Product product;
        Date transactionDate;
        Date startDate;
        boolean cancelled;

    }
    
    public static class Purchase {
        String id;
        Product product;
        Receipt[] receipts;
        
        public Receipt getOriginalReceipt() {
            if (receipts == null || receipts.length == 0) {
                return null;
            }
            Receipt first = receipts[0];
            for (Receipt r : receipts) {
                if (first.transactionDate == null) {
                    first = r;
                    continue;
                }
                if (r != null && r.transactionDate.getTime() < first.transactionDate.getTime()) {
                    first = r;
                    continue;
                }
            }
            return first;
            
        }
        
        public Receipt getLastReceipt() {
            if (receipts == null || receipts.length == 0) {
                return null;
            }
            Receipt last = receipts[0];
            for (Receipt r : receipts) {
                if (last.cancelled) {
                    last = r;
                }
                if (last.transactionDate == null) {
                    last = r;
                    continue;
                }
                if (r != null && !r.cancelled && r.transactionDate.getTime() > last.transactionDate.getTime()) {
                    last = r;
                    continue;
                }
                
            }
            return last;
            
        }
        
        /**
         * Gets the expiry date of the purchase, in case of subscription.
         * If there are no valid receipts, or the product is not a subscription
         * then this will return null;
         * @return 
         */
        public Date getExpiryDate() {
            Receipt last = getLastReceipt();
            if (last != null) {
                Date startDate = last.startDate;
                
                if (startDate == null) {
                    return null;
                }
                
                SubscriptionPeriod period = product.subscriptionPeriod;
                if (period == null) {
                    return null;
                }
                Calendar cal = Calendar.getInstance();
                cal.setTime(startDate);
                switch (period.unit) {
                    case DAYS:
                        cal.add(Calendar.DATE, period.value);
                        break;
                    case WEEKS:
                        cal.add(Calendar.DATE, period.value * 7);
                        break;
                    case MONTHs:
                        cal.add(Calendar.MONTH, period.value);
                        break;
                    case YEARS:
                        cal.add(Calendar.YEAR, period.value);
                        break;
                }
                return cal.getTime();
                
            }
            return null;
                
        }
        
    }
    
    
    
    public static class Store {
        static Store instance;
        Product[] availableProducts;
        Purchase[] purchases;
        RESTfulWebServiceClient productsClient;
        RESTfulWebServiceClient purchasesClient;
        
        
        public static Store getInstance() {
            if (instance == null) {
                instance = new Store();
            }
            return instance;
        }
        
        public void findProduct(String sku, SuccessCallback<Product> callback) {
            loadProducts(products->{
                if (products == null) {
                    callback.onSucess(null);
                    return;
                }
                for (Product p : products) {
                    if (p.sku != null && p.sku.equals(sku)) {
                        callback.onSucess(p);
                        return;
                    }
                }
                callback.onSucess(null);
            });
        }
        
        public void loadProducts(SuccessCallback<Product[]> callback) {
            if (availableProducts != null) {
                //callback.onSucess(Arrays.copyOf(availableProducts, availableProducts.length));
                callback.onSucess(null);
                return;
            }
            productsClient.find(new Query(), rowset->{
                if (rowset == null) {
                    callback.onSucess(null);
                    return;
                }
                if (rowset.getLast() > 0) {
                    availableProducts = new Product[rowset.getLast()-1];
                    int pos = 0;
                    for (Map m : rowset) {
                        availableProducts[pos++] = createProduct(m);
                    }
                } else {
                    availableProducts = new Product[0];
                }
                
                //callback.onSucess(Arrays.copyOf(availableProducts, availableProducts.length));
                callback.onSucess(null);
            });
        }
        
        public void loadPurchases(SuccessCallback<Purchase[]> callback) {
            if (purchases != null) {
                callback.onSucess(purchases);
                return;
            }
            purchasesClient.find(new Query(), rowset->{
                if (rowset == null) {
                    callback.onSucess(null);
                    return;
                }
                if (rowset.getLast() > 0) {
                    purchases = new Purchase[rowset.getLast()-1];
                    int pos = 0;
                    for (Map m : rowset) {
                        purchases[pos++] = createPurchase(m);
                    }
                } else {
                    purchases = new Purchase[0];
                }
                //callback.onSucess(Arrays.copyOf(purchases, purchases.length));
                callback.onSucess(null);
            });
        }
        
        private SubscriptionPeriod createSubscriptionPeriod(String unit, int value) {
            TimeUnit tu = null;
            if ("DAYS".equalsIgnoreCase(unit)) {
                tu = TimeUnit.DAYS;
            } else if ("WEEKS".equalsIgnoreCase(unit)) {
                tu = TimeUnit.WEEKS;
            } else if ("MONTHS".equalsIgnoreCase(unit)) {
                tu = TimeUnit.MONTHs;
            } else if ("YEARS".equalsIgnoreCase(unit)){
                tu = TimeUnit.YEARS;
            } else {
                throw new IllegalArgumentException("Time unit must be one of DAYS, WEEKS, MONTHS, YEARS but received "+unit);
            }
            
            SubscriptionPeriod out = new SubscriptionPeriod();
            out.unit = tu;
            out.value = value;
            return out;
            
        }
        
        private Product createProduct(Map m) {
            Result res = Result.fromContent(m);
            Product out = new Product();
            out.autoRenew = res.getAsBoolean("autoRenew");
            out.description = res.getAsString("description");
            out.name = res.getAsString("name");
            out.price = res.getAsDouble("price");
            out.sku = res.getAsString("sku");
            out.subscriptionPeriod = createSubscriptionPeriod(
                    res.getAsString("subscriptionPeriodUnit"),
                    res.getAsInteger("subscriptionPeriod")
            );
            return out;
            
        }
        
        private Receipt createReceipt(Map m, Product product) {
            Result res = Result.fromContent(m);
            Receipt out = new Receipt();
            out.product = product;
            out.cancelled = res.getAsBoolean("cancelled");
            out.startDate = new Date(res.getAsLong("startDate"));
            out.token = res.getAsString("token");
            out.transactionDate = new Date(res.getAsLong("transactionDate"));
            out.transactionId = res.getAsString("transactionId");
            
            return out;
        }
        
        private Purchase createPurchase(Map m) {
            Result res = Result.fromContent(m);
            Purchase out = new Purchase();
            out.product = createProduct((Map)res.get("product"));
            List<Map> receipts = (List<Map>)res.getAsArray("receipts");
            int pos = 0;
            out.receipts = new Receipt[receipts.size()];
            for (Map mReceipt : receipts) {
                out.receipts[pos++] = createReceipt(mReceipt, out.product);
            }
            return out;
        }
    }
    
    
    
    private class ViewFactory {
        
        Container createProductList(Product[] products) {
            Container root = new Container(new BorderLayout());
            
            Container list = new Container(new BoxLayout(BoxLayout.Y_AXIS));
            for (Product product : products) {
                Container productRow = createProductRow(list, product);
                list.add(productRow);
            }
            
            root.addComponent(BorderLayout.CENTER, list);
            
            return root;
        }
        
        Container createProductRow(Container list, Product product) {
            
            
            MultiButton out = new MultiButton();
            out.setTextLine1(product.name);
            out.setTextLine2(product.description);
            out.setTextLine3(product.subscriptionPeriod != null ? product.subscriptionPeriod.toString() + " Subscription" : "");
            
            return out;
            
        }
        
        Container createPurchaseList(Purchase[] purchases) {
            Container root = new Container(new BorderLayout());
            
            Container list = new Container(new BoxLayout(BoxLayout.Y_AXIS));
            for (Purchase purchase : purchases) {
                Container purchaseRow = createPurchaseRow(list, purchase);
                list.add(purchaseRow);
            }
            
            root.addComponent(BorderLayout.CENTER, list);
            
            return root;
        }
        
        Container createPurchaseRow(Container list, Purchase purchase) {
            
            
            MultiButton out = new MultiButton();
            out.setTextLine1(purchase.product.name);
            if (purchase.getOriginalReceipt() != null && purchase.getOriginalReceipt().transactionDate != null) {
                out.setTextLine2(purchase.getOriginalReceipt().transactionDate.toString());
            }
            if (purchase.getExpiryDate() != null) {
                out.setTextLine3("Expires "+purchase.getExpiryDate());
            }
            
            
            return out;
            
        }
    }
    
    
    private class Controller {
        
        void showProductList(Store store) {
            Status status = ToastBar.getInstance().createStatus();
            status.setMessage("Loading products...");
            status.setShowProgressIndicator(true);
            status.showDelayed(100);
            store.loadProducts(products->{
                status.clear();
                if (products == null) {
                    Dialog.show("Failed", "Failed to load product list", "OK", null);
                    return;
                }
                
                
                Form f = new Form("Products");
                f.setLayout(new BorderLayout());
                Container productList = new ViewFactory().createProductList(products);
                f.addComponent(BorderLayout.CENTER, productList);
                f.show();
                
            });
        }
        
        void showPurchases(Store store) {
            Status status = ToastBar.getInstance().createStatus();
            status.setMessage("Loading purchases...");
            status.setShowProgressIndicator(true);
            status.showDelayed(100);
            store.loadPurchases(purchases->{
                status.clear();
                if (purchases == null) {
                    Dialog.show("Failed", "Failed to load product list", "OK", null);
                    return;
                }
                
                
                Form f = new Form("Products");
                f.setLayout(new BorderLayout());
                Container purchaseList = new ViewFactory().createPurchaseList(purchases);
                f.addComponent(BorderLayout.CENTER, purchaseList);
                f.show();
                
            });
        }
        
        
    }
}
