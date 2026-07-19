// CarrierRequest.java
package com.nexuswms.fulfillment.dto.request;

import jakarta.validation.constraints.NotBlank;

import java.util.Map;

public record CarrierRequest(

    @NotBlank(message = "Carrier name is required")
    String name,

    @NotBlank(message = "Carrier code is required")
    String code,

    Map<String, Object> contactInfo
) {}