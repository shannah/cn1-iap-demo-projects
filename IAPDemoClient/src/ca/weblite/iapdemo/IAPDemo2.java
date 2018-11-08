/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ca.weblite.iapdemo;

import com.codename1.components.FloatingActionButton;
import com.codename1.components.SpanLabel;
import com.codename1.components.ToastBar;
import com.codename1.io.ConnectionRequest;
import com.codename1.io.Log;
import com.codename1.io.Storage;
import com.codename1.payment.Purchase;
import com.codename1.payment.PurchaseCallback;
import com.codename1.payment.Receipt;
import com.codename1.payment.ReceiptStore;
import com.codename1.processing.Result;
import com.codename1.ui.Button;
import com.codename1.ui.Container;
import com.codename1.ui.Dialog;
import com.codename1.ui.Display;
import com.codename1.ui.FontImage;
import com.codename1.ui.Form;
import com.codename1.ui.Label;
import com.codename1.ui.Toolbar;
import com.codename1.ui.animations.CommonTransitions;
import com.codename1.ui.layouts.BorderLayout;
import com.codename1.ui.layouts.BoxLayout;
import com.codename1.ui.plaf.UIManager;
import com.codename1.ui.util.Resources;
import com.codename1.util.Base64;
import com.codename1.util.Callback;
import com.codename1.util.SuccessCallback;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import com.codename1.ws.RESTfulWebServiceClient;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author shannah
 */
public class IAPDemo2 implements PurchaseCallback {
    
    private Form current;
    private Resources theme;
    private final ViewFactory viewFactory = new ViewFactory();
    private final Controller controller = new Controller();
    //private static final String tutorialSubscriptionsURL = "https://example.com/tutorialSubscriptions";
    private static final String localHost = "http://10.0.1.32";
    private static final String receiptsEndpoint = localHost+":8080/IAPServer/webresources/com.codename1.demos.iapserver.receipts";
    private static Purchase iap;
    public void init(Object context) {
        theme = UIManager.initFirstTheme("/theme");

        // Enable Toolbar on all Forms by default
        Toolbar.setGlobalToolbar(true);
        iap = Purchase.getInAppPurchase();
        // Pro only feature, uncomment if you have a pro subscription
        // Log.bindCrashProtection(true);
        
        // Define a receipt loader
        iap.setReceiptStore(createReceiptStore());
    }
    
    /**
     * Creates a receipt loader to load receipts from our web service
     * @return 
     */
    private ReceiptStore createReceiptStore() {
        return new ReceiptStore() {
            
            RESTfulWebServiceClient client = createRESTClient(receiptsEndpoint);
            
            @Override
            public void fetchReceipts(SuccessCallback<Receipt[]> callback) {
                RESTfulWebServiceClient.Query query = new RESTfulWebServiceClient.Query() {

                    @Override
                    protected void setupConnectionRequest(RESTfulWebServiceClient client, ConnectionRequest req) {
                        super.setupConnectionRequest(client, req);
                        req.setUrl(receiptsEndpoint);
                    }
                    
                };
                client.find(query, rowset->{
                    List<Receipt> out = new ArrayList<Receipt>();
                    for (Map m : rowset) {
                        Result res = Result.fromContent(m);
                        Receipt r = new Receipt();
                        r.setTransactionId(res.getAsString("transactionId"));
                        r.setPurchaseDate(new Date(res.getAsLong("purchaseDate")));
                        r.setQuantity(1);
                        r.setSku(res.getAsString("sku"));

                        if (m.containsKey("cancellationDate") && m.get("cancellationDate") != null) {
                            r.setCancellationDate(new Date(res.getAsLong("cancellationDate")));
                        }
                        if (m.containsKey("expiryDate") && m.get("expiryDate") != null) {
                            r.setExpiryDate(new Date(res.getAsLong("expiryDate")));
                        }
                        out.add(r);

                    }
                    callback.onSucess(out.toArray(new Receipt[out.size()]));
                }); 
            }

            @Override
            public void submitReceipt(Receipt r, SuccessCallback<Boolean> callback) {
                Map m = new HashMap();
                m.put("transactionId", r.getTransactionId());
                m.put("sku", r.getSku());
                m.put("purchaseDate", r.getPurchaseDate().getTime());
                m.put("orderData", r.getOrderData());
                m.put("storeCode", r.getStoreCode());
                client.create(m, callback);
            }

        };
    }
    
    public void start() {
        if(current != null){
            current.show();
            return;
        }
        controller.showCreditsForm();
        
        // If there are any pending purchases that haven't been sent to the server
        // we'll send them now.
        iap.synchronizeReceipts();
        
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
    
    public static final String SKU_10_CREDITS = "iapdemo.credits.10";
    public static final String SKU_NOADS_1MONTH = "iapdemo.noads.month.auto";
    public static final String SKU_NOADS_3MONTH = "iapdemo.noads.3month.auto";
    public static final String SKU_TUTORIALS_1MONTH =  "iapdemo.tutorials.1month";
    
    /**
     * List of all product skus we have
     */
    static String[] products = new String[] {
        SKU_NOADS_1MONTH,
        SKU_TUTORIALS_1MONTH,
        SKU_NOADS_3MONTH,
        SKU_10_CREDITS,
        "iapdemo.calculator"
        
    };
    
    /**
     * List of product skus for the NO ADs subscription.
     */
    String[] adSkus = new String[] {
        SKU_NOADS_1MONTH,
        SKU_NOADS_3MONTH
    };
    
    /**
     * List of product skus for tutorials section subscription.
     */
    String[] tutorialSkus = new String[]{
        SKU_TUTORIALS_1MONTH
    };
           
    /**
     * Storage key where we store the number of credits that the user currently has.
     */
    private static final String CREDITS_KEY = "IAPDemo2Credits.dat";
    
    /**
     * Callbacks used inside view factory to add callbacks upon completed purchase.
     * These are handy for the view code to be able to run code in the context
     * of the view, however, these callbacks should not be used for tracking of 
     * purchases in case the app crashes between the purchase and when the callback is called.
     * 
     */
    Map<String,Callback> purchaseCallbacks = new HashMap<String, Callback>();
    
    /**
     * Sets a purchase callback for a given sku to be fired when purchase is complete.
     * NOTE:  This callback is not guaranteed to be fired.  Use it only for updating the UI, 
     * not for recording purchases.
     * @param sku
     * @param callback 
     */
    private void setPurchaseCallback(String sku, Callback callback) {
        purchaseCallbacks.put(sku, callback);
    }
    
    /**
     * Gets the currently registered callback for a sku.  And removes it.
     * @param sku The sku for which the callback will be called.
     * @return 
     */
    private Callback getAndRemovePurchaseCallback(String sku) {
        Callback cb = purchaseCallbacks.get(sku);
        if (cb == null) {
            cb = new Callback() {

                @Override
                public void onSucess(Object value) {
                    
                }

                @Override
                public void onError(Object sender, Throwable err, int errorCode, String errorMessage) {
                    Log.p(errorMessage);
                    Log.e(err);
                }
                
            };
        } else {
            purchaseCallbacks.remove(sku);
        }
        return cb;
    }
    
    // CREDITS -----------------------------------------------------------------
    
    /**
     * Gets the number of credits that the user currently has. 
     * @return 
     */
    public int getCredits() {
        synchronized (CREDITS_KEY) {
            Storage s = Storage.getInstance();
            if (s.exists(CREDITS_KEY)) {
                return (Integer) Storage.getInstance().readObject(CREDITS_KEY);
            }
            
            return 0;
        }
    }
    
    /**
     * Exception thrown if number of credits are changed to a negative number.
     */
    public static class NegativeCreditsException extends Exception {
        
    }
    
    /**
     * Sets the number of credits that the user currently has.
     * @param credits
     * @throws ca.weblite.iapdemo.IAPDemo2.NegativeCreditsException 
     */
    public void setCredits(int credits) throws NegativeCreditsException {
        synchronized (CREDITS_KEY) {
            if (credits < 0) {
                throw new NegativeCreditsException();
            }
            Storage s = Storage.getInstance();
            s.writeObject(CREDITS_KEY, new Integer(credits));
        }
    }
    
    /**
     * Adds credits atomically.
     * 
     * @param credits
     * @throws ca.weblite.iapdemo.IAPDemo2.NegativeCreditsException 
     */
    public void addCredits(int credits) throws NegativeCreditsException {
        synchronized (CREDITS_KEY) {
            int existing = getCredits();
            setCredits(existing + credits);
        }
    }
    
    
    /**
     * Use a credit atomically.
     * 
     * @throws ca.weblite.iapdemo.IAPDemo2.NegativeCreditsException 
     */
    public void useCredit() throws NegativeCreditsException{
        addCredits(-1);
    }
    
   
    // END CREDITS -------------------------------------------------------------
    
    
    // REST Client
    
    /**
     * The username to connect to our subscriptions webservice.
     * @return 
     */
    private String getUsername() {
        return "admin";
    }
    
    /**
     * The password to connect to our subscriptions webservice.
     * @return 
     */
    private String getPassword() {
        return "password";
    }
    
    /**
     * Creates a REST client to connect to a particular endpoint.  The REST client
     * generated here will automatically add the Authorization header and the X-CN1-Platform header
     * which tells the service what platform we are on.
     * @param url The url of the endpoint.
     * @return 
     */
    private RESTfulWebServiceClient createRESTClient(String url) {
        return new RESTfulWebServiceClient(url) {

            @Override
            protected void setupConnectionRequest(ConnectionRequest req) {
                try {
                    
                    req.addRequestHeader("Authorization", "Basic " + Base64.encode((getUsername()+":"+getPassword()).getBytes("UTF-8")));
                    req.addRequestHeader("X-IAP-CN1-Platform", Display.getInstance().getPlatformName());
                    req.addRequestHeader("X-IAP-CN1-Simulator", String.valueOf(Display.getInstance().isSimulator()));
                } catch (Exception ex) {}
            }
            
        };
    }
    
    
    // END REST CLIENT ---------------------------------------------------------
    
    // IN APP PURCHASE ---------------------------------------------------------

    
    /**
     * Callback fired when item is successfully purchased through IAP.
     * @param sku 
     */
    @Override
    public void itemPurchased(String sku) {
        switch (sku) {
            case SKU_NOADS_1MONTH:
            case SKU_NOADS_3MONTH:
                // We don't need to do anything here 
                // IAP will automatically post them to the receipt store
                break;
            case SKU_10_CREDITS: {
                // Consumable product
                // We don't send credits to server, they are device only
                try {
                    addCredits(10);
                } catch (NegativeCreditsException ex) {
                    throw new RuntimeException("This should never happen");
                }
                break;
            }
            
            case SKU_TUTORIALS_1MONTH: {
                // We don't need to do anything here
                // IAP will automatically post them to the receipt store.
                break;
            }   
        }
        
        getAndRemovePurchaseCallback(sku).onSucess(sku);
    }

    /**
     * Fired when there is a purchase error in IAP.
     * @param sku
     * @param errorMessage 
     */
    @Override
    public void itemPurchaseError(String sku, String errorMessage) {
        getAndRemovePurchaseCallback(sku).onError(this, new RuntimeException(errorMessage), 1, errorMessage);
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
    
    
    /**
     * This class is purely to help for code organization as it groups together UI
     * creation code in a single place.
     */
    class ViewFactory {
        
        /**
         * Creates a toolbar and adds it to the given form.
         * @param f Form to add toolbar to.
         * @return The toolbar.
         */
        Toolbar createToolbar(Form f) {
            
            Toolbar tb = new Toolbar();
            f.setToolbar(tb);
            
            Button btnCredits = new Button("Credits");
            btnCredits.addActionListener(e->{
                new Controller().showCreditsForm();
            });
            FloatingActionButton badge = FloatingActionButton.createBadge(String.valueOf(getCredits()));
            Container wrappedBtn = badge.bindFabToContainer(btnCredits);
            
            tb.addComponentToSideMenu(wrappedBtn);
            
            Button btnSubscriptions = new Button("Subscriptions");
            btnSubscriptions.addActionListener(e->{
                controller.showSubscriptionsForm();
            });
            tb.addComponentToSideMenu(btnSubscriptions);
            
            
            Button btnTutorials = new Button("Tutorials");
            btnTutorials.addActionListener(e-> {
                controller.showTutorialsForm();
            });
            tb.addComponentToSideMenu(btnTutorials);
            
            
            return tb;
            
        }
        
        /**
         * Creates an adbar container.  If the user has a no-ads subscription, then 
         * the adbar will show a label saying "no ads showing".  If they don't have 
         * a subscription, it will have a label saying "This is an ad"
         * @return 
         */
        Container createAdbar() {
            Container wrapper = new Container();
            if (wrapper.getLayout() instanceof BorderLayout) {
                
            } else {
                wrapper.setLayout(new BorderLayout());
            }
            
            iap.synchronizeReceiptsSync(24 * 60 * 60 * 1000);
            if (!iap.isSubscribed(adSkus)) {
                wrapper.addComponent(BorderLayout.CENTER, new Label("This is an ad"));
            } else {
                wrapper.addComponent(BorderLayout.CENTER, new Label("No ads are showing"));
            }
            return wrapper;
        }
        
        
        
        /**
         * Creates a list of 
         * @return 
         */
        Container createSubscriptionsList() {
            Container list = new Container(new BoxLayout(BoxLayout.Y_AXIS));
            
            Container adsRow = new Container(new BorderLayout());
            adsRow.addComponent(BorderLayout.CENTER, new Label("Advertisements"));
            
            iap.synchronizeReceiptsSync(0);
            if (iap.isSubscribed(adSkus)) {
                adsRow.addComponent(BorderLayout.SOUTH, new Label("No ads.  Expires "+iap.getExpiryDate(adSkus)));
            } else {
                adsRow.addComponent(BorderLayout.SOUTH, new Label("Subscribe to remove ads"));
                
            }
            
            
            Button btnSubscribe1 = new Button("Subscribe 1 Month No Ads");
            btnSubscribe1.addActionListener(e->{
                setPurchaseCallback(SKU_NOADS_1MONTH, new Callback() {

                    @Override
                    public void onSucess(Object value) {
                        ToastBar.showMessage("Thanks for subscribing!", FontImage.MATERIAL_LOYALTY);
                        iap.synchronizeReceiptsSync(0);
                        Container parent = list.getParent();
                        if (parent != null) {
                            parent.replace(list, createSubscriptionsList(), CommonTransitions.createFade(300));
                            
                        }
                        
                    }

                    @Override
                    public void onError(Object sender, Throwable err, int errorCode, String errorMessage) {
                        Dialog.show("Subscription failed", errorMessage, "OK", null);
                        
                    }
                    
                });
                
                iap.purchase(SKU_NOADS_1MONTH);
            });
            
            Button btnSubscribe3 = new Button("Subscribe 3 Months No Ads");
            btnSubscribe3.addActionListener(e->{
                setPurchaseCallback(SKU_NOADS_1MONTH, new Callback() {

                    @Override
                    public void onSucess(Object value) {
                        ToastBar.showMessage("Thanks for subscribing!", FontImage.MATERIAL_LOYALTY);
                        iap.synchronizeReceiptsSync(0);
                        Container parent = list.getParent();
                        if (parent != null) {
                            parent.replace(list, createSubscriptionsList(), CommonTransitions.createFade(300));
                            
                        }
                        
                    }

                    @Override
                    public void onError(Object sender, Throwable err, int errorCode, String errorMessage) {
                        Dialog.show("Subscription failed", errorMessage, "OK", null);
                        
                    }
                    
                });
                iap.purchase(SKU_NOADS_3MONTH);
            });
            
            
            Button syncReceipts = new Button("Sync Receipts");
            syncReceipts.addActionListener(e->{
                Purchase.getInAppPurchase().synchronizeReceipts();
            });
            
            Button clearStorage = new Button("Clear Storage");
            clearStorage.addActionListener(e->{
                if (Dialog.show("Are you sure?", "This will clear all pending receipts.  Are you sure you want to do this?", "Yes", "Cancel")) {
                    Storage.getInstance().clearStorage();
                }
            });
            
            list.add(adsRow).add(btnSubscribe1).add(btnSubscribe3).add(syncReceipts).add(clearStorage);
            return BorderLayout.center(list);
        }
        
        void addAds(Container c) {
            if (c.getLayout() instanceof BorderLayout) {
                c.addComponent(BorderLayout.SOUTH, createAdbar());
            } else {
                c.addComponent(createAdbar());
            }
        }
        
        Container createTutorialsList() {
            Container root = new Container(new BorderLayout());
            Container list = new Container(new BoxLayout(BoxLayout.Y_AXIS));
            if (iap.isSubscribed(SKU_TUTORIALS_1MONTH)) {
                list.add(new Button("Tutorial 1"));
                list.add(new Button("Tutorial 2"));
                list.add(new Button("Tutorial 3"));
                list.add(new Label("Your subscription expires "+iap.getExpiryDate(SKU_TUTORIALS_1MONTH)));
                Button btnRenew = new Button("Renew");
                btnRenew.addActionListener(e->{
                    setPurchaseCallback(SKU_TUTORIALS_1MONTH, new Callback() {

                        @Override
                        public void onSucess(Object value) {
                            if (root.getParent() != null) {
                                root.getParent().replace(root, createTutorialsList(), CommonTransitions.createFade(500));
                            }
                        }

                        @Override
                        public void onError(Object sender, Throwable err, int errorCode, String errorMessage) {
                            Dialog.show("Purchase failed", errorMessage, "OK", null);
                        }
                        
                    });
                    iap.purchase(SKU_TUTORIALS_1MONTH);
                });
                
               
            } else {
                list.add(new SpanLabel("You are not currently subscribed to view tutorials"));
                Button btnRenew = new Button("Subscribe Now");
                btnRenew.addActionListener(e->{
                    setPurchaseCallback(SKU_TUTORIALS_1MONTH, new Callback() {

                        @Override
                        public void onSucess(Object value) {
                            if (root.getParent() != null) {
                                root.getParent().replace(root, createTutorialsList(), CommonTransitions.createFade(500));
                            }
                        }

                        @Override
                        public void onError(Object sender, Throwable err, int errorCode, String errorMessage) {
                            Dialog.show("Purchase failed", errorMessage, "OK", null);
                        }
                        
                    });
                    iap.purchase(SKU_TUTORIALS_1MONTH);
                });
            }
            
            root.addComponent(BorderLayout.CENTER, list);
            return root;
            
        }
        
        
        
    }
    
    class Controller {
        
        
        void showSubscriptionsForm() {
            Form f = new Form("Subscriptions", new BorderLayout());
            Toolbar tb = viewFactory.createToolbar(f);
            tb.setTitle("Subscriptions");
            f.addComponent(BorderLayout.CENTER, viewFactory.createSubscriptionsList());
            
            Button manageSubscriptions = new Button("Manage Subscriptions");
            
            
            f.show();
            
        }
        
        void showTutorialsForm() {
            Form f = new Form("Tutorials", new BorderLayout());
            Toolbar tb = viewFactory.createToolbar(f);
            tb.setTitle("Tutorials");
            f.addComponent(BorderLayout.CENTER, viewFactory.createTutorialsList());
            
            f.show();
        }
        
        void showCreditsForm() {
            Form f = new Form("Credits");
            new ViewFactory().createToolbar(f);
            f.setLayout(new BorderLayout());
            SpanLabel l = new SpanLabel("You have "+getCredits()+" credits");
            Button buyCredits = new Button("Buy 10 Credits");
            
            Runnable update = () -> {
                l.setText("You have "+getCredits()+" credits");
                f.revalidate();
            };
            
            buyCredits.addActionListener(e->{
                
                setPurchaseCallback(SKU_10_CREDITS, new Callback() {

                    @Override
                    public void onSucess(Object value) {
                        
                        update.run();
                    }

                    @Override
                    public void onError(Object sender, Throwable err, int errorCode, String errorMessage) {
                        Log.e(err);
                        Dialog.show("Purchase failed", errorMessage, "OK", null);
                    }
                    
                });
                
                iap.purchase(SKU_10_CREDITS);
            });
            
            
            Button useCredit = new Button("Use 1 Credit");
            useCredit.addActionListener(e->{
                try {
                    useCredit();
                } catch (NegativeCreditsException ex) {
                    Dialog.show("No credits", "You have no credits left.  Please buy more.", "OK", null);
                }
                update.run();
            });
            
            
            f.addComponent(BorderLayout.CENTER, BoxLayout.encloseY(l, useCredit, buyCredits));
            viewFactory.addAds(f);
            f.show();
        }
    }
}
