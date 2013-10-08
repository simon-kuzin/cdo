/*
 * Copyright (c) 2004-2013 Eike Stepper (Berlin, Germany), CEA LIST, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *    Christian W. Damus (CEA LIST) - initial API and implementation
 */
package org.eclipse.emf.cdo.internal.security.ui.util;

import org.eclipse.emf.common.notify.AdapterFactory;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EReference;
import org.eclipse.emf.edit.domain.EditingDomain;

import org.eclipse.core.databinding.DataBindingContext;
import org.eclipse.jface.layout.TableColumnLayout;
import org.eclipse.jface.viewers.CellEditor;
import org.eclipse.jface.viewers.CellLabelProvider;
import org.eclipse.jface.viewers.ColumnWeightData;
import org.eclipse.jface.viewers.EditingSupport;
import org.eclipse.jface.viewers.IFilter;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.swt.SWT;

/**
 * 
 */
public class OneToManyTableBlock extends OneToManyBlock
{

  public OneToManyTableBlock(DataBindingContext context, EditingDomain domain, AdapterFactory adapterFactory,
      ITableConfiguration tableConfig)
  {
    super(context, domain, adapterFactory, tableConfig);
  }

  @Override
  protected ITableConfiguration getConfiguration()
  {
    return (ITableConfiguration)super.getConfiguration();
  }

  @Override
  protected boolean isTable()
  {
    return true;
  }

  @Override
  protected void configureColumns(final TableViewer viewer, TableColumnLayout layout)
  {
    super.configureColumns(viewer, layout);

    viewer.getTable().setHeaderVisible(true);
    viewer.getTable().setLinesVisible(true);

    final ITableConfiguration tableConfig = getConfiguration();

    String[] columnTitles = tableConfig.getColumnTitles();

    for (int i = 0; i < columnTitles.length; i++)
    {
      TableViewerColumn column = new TableViewerColumn(viewer, SWT.LEFT);
      column.getColumn().setText(columnTitles[i]);
      layout.setColumnData(
          column.getColumn(),
          new ColumnWeightData(tableConfig.getColumnWeight(i), tableConfig.getColumnMinimumSize(i), tableConfig
              .isColumnResizable(i)));

      final int columnIndex = i;

      column.setLabelProvider(tableConfig.getLabelProvider(viewer, columnIndex));

      column.setEditingSupport(new EditingSupport(viewer)
      {

        @Override
        protected void setValue(Object element, Object value)
        {
          tableConfig.setValue(viewer, element, columnIndex, value);
        }

        @Override
        protected Object getValue(Object element)
        {
          return tableConfig.getValue(viewer, element, columnIndex);
        }

        @Override
        protected boolean canEdit(Object element)
        {
          return tableConfig.canEdit(viewer, element, columnIndex);
        }

        @Override
        protected CellEditor getCellEditor(Object element)
        {
          return tableConfig.getCellEditor(viewer, columnIndex);
        }
      });
    }
  }

  //
  // Nested types
  //

  public static interface ITableConfiguration extends IOneToManyConfiguration
  {
    String[] getColumnTitles();

    int getColumnWeight(int index);

    int getColumnMinimumSize(int index);

    boolean isColumnResizable(int index);

    CellLabelProvider getLabelProvider(TableViewer viewer, int columnIndex);

    boolean canEdit(TableViewer viewer, Object element, int columnIndex);

    void setValue(TableViewer viewer, Object element, int columnIndex, Object value);

    Object getValue(TableViewer viewer, Object element, int columnIndex);

    CellEditor getCellEditor(TableViewer viewer, int columnIndex);
  }

  public static abstract class TableConfiguration extends OneToManyConfiguration implements ITableConfiguration
  {

    public TableConfiguration(EReference reference, EClass itemType, IFilter filter)
    {
      super(reference, itemType, filter);
    }

    public TableConfiguration(EReference reference, EClass itemType)
    {
      super(reference, itemType);
    }

    public TableConfiguration(EReference reference, IFilter filter)
    {
      super(reference, filter);
    }

    public TableConfiguration(EReference reference)
    {
      super(reference);
    }

  }
}
