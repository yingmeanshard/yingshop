package com.example.demo.service;

import com.example.demo.model.Address;

import java.util.List;

public interface AddressService {

    List<Address> findByUser(Long userId);

    Address getById(Long id);

    Address save(Address address);

    void delete(Long id);

    void markDefault(Long userId, Long addressId);
}