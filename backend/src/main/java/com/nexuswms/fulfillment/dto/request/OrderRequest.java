// OrderRequest.java
package com.nexuswms.fulfillment.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty; 

import java.util.List;
import java.util.Map;

public record OrderRequest(

    @NotBlank(message = "Customer name is required")
    String customerName,

    Map<String, Object> customerAddress,

    String priority,

    String notes,

    @NotEmpty(message = "Order must have at least one line")
    @Valid
    List<OrderLineRequest> lines
) {}