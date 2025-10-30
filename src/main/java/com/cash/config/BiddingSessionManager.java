package com.cash.config;

import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Component;

/**
 * Stores a single auction item identifier per servlet session.
 * Controllers/services can use this to remember the item a user chose to bid
 * on.
 */
@Component
public class BiddingSessionManager {

  private static final String ITEM_ID_ATTR = "biddingItemId";

  /**
   * Remember which item the current session is bidding on.
   */
  public void setItem(HttpSession session, Long itemId) {
    session.setAttribute(ITEM_ID_ATTR, itemId);
  }

  /**
   * Retrieve the item tied to this session (if any).
   */
  public Long getItem(HttpSession session) {
    return (Long) session.getAttribute(ITEM_ID_ATTR);
  }

  /**
   * Clear the stored item, typically on logout.
   */
  public void clear(HttpSession session) {
    session.removeAttribute(ITEM_ID_ATTR);
  }
}
