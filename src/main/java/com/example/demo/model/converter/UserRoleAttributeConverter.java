package com.example.demo.model.converter;

import javax.persistence.AttributeConverter;
import javax.persistence.Converter;

import com.example.demo.model.UserRole;

@Converter(autoApply = true)
public class UserRoleAttributeConverter implements AttributeConverter<UserRole, String> {

    @Override
    public String convertToDatabaseColumn(UserRole attribute) {
        return attribute == null ? UserRole.CUSTOMER.getPersistedValue() : attribute.getPersistedValue();
    }

    @Override
    public UserRole convertToEntityAttribute(String dbData) {
        return UserRole.fromDatabaseValue(dbData);
    }
}
