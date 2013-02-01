/*
 * Copyright (C) 2010-2012 Serge Rieder
 * serge@jkiss.org
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */
package org.jkiss.dbeaver.model.impl.jdbc.data;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Tree;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.core.CoreMessages;
import org.jkiss.dbeaver.model.DBConstants;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.data.DBDDisplayFormat;
import org.jkiss.dbeaver.model.data.DBDStructure;
import org.jkiss.dbeaver.model.data.DBDValueController;
import org.jkiss.dbeaver.model.data.DBDValueEditor;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.exec.DBCExecutionContext;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCExecutionContext;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCPreparedStatement;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSDataType;
import org.jkiss.dbeaver.model.struct.DBSTypedObject;
import org.jkiss.dbeaver.ui.dialogs.data.ComplexObjectEditor;
import org.jkiss.dbeaver.ui.dialogs.data.DefaultValueViewDialog;
import org.jkiss.dbeaver.ui.properties.PropertySourceAbstract;

import java.sql.Ref;
import java.sql.SQLException;

/**
 * JDBC reference value handler.
 * Handle STRUCT types.
 *
 * @author Serge Rider
 */
public class JDBCReferenceValueHandler extends JDBCAbstractValueHandler {

    static final Log log = LogFactory.getLog(JDBCReferenceValueHandler.class);

    public static final JDBCReferenceValueHandler INSTANCE = new JDBCReferenceValueHandler();

    @Override
    public int getFeatures()
    {
        return FEATURE_VIEWER | FEATURE_EDITOR | FEATURE_SHOW_ICON;
    }

    /**
     * NumberFormat is not thread safe thus this method is synchronized.
     */
    @Override
    public synchronized String getValueDisplayString(DBSTypedObject column, Object value, DBDDisplayFormat format)
    {
        JDBCReference reference = (JDBCReference) value;
        return reference == null || reference.isNull() ? DBConstants.NULL_VALUE_LABEL : reference.toString();
    }

    @Override
    protected Object fetchColumnValue(
        DBCExecutionContext context,
        JDBCResultSet resultSet,
        DBSTypedObject type,
        int index)
        throws DBCException, SQLException
    {
        Ref value = resultSet.getRef(index);
        return getValueFromObject(context, type, value, false);
    }

    @Override
    protected void bindParameter(
        JDBCExecutionContext context,
        JDBCPreparedStatement statement,
        DBSTypedObject paramType,
        int paramIndex,
        Object value)
        throws DBCException, SQLException
    {
        JDBCReference reference = (JDBCReference) value;
        statement.setRef(paramIndex, reference.getValue());
    }

    @Override
    public Class getValueObjectType()
    {
        return Ref.class;
    }

    @Override
    public JDBCReference getValueFromObject(DBCExecutionContext context, DBSTypedObject type, Object object, boolean copy) throws DBCException
    {
        String typeName;
        try {
            if (object instanceof Ref) {
                typeName = ((Ref) object).getBaseTypeName();
            } else {
                typeName = type.getTypeName();
            }
        } catch (SQLException e) {
            throw new DBCException(e);
        }
        DBSDataType dataType = null;
        try {
            dataType = DBUtils.resolveDataType(context.getProgressMonitor(), context.getDataSource(), typeName);
        } catch (DBException e) {
            log.error("Error resolving data type '" + typeName + "'", e);
        }
        if (object == null) {
            return new JDBCReference(dataType, null);
        } else if (object instanceof JDBCReference) {
            return (JDBCReference)object;
        } else if (object instanceof Ref) {
            return new JDBCReference(dataType, (Ref) object);
        } else {
            throw new DBCException("Unsupported struct type: " + object.getClass().getName());
        }
    }

    @Override
    public DBDValueEditor createEditor(final DBDValueController controller)
        throws DBException
    {
       return null;
    }

}