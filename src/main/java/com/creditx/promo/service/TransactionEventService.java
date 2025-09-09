package com.creditx.promo.service;

import com.creditx.promo.dto.TransactionPostedEvent;

public interface TransactionEventService {

  void processTransactionPosted(TransactionPostedEvent event);
}
