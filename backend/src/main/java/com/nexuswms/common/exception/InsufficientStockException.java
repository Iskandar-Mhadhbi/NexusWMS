package com.nexuswms.common.exception;

public class InsufficientStockException extends RuntimeException {

    public InsufficientStockException(String skuCode, String shelfCode) {
        super("Insufficient stock for SKU " + skuCode + " on shelf " + shelfCode);
    }
}