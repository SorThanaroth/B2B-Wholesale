package com.wholesale.marketplace.modules.user.dto;

import com.wholesale.marketplace.modules.user.Address;

import java.util.UUID;

public record AddressDto(
        UUID id,
        String label,
        String street,
        String city,
        String province,
        boolean isDefault
) {
    public static AddressDto from(Address a) {
        return new AddressDto(a.getId(), a.getLabel(), a.getStreet(), a.getCity(), a.getProvince(), a.isDefault());
    }
}
