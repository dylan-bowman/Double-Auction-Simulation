/*
 * Author: Dylan Bowman
 * 
 * Expiration Comparator implements Comparator<LimitOrder>
 * 
 * Provides a comparator to order limirOrders by expiration date.
 */

import java.util.Comparator;

public class ExpirationComparator implements Comparator<LimitOrder> {
    private static final ExpirationComparator instance = 
        new ExpirationComparator();
    
    public static ExpirationComparator getInstance() {
        return instance;
    }
    
    private ExpirationComparator() {
    }
    
    @Override
    // return 0 if equal, subtraction of limitorders otherwise
    public int compare(LimitOrder one, LimitOrder two) {
        return one.getExpiration() - two.getExpiration();
    }
}