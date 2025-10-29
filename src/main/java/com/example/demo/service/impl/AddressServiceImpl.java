package com.example.demo.service.impl;

import com.example.demo.dao.AddressDAO;
import com.example.demo.model.Address;
import com.example.demo.model.User;
import com.example.demo.service.AddressService;
import com.example.demo.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class AddressServiceImpl implements AddressService {

    private final AddressDAO addressDAO;
    private final UserService userService;

    @Autowired
    public AddressServiceImpl(AddressDAO addressDAO, UserService userService) {
        this.addressDAO = addressDAO;
        this.userService = userService;
    }

    @Override
    public List<Address> findByUser(Long userId) {
        return addressDAO.findByUserId(userId);
    }

    @Override
    public Address getById(Long id) {
        return addressDAO.findById(id);
    }

    @Override
    public Address save(Address address) {
        if (address.getUser() == null || address.getUser().getId() == null) {
            throw new IllegalArgumentException("Address must be associated with a user");
        }
        return addressDAO.save(address);
    }

    @Override
    public void delete(Long id) {
        Address address = addressDAO.findById(id);
        if (address != null) {
            User user = address.getUser();
            Long userId = user != null ? user.getId() : null;
            addressDAO.delete(address);
            if (userId != null && user != null && user.getDefaultAddressId() != null
                    && user.getDefaultAddressId().equals(id)) {
                userService.setDefaultAddress(userId, null);
            }
        }
    }

    @Override
    public void markDefault(Long userId, Long addressId) {
        Address address = addressDAO.findById(addressId);
        if (address == null || address.getUser() == null || !address.getUser().getId().equals(userId)) {
            throw new IllegalArgumentException("無法設定預設地址");
        }
        userService.setDefaultAddress(userId, addressId);
    }
}