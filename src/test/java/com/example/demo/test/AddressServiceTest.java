package com.example.demo.test;

import com.example.demo.dao.AddressDAO;
import com.example.demo.model.Address;
import com.example.demo.model.User;
import com.example.demo.service.UserService;
import com.example.demo.service.impl.AddressServiceImpl;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class AddressServiceTest {

    @Mock
    private AddressDAO addressDAO;

    @Mock
    private UserService userService;

    @InjectMocks
    private AddressServiceImpl addressService;

    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    @Test
    public void markDefaultShouldDelegateToUserService() {
        User user = new User();
        user.setId(1L);
        Address address = new Address();
        address.setId(10L);
        address.setUser(user);

        when(addressDAO.findById(10L)).thenReturn(address);

        addressService.markDefault(1L, 10L);

        verify(userService).setDefaultAddress(1L, 10L);
    }

    @Test
    public void markDefaultShouldRejectForeignAddress() {
        User owner = new User();
        owner.setId(2L);
        Address address = new Address();
        address.setId(20L);
        address.setUser(owner);

        when(addressDAO.findById(20L)).thenReturn(address);

        expectedException.expect(IllegalArgumentException.class);
        addressService.markDefault(1L, 20L);
    }

    @Test
    public void deleteShouldClearDefaultAddress() {
        User user = new User();
        user.setId(5L);
        user.setDefaultAddressId(55L);
        Address address = new Address();
        address.setId(55L);
        address.setUser(user);

        when(addressDAO.findById(55L)).thenReturn(address);

        addressService.delete(55L);

        verify(addressDAO).delete(address);
        verify(userService).setDefaultAddress(5L, null);
    }
}