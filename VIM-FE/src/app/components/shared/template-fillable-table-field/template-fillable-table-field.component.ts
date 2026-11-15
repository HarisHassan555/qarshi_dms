import { Component, EventEmitter, Input, Output } from '@angular/core';
import {
    applyTableCellValueChange,
    buildTableRenderRows,
    getFormulaDefinitionForCellKey,
    getTableIndicatorColumnCount,
    normalizeTableCellValues,
    normalizeTableHtmlForField,
    TableFieldLike,
    TableRenderCell,
    TableRenderRow,
} from 'src/app/utils/template-fillable-table-field.util';
import {
    getFormulaEditText,
    isExcelFormulaComplete,
    isExcelFormulaText,
} from 'src/app/utils/template-table-formula.util';

@Component({
    selector: 'app-template-fillable-table-field',
    templateUrl: './template-fillable-table-field.component.html',
    styleUrls: ['./template-fillable-table-field.component.css'],
})
export class TemplateFillableTableFieldComponent {
    @Input() field!: TableFieldLike;
    @Input() cellValues: Record<string, string> = {};
    @Input() editable = false;
    @Input() inline = false;
    @Output() cellValuesChange = new EventEmitter<Record<string, string>>();
    @Output() tableHtmlChange = new EventEmitter<string>();

    tableFieldIndicatorsDismissed = false;
    private tableRenderRowsCache: { html: string; rows: TableRenderRow[] } | null = null;
    private editingTableFormulaKey: string | null = null;
    private tableFormulaDrafts = new Map<string, string>();

    dismissTableFieldIndicators(event: Event): void {
        event.preventDefault();
        event.stopPropagation();
        this.tableFieldIndicatorsDismissed = true;
    }

    getTableIndicatorColumns(): string[] {
        return getTableIndicatorColumnCount(this.field);
    }

    getTableRenderRows(): TableRenderRow[] {
        const html = normalizeTableHtmlForField(this.field, this.field.tableHtml);
        if (this.tableRenderRowsCache && this.tableRenderRowsCache.html === html) {
            return this.tableRenderRowsCache.rows;
        }
        const rows = buildTableRenderRows(this.field, html);
        this.tableRenderRowsCache = { html, rows };
        return rows;
    }

    getTableCellValue(cellKey: string): string {
        return normalizeTableCellValues(this.cellValues)[cellKey] || '';
    }

    tableFormulaInputValue(cellKey: string): string {
        if (this.isTableFormulaEditing(cellKey)) {
            return this.getTableFormulaExpression(cellKey);
        }
        return this.getTableCellValue(cellKey);
    }

    isTableFormulaEditing(cellKey: string): boolean {
        return this.editingTableFormulaKey === this.tableFormulaDraftKey(cellKey);
    }

    beginTableCellEdit(cell: TableRenderCell, event: FocusEvent): void {
        if (cell.formula) {
            this.beginTableFormulaEdit(cell.key);
        }
        event.stopPropagation();
    }

    endTableCellEdit(cell: TableRenderCell): void {
        this.endTableFormulaEdit(cell.key);
    }

    onTableCellInput(cell: TableRenderCell, value: string): void {
        if (cell.formula || this.isTableFormulaEditing(cell.key)) {
            if (!this.isTableFormulaEditing(cell.key)) {
                this.beginTableFormulaEdit(cell.key);
            }
            this.setTableFormulaExpression(cell.key, value);
            return;
        }
        const result = applyTableCellValueChange(this.field, this.cellValues, cell.key, value);
        this.tableRenderRowsCache = null;
        this.cellValuesChange.emit(result.values);
        if (result.tableHtml !== this.field.tableHtml) {
            this.tableHtmlChange.emit(result.tableHtml);
        }
    }

    trackTableRow(_index: number, row: TableRenderRow): string {
        return row.cells.map((cell) => cell.key).join('|');
    }

    trackTableCell(_index: number, cell: TableRenderCell): string {
        return cell.key;
    }

    private tableFormulaDraftKey(cellKey: string): string {
        return `${this.field.id}:${cellKey}`;
    }

    private beginTableFormulaEdit(cellKey: string): void {
        this.editingTableFormulaKey = this.tableFormulaDraftKey(cellKey);
    }

    private endTableFormulaEdit(cellKey: string): void {
        const draftKey = this.tableFormulaDraftKey(cellKey);
        if (this.editingTableFormulaKey !== draftKey) {
            return;
        }
        const draft = this.tableFormulaDrafts.get(draftKey);
        this.editingTableFormulaKey = null;
        if (draft !== undefined && isExcelFormulaComplete(draft)) {
            this.tableFormulaDrafts.delete(draftKey);
        }
    }

    private getTableFormulaExpression(cellKey: string): string {
        const draft = this.tableFormulaDrafts.get(this.tableFormulaDraftKey(cellKey));
        if (draft !== undefined) {
            return draft;
        }
        const definition = getFormulaDefinitionForCellKey(this.field, cellKey);
        return definition ? getFormulaEditText(definition) : '';
    }

    private setTableFormulaExpression(cellKey: string, expression: string): void {
        const trimmed = String(expression || '').trim();
        const draftKey = this.tableFormulaDraftKey(cellKey);
        if (!trimmed) {
            this.tableFormulaDrafts.delete(draftKey);
            const result = applyTableCellValueChange(this.field, this.cellValues, cellKey, '');
            this.tableRenderRowsCache = null;
            this.cellValuesChange.emit(result.values);
            this.tableHtmlChange.emit(result.tableHtml);
            return;
        }
        if (!isExcelFormulaText(trimmed)) {
            this.tableFormulaDrafts.delete(draftKey);
            const result = applyTableCellValueChange(this.field, this.cellValues, cellKey, trimmed);
            this.tableRenderRowsCache = null;
            this.cellValuesChange.emit(result.values);
            this.tableHtmlChange.emit(result.tableHtml);
            return;
        }
        if (!isExcelFormulaComplete(trimmed)) {
            this.tableFormulaDrafts.set(draftKey, trimmed);
            return;
        }
        this.tableFormulaDrafts.set(draftKey, trimmed);
        const result = applyTableCellValueChange(this.field, this.cellValues, cellKey, trimmed);
        this.tableRenderRowsCache = null;
        this.cellValuesChange.emit(result.values);
        this.tableHtmlChange.emit(result.tableHtml);
    }
}
