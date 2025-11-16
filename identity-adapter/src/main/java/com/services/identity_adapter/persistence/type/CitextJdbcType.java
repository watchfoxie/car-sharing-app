package com.services.identity_adapter.persistence.type;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Types;

import org.hibernate.type.descriptor.ValueBinder;
import org.hibernate.type.descriptor.WrapperOptions;
import org.hibernate.type.descriptor.java.JavaType;
import org.hibernate.type.descriptor.jdbc.BasicBinder;
import org.hibernate.type.descriptor.jdbc.VarcharJdbcType;

/**
 * Custom JDBC type descriptor that tells Hibernate to bind PostgreSQL {@code citext}
 * values using {@link Types#OTHER}. This ensures prepared-statement parameters are
 * sent to PostgreSQL as {@code citext}, preserving case-insensitive comparisons.
 */
public final class CitextJdbcType extends VarcharJdbcType {


    @Override
    public int getJdbcTypeCode() {
        return Types.OTHER;
    }

    @Override
    public String getFriendlyName() {
        return "CITEXT";
    }

    @Override
    public <X> ValueBinder<X> getBinder(JavaType<X> javaTypeDescriptor) {
        return new BasicBinder<>(javaTypeDescriptor, this) {
            @Override
            protected void doBind(PreparedStatement st, X value, int index, WrapperOptions options)
                    throws SQLException {
                st.setObject(index, javaTypeDescriptor.unwrap(value, String.class, options), Types.OTHER);
            }

            @Override
            protected void doBind(CallableStatement st, X value, String name, WrapperOptions options)
                    throws SQLException {
                st.setObject(name, javaTypeDescriptor.unwrap(value, String.class, options), Types.OTHER);
            }
        };
    }
}
