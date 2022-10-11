package it.usna.swing.table;

import java.awt.Component;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.MouseEvent;
import java.util.Arrays;
import java.util.stream.Collectors;

import javax.swing.JTable;
import javax.swing.SwingUtilities;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableColumn;
import javax.swing.table.TableModel;

import it.usna.util.AppProperties;

/**
 * A swing JTable with column header tooltips, cell tooltips, column visibility control and
 * property (columns sizes, positions and visibility) saving abilities.
 * @author USNA - Antonio Flaccomio
 * @version 1.0
 */
public class TooltipTable extends JTable {

	private static final long serialVersionUID = 1L;
	private final static String COL_WIDTH = "COL_W";
	private final static String COL_POSITION = "COL_P";

	private TableColumn[] hiddenColumns;

	/**
	 * Default constructor
	 */
	public TooltipTable() {
	}

	/**
	 * Create a table with specified TableModel
	 * @param tm TableModel
	 * @param header String[]
	 */
	public TooltipTable(final TableModel tm) {
		super(tm);
		setHeadersTooltip((String[])null);
	}

	public void setHeadersTooltip(final String ... headerTips) {
		tableHeader = new JTableHeader(super.columnModel) {
			private static final long serialVersionUID = 1L;

			@Override
			public String getToolTipText(final MouseEvent evt) {
				final int tc = columnAtPoint(evt.getPoint());
				final int mc = convertColumnIndexToModel(tc);
				if(headerTips != null && mc < headerTips.length && headerTips[mc] != null) {
					return headerTips[mc];
				} else {
					final String name = getColumnName(tc);
					final int strWidth = SwingUtilities.computeStringWidth(getGraphics().getFontMetrics(), name);
					return (getHeaderRect(tc).width <= strWidth) ? name : null;
				}
			}
		};
		tableHeader.setTable(this); // Otherwise sort graphics don't work	
	}

	/**
	 * Override the default method to show tooltip if the cell is not wide enough to fully show the value
	 */
	@Override
	public String getToolTipText(final MouseEvent evt) {
		if(((Component) evt.getSource()).isVisible()) {
			final int r, c;
			final Object value;
			if ((r = rowAtPoint(evt.getPoint())) >= 0 && (c = columnAtPoint(evt.getPoint())) >= 0 && (value = getValueAt(r, c)) != null) {
				final Component comp = this.getCellRenderer(r, c).getTableCellRendererComponent(this, value, false, false, r, c);
				//final int strWidth = SwingUtilities.computeStringWidth(getGraphics().getFontMetrics(), value.toString()); // Nota: se la stringa e' di tipo html il calcolo dell'estensione non e' valido!
				//if (getCellRect(r, c, false).width <= /*strWidth*/comp.getPreferredSize().width && (strVal = cellValueToString(value, r, c)).length() > 0) {
				//	return strVal;
				//}
				final String strVal = cellTooltipValue(value, getCellRect(r, c, false).width <= comp.getPreferredSize().width, r, c);
				if (strVal != null && strVal.length() > 0) {
					return strVal;
				}
			}
		}
		return null;
	}

	/**
	 * Try to map an Object cell value to a String value
	 */
	protected String cellTooltipValue(Object value, boolean cellTooSmall, int row, int column) {
		if(cellTooSmall) {
			if(value == null) return "";
			else if(value instanceof Object[]) return Arrays.stream((Object[])value).map(v -> v.toString()).collect(Collectors.joining(" + "));
			else return value.toString();
		} else {
			return null;
		}
	}

	/**
	 * Override the default method to show tooltip if the cell is not wide enough to fully show the value
	 */
	@Override
	public Point getToolTipLocation(final MouseEvent evt) {
		final int r = rowAtPoint(evt.getPoint());
		final int c = columnAtPoint(evt.getPoint());
		final Rectangle cellRec = getCellRect(r, c, true);
		return new Point(cellRec.x, cellRec.y);
	}

	/**
	 * Save columns position and visibility
	 */
	public void saveColPos(final AppProperties prop, final String prefix) {
		String pos[] = new String[getColumnCount()];
		for(int i = 0; i < pos.length; i++) {
			pos[i] = convertColumnIndexToModel(i) + "";
		}
		prop.setMultipleProperty(prefix + "." + COL_POSITION, pos, ',');
	}

	/**
	 * Restore columns position and visibility (columns must be original - model order - position; call restoreColumns() otherwise)
	 * @param prefix String the prefix used to distinguish attributes among tables if more than one table is saved on the same Properties object
	 */
	public void loadColPos(final AppProperties prop, final String prefix) {
		try { // in case a newer/older version had a different number of columns
			String pos[] = prop.getMultipleProperty(prefix + "." + COL_POSITION, ',');
			if(pos != null) {
				int i;
				for(i = 0; i < pos.length; i++) {
					int modelPos = Integer.parseInt(pos[i]);
					moveColumn(convertColumnIndexToView(modelPos), i);
				}
				while(i < getColumnCount()) { // getColumnCount() decreases a every iteration
					hideColumn(convertColumnIndexToModel(i));
				}
			}
		} catch(Exception e) {}
	}

	/**
	 * Save table column width
	 * @param prefix String the prefix used to distinguish attributes among tables if more than one table is saved on the same Properties object
	 */
	public void saveColWidth(final AppProperties prop, final String prefix) {
		final int modelCol = dataModel.getColumnCount();
		String w[] = new String[modelCol];
		for (int col = 0; col < modelCol; col++) {
			int vc = convertColumnIndexToView(col);
			w[col] = (vc >= 0) ? columnModel.getColumn(vc).getWidth() + "" : "0";
		}
		prop.setMultipleProperty(prefix + "." + COL_WIDTH, w, ',');
	}

	public void loadColWidth(final AppProperties prop, final String prefix) {
		String w[] = prop.getMultipleProperty(prefix + "." + COL_WIDTH, ',');
		final int modelCol = dataModel.getColumnCount();
		if(w != null && w.length == modelCol) { // in case a newer/older version had a different number of columns
			for (int i = 0; i < modelCol; i++) {
				int vc = convertColumnIndexToView(i);
				if(vc >= 0) {
					columnModel.getColumn(vc).setPreferredWidth(Integer.parseInt(w[i]));	
				}
			}
		}
	}

	public void restoreColumns() {
		if (hiddenColumns != null) {
			Arrays.stream(hiddenColumns).filter(c -> c != null).forEach(c -> addColumn(c));
			hiddenColumns = null;
		}
		int pos = 0;
		for (int i = 0; i < dataModel.getColumnCount(); i++) {
			if(convertColumnIndexToView(i) >= 0) {
				moveColumn(convertColumnIndexToView(i), pos++);
			}
		}
	}

	/**
	 * Hide a column
	 * @param modelInd int column index in the table model
	 */
	public int hideColumn(final int modelInd) {
		final int pos = convertColumnIndexToView(modelInd);
		if (pos != -1) {
			if(hiddenColumns == null) {
				hiddenColumns = new TableColumn[dataModel.getColumnCount()];
			}
			TableColumn col = columnModel.getColumn(pos);
			hiddenColumns[modelInd] = col;
			removeColumn(col);
		}
		return pos;
	}

	/**
	 * Previously hidden column will be shown
	 * @param modelInd int column index in the table model
	 */
	public boolean showColumn(final int modelInd) {
		if (hiddenColumns != null && hiddenColumns[modelInd] != null) {
			addColumn(hiddenColumns[modelInd]);
			hiddenColumns[modelInd] = null;
			return true;
		}
		return false;
	}

	/**
	 * Previously hidden column will be shown
	 * @param modelInd int column index in the table model
	 * @param int viewPos new column position; if < 0 try to hint
	 */
	public void showColumn(final int modelInd, int viewPos) {
		if(showColumn(modelInd)) {
			if(viewPos < 0) {
				for(int i = modelInd - 1; i >= 0; i--) {
					if(isColumnVisible(i)) {
						moveColumn(getColumnCount() - 1, convertColumnIndexToView(i) + 1);
						return;
					}
				}
				moveColumn(getColumnCount() - 1, 0);
			} else {
				moveColumn(getColumnCount() - 1, viewPos);
			}
		}
	}

	/**
	 * Check if a column is visible
	 * @param modelInd int column index in the table model
	 * @return boolean
	 */
	public boolean isColumnVisible(final int modelInd) {
		return hiddenColumns == null || hiddenColumns[modelInd] == null;
	}
} // 230 - 245