package eu.sakarah.tool.mapping.excel;

import org.apache.commons.lang.StringUtils;
import org.apache.poi.xssf.usermodel.XSSFCell;
import org.apache.poi.xssf.usermodel.XSSFRow;

public class ExcelUtils {

	public static String readCell(XSSFRow row, int index) {

		String cellContent = null;
		XSSFCell cell = row.getCell(index);
		if (cell != null) {
			switch (cell.getCellTypeEnum()) {
			case NUMERIC:
				cellContent = new Integer((int) cell.getNumericCellValue()).toString();
				break;
			case STRING:
				cellContent = StringUtils.trimToNull(cell.getStringCellValue());
				break;
			case _NONE:
			case BLANK:
				// Nothing to do.
				break;
			default:
				throw new RuntimeException("Impossible d'extraire un text de la ligne/colonne " + (row.getRowNum()+1) + "/" + (index+1) + " : le type de colonne " + cell.getCellTypeEnum().name() + " n'est pas géré.");
			}
		}
		return cellContent;
	}
}
