package com.example.demo.test;

import com.example.demo.dao.impl.OrderDAOImpl;
import com.example.demo.model.Order;
import com.example.demo.model.User;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.query.Query;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class OrderDAOImplTest {

    @Mock
    private SessionFactory sessionFactory;

    @Mock
    private Session session;

    @Mock
    private Query<Order> query;

    private OrderDAOImpl orderDAO;

    @Before
    public void setUp() {
        when(sessionFactory.getCurrentSession()).thenReturn(session);
        when(session.createQuery(anyString(), eq(Order.class))).thenReturn(query);
        orderDAO = new OrderDAOImpl(sessionFactory);
    }

    @Test
    public void testFindByUser() {
        User user = new User();
        user.setId(1L);

        List<Order> expectedOrders = List.of(new Order(), new Order());
        when(query.setParameter(eq("userId"), eq(user.getId()))).thenReturn(query);
        when(query.list()).thenReturn(expectedOrders);

        List<Order> actualOrders = orderDAO.findByUser(user);

        assertEquals(expectedOrders, actualOrders);
        verify(query).setParameter("userId", user.getId());
        verify(query).list();
    }
}
