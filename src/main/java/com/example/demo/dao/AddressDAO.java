package com.example.demo.dao;

import com.example.demo.model.Address;

import java.util.List;

public interface AddressDAO {

    Address findById(Long id);

    List<Address> findByUserId(Long userId);

    Address save(Address address);

    void delete(Address address);
}